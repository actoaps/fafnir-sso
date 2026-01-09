package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.model.conf.ProviderConf;
import dk.acto.fafnir.sso.provider.unilogin.*;
import dk.acto.fafnir.sso.util.PkceUtil;
import dk.acto.fafnir.sso.util.TokenFactory;
import https.unilogin.Institutionstilknytning;
import https.wsibruger_unilogin_dk.ws.WsiBruger;
import https.wsibruger_unilogin_dk.ws.WsiBrugerPortType;
import https.wsiinst_unilogin_dk.ws.WsiInst;
import https.wsiinst_unilogin_dk.ws.WsiInstPortType;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class UniLoginProvider {
    private final FafnirConf fafnirConf;
    private final TokenFactory tokenFactory;
    private final ProviderConf providerConf;
    private final UniLoginHelper uniloginHelper;

    public String authenticate(HttpSession session) throws NoSuchAlgorithmException {
        var codeVerifier = PkceUtil.generateCodeVerifier();
        session.setAttribute("codeVerifier", codeVerifier);

        var responseType = "response_type=" + URLEncoder.encode("code");
        var client = "&client_id=" + URLEncoder.encode(System.getenv("UL_CLIENT_ID"));
        var redirect = "&redirect_uri=" + URLEncoder.encode(System.getenv("FAFNIR_URL") + "/unilogin/callback");
        var codeChallengeMethod = "&code_challenge_method=" + URLEncoder.encode("S256");
        var codeChallenge = "&code_challenge=" + URLEncoder.encode(PkceUtil.generateCodeChallenge(codeVerifier));
        var nonce = "&nonce=" + URLEncoder.encode(new SecureRandom().ints(16, 0, 256)
            .mapToObj(i -> String.format("%02x", i))
            .collect(Collectors.joining()));
        var state = "&state=" + URLEncoder.encode(new SecureRandom().ints(16, 0, 256)
            .mapToObj(i -> String.format("%02x", i))
            .collect(Collectors.joining()));
        var scope = "&scope=" + URLEncoder.encode("openid");
        var responseMode = "&response_mode=" + URLEncoder.encode("form_post");
        return "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect" + "/auth?" + responseType + client + redirect + codeChallengeMethod + codeChallenge + nonce + state + scope + responseMode;
    }

    public AuthenticationResult callback(UniloginTokenCredentials data, HttpSession session) throws IOException {
        var UL_CLIENT_ID = System.getenv("UL_CLIENT_ID");
        var UL_SECRET = System.getenv("UL_SECRET");
        var UL_REDIRECT_URL = System.getenv("FAFNIR_URL") + "/unilogin/callback";
        var OID_BASE_URL = "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect/";

        var CODE_VERIFIER = (String) session.getAttribute("codeVerifier");

        var accessCode = data.getCode();
        AccessToken accessToken;

        accessToken = getAccessToken(accessCode, UL_CLIENT_ID, UL_SECRET, UL_REDIRECT_URL, CODE_VERIFIER, OID_BASE_URL);

        IntrospectionToken intro;

        intro = getIntrospectToken(accessToken.getAccess_token(), UL_CLIENT_ID, UL_SECRET, OID_BASE_URL);

        if (intro == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var userId = intro.getUniid();
        
        // Store aktørgruppe from introspection token for role extraction
        if (intro.getAktoer_gruppe() != null) {
            session.setAttribute("aktoer_gruppe", intro.getAktoer_gruppe());
            log.debug("Stored aktørgruppe from introspection token: {}", intro.getAktoer_gruppe());
        }
        
        // Try to get institution data from UserInfo endpoint (new OIDC approach)
        List<Institution> institutions = null;
        UserInfoResponse userInfo = null;
        try {
            userInfo = getUserInfo(accessToken.getAccess_token(), OID_BASE_URL);
            if (userInfo != null && userInfo.getInstBrugere() != null && !userInfo.getInstBrugere().isEmpty()) {
                institutions = convertUserInfoToInstitutions(userInfo);
                log.debug("Successfully retrieved institutions from UserInfo endpoint for user: {}", userId);
                // Store UserInfo in session for role extraction later
                session.setAttribute("userInfo", userInfo);
                session.setAttribute("accessToken", accessToken.getAccess_token());
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve extended user information from UserInfo endpoint, falling back to web services: {}", e.getMessage());
        }
        
        // Fallback to web services if UserInfo failed or returned no data
        if (institutions == null || institutions.isEmpty()) {
            log.debug("Using deprecated web service call to get institution list for user: {}", userId);
            institutions = getInstitutionList(userId);
        }

        if (institutions.isEmpty()) {
            return AuthenticationResult.failure(FailureReason.CONNECTION_FAILED);
        } else if (institutions.size() == 1) {
            return callbackWithInstitution(userId, institutions.get(0).getId(), institutions.get(0).getName(), session);
        } else {
            String chooseInstitutionUrl = uniloginHelper.getChooseInstitutionUrl(userId);
            return AuthenticationResult.redirect(chooseInstitutionUrl);
        }
    }


    /**
     * @deprecated Use UserInfo endpoint instead. This method will be removed in a future version.
     * Call getUserInfo() and convertUserInfoToInstitutions() instead.
     */
    @Deprecated
    public List<Institution> getInstitutionList(String userId) {
        URL wsdlURLBruger = getClass().getClassLoader().getResource("wsdl/wsibruger_v6.wsdl");
        URL wsdlURLInst = getClass().getClassLoader().getResource("wsdl/wsiinst_v5.wsdl");

        var wsiBrugerService = new WsiBruger(wsdlURLBruger);
        var wsiInstService = new WsiInst(wsdlURLInst);

        var wsiBruger = wsiBrugerService.getWsiBrugerPort();
        var wsiInst = wsiInstService.getWsiInstPort();

        try {
            List<Institutionstilknytning> institutionstilknytninger = wsiBruger.hentBrugersInstitutionstilknytninger(
                uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);

            return institutionstilknytninger.stream().map(institutionstilknytning -> {
                String instName = "";
                https.unilogin.Institution inst = null;
                try {
                    inst = wsiInst.hentInstitution(
                        uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionstilknytning.getInstnr());
                    instName = inst.getInstnavn();
                } catch (Exception e) {
                    log.error("Error fetching institution name", e);
                }
                var roleNames = toUserRoles(institutionstilknytninger).stream()
                    .map(UserRole::toString)
                    .collect(Collectors.toList());
                return convertInstitution(inst, roleNames);
            }).distinct().collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching user's institution affiliations", e);
        }
        return Collections.emptyList();
    }

    private Set<UserRole> toUserRoles(List<Institutionstilknytning> institutionstilknytninger) {
        Set<UserRole> roles = new HashSet<>();
        for (var institutionstilknytning : institutionstilknytninger) {
            var ansat = institutionstilknytning.getAnsat();
            var ekstern = institutionstilknytning.getEkstern();
            var elev = institutionstilknytning.getElev();
            if (ansat != null) {
                ansat.getRolle().forEach(ansatrolle ->
                    roles.add(UserRole.builder().name(ansatrolle.name()).type("EMPLOYEE").build()));
            }
            if (ekstern != null) {
                roles.add(UserRole.builder()
                    .name(ekstern.getRolle().name())
                    .type("EMP_EXTERNAL")
                    .build());
            }
            if (elev != null) {
                roles.add(UserRole.builder()
                    .name(elev.getRolle().name())
                    .type("PUPIL")
                    .build());
            }
        }
        return roles;
    }

    private Institution convertInstitution(https.unilogin.Institution inst, List<String> roles) {
        return Institution.builder()
            .name(inst.getInstnavn())
            .id(inst.getInstnr())
            .roles(roles)
            .build();
    }

    public String getFailureUrl(FailureReason reason) {
        return fafnirConf.getFailureRedirect() + "#" + reason.getErrorCode();
    }


    public AuthenticationResult callbackWithInstitution(String userId, String institutionId) {
        return callbackWithInstitution(userId, institutionId, null, null);
    }
    
    public AuthenticationResult callbackWithInstitution(String userId, String institutionId, String institutionName) {
        return callbackWithInstitution(userId, institutionId, institutionName, null);
    }

    public AuthenticationResult callbackWithInstitution(String userId, String institutionId, String institutionName, HttpSession session) {
        var name = getUserFullNameFromId(userId);
        
        // Try to get institution name from parameter, otherwise fallback to web service
        final var orgName = institutionName != null ? institutionName :
            getInstitutionFromId(institutionId)
                .map(Institution::getName)
                .orElseThrow(() -> new RuntimeException("No institution"));

        // Try to get roles from UserInfo, fallback to aktørgruppe, then web service
        Set<UserRole> roles = getUserRolesFromUserInfo(userId, institutionId, session);
        if (roles.isEmpty()) {
            // Try to get role from aktørgruppe (actor group) from introspection token
            roles = getUserRolesFromAktorgruppe(session);
            if (!roles.isEmpty()) {
                log.debug("Using aktørgruppe from introspection token for user: {} at institution: {}", userId, institutionId);
            }
        }
        if (roles.isEmpty()) {
            log.debug("Using deprecated web service call to get user roles for user: {} at institution: {}", userId, institutionId);
            roles = this.getUserRoles(institutionId, userId);
        }

        var subjectActual = UserData.builder()
            .subject(userId)
            .name(name)
            .build();
        var orgActual = OrganisationData.builder()
            .organisationId(institutionId)
            .organisationName(orgName)
            .build();
        var claimsActual = ClaimData.builder()
            .claims(roles.stream()
                .map(UserRole::toString)
                .toArray(String[]::new))
            .build();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, ProviderMetaData.builder()
            .providerName("UniLogin")
            .providerId("unilogin")
            .organisationSupport(OrganisationSupport.NATIVE)
            .inputs(List.of())
            .build());
        return AuthenticationResult.success(jwt);

    }

    /**
     * @deprecated Use UserInfo endpoint instead. Institution data should come from UserInfo response.
     * This method will be removed in a future version.
     */
    @Deprecated
    private Optional<Institution> getInstitutionFromId(String institutionId) {
        var wsiURL = getClass().getClassLoader().getResource("wsdl/wsiinst_v5.wsdl");
        var SERVICE = new QName("https://wsiinst.unilogin.dk/ws", "WsiInst");
        var wsiInst = new WsiInst(wsiURL, SERVICE);
        var wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            var inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionId);
            return Optional.of(Institution.builder()
                .id(inst.getInstnr())
                .name(inst.getInstnavn())
                .build());
        } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Optional.empty();
        }
    }

    /**
     * @deprecated Use getUserRolesFromUserInfo() instead. This method will be removed in a future version.
     */
    @Deprecated
    private Set<UserRole> getUserRoles(String institutionId, String userId) {
        var wsdlURL = getClass().getClassLoader().getResource("wsdl/wsibruger_v6.wsdl");
        var SERVICE_NAME = new QName("https://wsibruger.unilogin.dk/ws", "WsiBruger");
        var wsiBruger = new WsiBruger(wsdlURL, SERVICE_NAME);
        var wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        try {
            var institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
            institutionstilknytninger = institutionstilknytninger.stream()
                .filter(til -> institutionId.equals(til.getInstnr()))
                .collect(Collectors.toList());
            return toUserRoles(institutionstilknytninger);
        } catch (https.wsibruger_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Collections.emptySet();
        }
    }

    private String getUserFullNameFromId(String userId) {
        return userId;
    }

    private AccessToken getAccessToken(String code, String clientId, String clientSecret, String redirectUri, String codeVerifier, String oidcBaseUrl) throws IOException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("grant_type", "authorization_code"));
        nvps.add(new BasicNameValuePair("code", code));
        nvps.add(new BasicNameValuePair("client_id", clientId));
        nvps.add(new BasicNameValuePair("client_secret", clientSecret));
        nvps.add(new BasicNameValuePair("redirect_uri", redirectUri));
        nvps.add(new BasicNameValuePair("code_verifier", codeVerifier));
        HttpResponse response = httpPostRequest(oidcBaseUrl + "/token", nvps);

        return getObjectMapper().readValue(response.getEntity().getContent(), AccessToken.class);
    }

    private IntrospectionToken getIntrospectToken(String accesstoken, String clientId, String clientSecret, String oidcBaseUrl) throws IOException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("token", accesstoken));
        nvps.add(new BasicNameValuePair("client_id", clientId));
        nvps.add(new BasicNameValuePair("client_secret", clientSecret));
        HttpResponse response = httpPostRequest(oidcBaseUrl + "/token/introspect", nvps);

        return getObjectMapper().readValue(response.getEntity().getContent(), IntrospectionToken.class);
    }

    private HttpResponse httpPostRequest(String uri, List<NameValuePair> nvps) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost();
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpClient.execute(httpPost);
        return response;
    }

    /**
     * Retrieves extended user information from the OIDC UserInfo endpoint.
     * This requires "Udvidede brugerinformationer" to be enabled in Udbyderportalen.
     * 
     * @param accessToken The OAuth2 access token
     * @param oidcBaseUrl The base URL for OIDC endpoints
     * @return UserInfoResponse containing extended user information, or null if failed
     * @throws IOException If the HTTP request fails
     */
    private UserInfoResponse getUserInfo(String accessToken, String oidcBaseUrl) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(oidcBaseUrl + "userinfo");
        httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + accessToken));
        httpGet.setHeader(new BasicHeader("Accept", "application/json"));
        
        try {
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode == 200) {
                UserInfoResponse userInfo = getObjectMapper().readValue(response.getEntity().getContent(), UserInfoResponse.class);
                // Debug logging for testing
                if (userInfo != null && userInfo.getInstBrugere() != null) {
                    log.info("UserInfo endpoint returned {} institution(s) for user", userInfo.getInstBrugere().size());
                    if (log.isDebugEnabled()) {
                        log.debug("UserInfo response: inst_brugere={}", 
                            userInfo.getInstBrugere().stream()
                                .map(inst -> String.format("{in_tnr=%s, in_tnavn=%s}", inst.getInTnr(), inst.getInTnavn()))
                                .collect(Collectors.joining(", ")));
                    }
                } else {
                    log.info("UserInfo endpoint returned no institution data (inst_brugere is null or empty)");
                }
                return userInfo;
            } else {
                log.warn("UserInfo endpoint returned status code: {}", statusCode);
                return null;
            }
        } catch (Exception e) {
            log.error("Error calling UserInfo endpoint", e);
            throw new IOException("Failed to retrieve user information from UserInfo endpoint", e);
        } finally {
            httpClient.close();
        }
    }

    /**
     * Converts UserInfo response to Institution list.
     * 
     * @param userInfo The UserInfo response from OIDC endpoint
     * @return List of Institution objects
     */
    private List<Institution> convertUserInfoToInstitutions(UserInfoResponse userInfo) {
        if (userInfo == null || userInfo.getInstBrugere() == null) {
            return Collections.emptyList();
        }
        
        return userInfo.getInstBrugere().stream()
            .map(inst -> Institution.builder()
                .id(inst.getInTnr())
                .name(inst.getInTnavn())
                .roles(Collections.emptyList()) // Roles not available in basic institution affiliation
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Attempts to get user roles from UserInfo endpoint.
     * Roles are available when "Roller" is enabled in Udbyderportalen.
     * 
     * @param userId The user ID
     * @param institutionId The institution ID to get roles for
     * @param session The HTTP session containing UserInfo response
     * @return Set of UserRole objects, empty if not available
     */
    private Set<UserRole> getUserRolesFromUserInfo(String userId, String institutionId, HttpSession session) {
        try {
            // If session is null, we can't get UserInfo - fallback to web service
            if (session == null) {
                log.debug("Session is null, cannot retrieve UserInfo for roles for user: {} at institution: {}", userId, institutionId);
                return Collections.emptySet();
            }
            
            // Try to get UserInfo from session first
            UserInfoResponse userInfo = (UserInfoResponse) session.getAttribute("userInfo");
            
            // If not in session, try to get it from access token
            if (userInfo == null) {
                String accessToken = (String) session.getAttribute("accessToken");
                if (accessToken != null) {
                    String OID_BASE_URL = "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect/";
                    userInfo = getUserInfo(accessToken, OID_BASE_URL);
                }
            }
            
            if (userInfo == null || userInfo.getInstBrugere() == null) {
                log.debug("UserInfo not available for role extraction for user: {} at institution: {}", userId, institutionId);
                return Collections.emptySet();
            }
            
            // Find the institution affiliation matching the institutionId
            Optional<InstitutionTilknytning> institutionAffiliation = userInfo.getInstBrugere().stream()
                .filter(inst -> institutionId.equals(inst.getInTnr()))
                .findFirst();
            
            if (institutionAffiliation.isEmpty()) {
                log.debug("Institution {} not found in UserInfo for user: {}", institutionId, userId);
                return Collections.emptySet();
            }
            
            InstitutionTilknytning affiliation = institutionAffiliation.get();
            Set<UserRole> roles = new HashSet<>();
            
            // Extract roles from the affiliation
            // Try different possible formats for roles
            
            // Format 1: Direct "roller" array with strings like "PÆDAGOG@EMPLOYEE"
            if (affiliation.getRoller() != null && !affiliation.getRoller().isEmpty()) {
                for (String roleString : affiliation.getRoller()) {
                    UserRole role = parseRoleString(roleString);
                    if (role != null) {
                        roles.add(role);
                    }
                }
            }
            
            // Format 2: Separate arrays for ansat_roller, elev_roller, ekstern_roller
            if (affiliation.getAnsatRoller() != null && !affiliation.getAnsatRoller().isEmpty()) {
                for (String roleName : affiliation.getAnsatRoller()) {
                    roles.add(UserRole.builder().name(roleName).type("EMPLOYEE").build());
                }
            }
            
            if (affiliation.getElevRoller() != null && !affiliation.getElevRoller().isEmpty()) {
                for (String roleName : affiliation.getElevRoller()) {
                    roles.add(UserRole.builder().name(roleName).type("PUPIL").build());
                }
            }
            
            if (affiliation.getEksternRoller() != null && !affiliation.getEksternRoller().isEmpty()) {
                for (String roleName : affiliation.getEksternRoller()) {
                    roles.add(UserRole.builder().name(roleName).type("EMP_EXTERNAL").build());
                }
            }
            
            if (!roles.isEmpty()) {
                log.debug("Successfully extracted {} role(s) from UserInfo for user: {} at institution: {}", 
                    roles.size(), userId, institutionId);
            } else {
                log.debug("No roles found in UserInfo for user: {} at institution: {}", userId, institutionId);
            }
            
            return roles;
            
        } catch (Exception e) {
            log.warn("Error extracting roles from UserInfo for user: {} at institution: {}: {}", 
                userId, institutionId, e.getMessage());
            return Collections.emptySet();
        }
    }
    
    /**
     * Parses a role string in the format "ROLENAME@TYPE" into a UserRole object.
     * 
     * @param roleString The role string (e.g., "PÆDAGOG@EMPLOYEE")
     * @return UserRole object, or null if parsing fails
     */
    private UserRole parseRoleString(String roleString) {
        if (roleString == null || roleString.isEmpty()) {
            return null;
        }
        
        int atIndex = roleString.indexOf('@');
        if (atIndex == -1 || atIndex == 0 || atIndex == roleString.length() - 1) {
            // Invalid format, return null
            log.debug("Invalid role string format: {}", roleString);
            return null;
        }
        
        String name = roleString.substring(0, atIndex);
        String type = roleString.substring(atIndex + 1);
        
        return UserRole.builder()
            .name(name)
            .type(type)
            .build();
    }

    /**
     * Attempts to get user roles from aktørgruppe (actor group) in the introspection token.
     * Aktørgruppe is a single value indicating the user's primary actor type (e.g., "PUPIL", "EMPLOYEE", "EMP_EXTERNAL").
     * This is a fallback when detailed roles are not available from UserInfo.
     * 
     * @param session The HTTP session containing aktørgruppe
     * @return Set of UserRole objects (typically one role), empty if not available
     */
    private Set<UserRole> getUserRolesFromAktorgruppe(HttpSession session) {
        if (session == null) {
            return Collections.emptySet();
        }
        
        String aktoerGruppe = (String) session.getAttribute("aktoer_gruppe");
        if (aktoerGruppe == null || aktoerGruppe.isEmpty()) {
            log.debug("Aktørgruppe not available in session");
            return Collections.emptySet();
        }
        
        Set<UserRole> roles = new HashSet<>();
        
        // Map aktørgruppe values to UserRole
        // Common values: "PUPIL", "EMPLOYEE", "EMP_EXTERNAL"
        switch (aktoerGruppe.toUpperCase()) {
            case "PUPIL":
            case "ELEV":
                // For pupils, we don't have a specific role name, so we use the actor group as the role
                roles.add(UserRole.builder()
                    .name(aktoerGruppe)
                    .type("PUPIL")
                    .build());
                log.debug("Extracted role from aktørgruppe: {} -> PUPIL", aktoerGruppe);
                break;
            case "EMPLOYEE":
            case "ANSAT":
                roles.add(UserRole.builder()
                    .name(aktoerGruppe)
                    .type("EMPLOYEE")
                    .build());
                log.debug("Extracted role from aktørgruppe: {} -> EMPLOYEE", aktoerGruppe);
                break;
            case "EMP_EXTERNAL":
            case "EKSTERN":
                roles.add(UserRole.builder()
                    .name(aktoerGruppe)
                    .type("EMP_EXTERNAL")
                    .build());
                log.debug("Extracted role from aktørgruppe: {} -> EMP_EXTERNAL", aktoerGruppe);
                break;
            default:
                // Unknown aktørgruppe value, but still create a role with it
                log.debug("Unknown aktørgruppe value: {}, creating generic role", aktoerGruppe);
                roles.add(UserRole.builder()
                    .name(aktoerGruppe)
                    .type("UNKNOWN")
                    .build());
                break;
        }
        
        return roles;
    }

    ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    public ProviderMetaData getMetaData() {
        return MetadataProvider.UNILOGIN;
    }
}
