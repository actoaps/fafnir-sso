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
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class UniLoginProvider {
    private final FafnirConf fafnirConf;
    private final TokenFactory tokenFactory;
    private final ProviderConf providerConf;
    private final UniLoginHelper uniloginHelper;
    
    // In-memory cache for temporary JWT storage during logout flow
    // Key: one-time token, Value: JWT, TTL: 5 minutes
    private static final Map<String, CacheEntry> jwtCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes
    
    static {
        // Clean up expired entries every minute
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            jwtCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private static class CacheEntry {
        private final String jwt;
        private final long expiresAt;
        
        CacheEntry(String jwt, long ttlSeconds) {
            this.jwt = jwt;
            this.expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000);
        }
        
        boolean isExpired(long now) {
            return now > expiresAt;
        }
        
        String getJwt() {
            return jwt;
        }
    }

    /**
     * Gets the OIDC base URL based on TEST_ENABLED_UNILOGIN environment variable.
     * Defaults to production endpoint if TEST_ENABLED_UNILOGIN is not set, empty, or "false".
     * @return Test endpoint if TEST_ENABLED_UNILOGIN is set to a truthy value, otherwise production endpoint
     */
    private String getOidcBaseUrl() {
        String testEnabled = System.getenv("TEST_ENABLED_UNILOGIN");
        // Default to production if not set, empty, or explicitly "false"
        if (testEnabled != null && !testEnabled.trim().isEmpty() && !testEnabled.trim().equalsIgnoreCase("false")) {
            return "https://et-broker.unilogin.dk/auth/realms/broker/protocol/openid-connect/";
        }
        // Production endpoint (default)
        return "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect/";
    }

    /**
     * Gets the OIDC authorization base URL (without trailing slash) based on TEST_ENABLED_UNILOGIN.
     * Defaults to production endpoint if TEST_ENABLED_UNILOGIN is not set, empty, or "false".
     * @return Test endpoint if TEST_ENABLED_UNILOGIN is set to a truthy value, otherwise production endpoint
     */
    private String getOidcAuthBaseUrl() {
        String testEnabled = System.getenv("TEST_ENABLED_UNILOGIN");
        // Default to production if not set, empty, or explicitly "false"
        if (testEnabled != null && !testEnabled.trim().isEmpty() && !testEnabled.trim().equalsIgnoreCase("false")) {
            return "https://et-broker.unilogin.dk/auth/realms/broker/protocol/openid-connect";
        }
        // Production endpoint (default)
        return "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect";
    }

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
        return getOidcAuthBaseUrl() + "/auth?" + responseType + client + redirect + codeChallengeMethod + codeChallenge + nonce + state + scope + responseMode;
    }

    public AuthenticationResult callback(UniloginTokenCredentials data, HttpSession session) throws IOException {
        var UL_CLIENT_ID = System.getenv("UL_CLIENT_ID");
        var UL_SECRET = System.getenv("UL_SECRET");
        var UL_REDIRECT_URL = System.getenv("FAFNIR_URL") + "/unilogin/callback";
        var OID_BASE_URL = getOidcBaseUrl();

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
                institutions = convertUserInfoToInstitutions(userInfo, session);
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
            Institution inst = institutions.get(0);
            log.debug("Single institution found - id: {}, name: {}", inst.id, inst.name);
            return callbackWithInstitution(userId, inst.id, inst.name, session);
        } else {
            String chooseInstitutionUrl = uniloginHelper.getChooseInstitutionUrl(userId);
            return AuthenticationResult.redirect(chooseInstitutionUrl);
        }
    }


    /**
     * Gets institution list from UserInfo stored in session, or falls back to deprecated web service.
     * 
     * @param userId The user ID
     * @param session The HTTP session containing UserInfo
     * @return List of Institution objects
     */
    public List<Institution> getInstitutionListFromSession(String userId, HttpSession session) {
        log.info("getInstitutionListFromSession called for user: {}, session is null: {}", userId, session == null);
        
        if (session != null) {
            UserInfoResponse userInfo = (UserInfoResponse) session.getAttribute("userInfo");
            log.info("UserInfo from session - is null: {}, has instBrugere: {}", 
                userInfo == null, 
                userInfo != null && userInfo.getInstBrugere() != null);
            
            if (userInfo != null && userInfo.getInstBrugere() != null && !userInfo.getInstBrugere().isEmpty()) {
                log.info("Retrieving institutions from UserInfo in session for user: {}, count: {}", 
                    userId, userInfo.getInstBrugere().size());
                List<Institution> institutions = convertUserInfoToInstitutions(userInfo, session);
                log.info("Converted {} institution(s) from UserInfo", institutions.size());
                return institutions;
            }
        }
        // Fallback to deprecated web service
        log.warn("UserInfo not available in session, falling back to deprecated web service for user: {}", userId);
        return getInstitutionList(userId);
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
        return new Institution(inst.getInstnr(), inst.getInstnavn(), roles);
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
        
        // Try to get institution name from parameter, then from UserInfo in session, otherwise fallback to web service
        String orgName = (institutionName != null && !institutionName.isEmpty()) ? institutionName : null;
        
        // If not provided, try to get from UserInfo stored in session
        if (orgName == null && session != null) {
            UserInfoResponse userInfo = (UserInfoResponse) session.getAttribute("userInfo");
            if (userInfo != null && userInfo.getInstBrugere() != null) {
                Optional<String> nameFromUserInfo = userInfo.getInstBrugere().stream()
                    .filter(inst -> institutionId.equals(inst.getInTnr()))
                    .map(InstitutionTilknytning::getInTnavn)
                    .filter(n -> n != null && !n.isEmpty())
                    .findFirst();
                if (nameFromUserInfo.isPresent()) {
                    orgName = nameFromUserInfo.get();
                    log.debug("Retrieved institution name from UserInfo for institution: {}", institutionId);
                } else {
                    log.warn("Institution {} not found in UserInfo for user: {}", institutionId, userId);
                }
            } else {
                log.debug("UserInfo not available in session for user: {}", userId);
            }
        }
        
        // Final fallback to web service only if we still don't have a name AND UserInfo is not available
        final String finalOrgName;
        if (orgName != null && !orgName.isEmpty()) {
            finalOrgName = orgName;
        } else if (session != null && session.getAttribute("userInfo") != null) {
            // UserInfo is available but institution not found - this is an error
            log.error("Institution {} not found in UserInfo and no name provided. UserInfo is available but institution is missing.", institutionId);
            throw new RuntimeException("Institution " + institutionId + " not found in UserInfo");
        } else {
            // UserInfo not available, use deprecated web service as last resort
            log.warn("UserInfo not available, falling back to deprecated web service for institution: {}", institutionId);
            finalOrgName = getInstitutionFromId(institutionId)
                .map(inst -> inst.name)
                .orElseThrow(() -> new RuntimeException("No institution found for ID: " + institutionId));
        }

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
            .organisationName(finalOrgName)
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
        
        // Generate a one-time token for secure JWT retrieval after logout
        String oneTimeToken = generateOneTimeToken();
        
        // Store JWT in cache with short TTL (5 minutes)
        jwtCache.put(oneTimeToken, new CacheEntry(jwt, CACHE_TTL_SECONDS));
        log.debug("Stored JWT in cache with one-time token (expires in {} seconds)", CACHE_TTL_SECONDS);
        
        // Instead of returning success directly, redirect to logout first
        // This ensures the user is logged out from UniLogin after we get the data
        // Store token in session to retrieve after logout (cookie approach would require HttpServletResponse)
        if (session != null) {
            session.setAttribute("logout_token", oneTimeToken);
            log.debug("Stored one-time token in session for post-logout retrieval");
        }
        
        // Use base URL only (no token in URL) to avoid UniLogin redirect URI validation issues
        String logoutUrl = getLogoutUrl(
            fafnirConf.getUrl() + "/unilogin/logout-complete",
            null // We don't have id_token_hint here, but it's optional
        );
        
        log.info("Authentication successful, redirecting to UniLogin logout to end UniLogin session");
        return AuthenticationResult.redirect(logoutUrl);
    }
    
    /**
     * Generates a secure random one-time token for JWT retrieval.
     * 
     * @return A random token string
     */
    private String generateOneTimeToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Retrieves and removes a JWT from the cache using a one-time token.
     * This is a one-time operation - the token is consumed after use.
     * 
     * @param token The one-time token
     * @return The JWT if found and not expired, null otherwise
     */
    public String retrieveJwtFromCache(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        CacheEntry entry = jwtCache.remove(token);
        if (entry == null) {
            log.warn("One-time token not found or already used: {}", token.substring(0, Math.min(8, token.length())) + "...");
            return null;
        }
        
        if (entry.isExpired(System.currentTimeMillis())) {
            log.warn("One-time token expired: {}", token.substring(0, Math.min(8, token.length())) + "...");
            return null;
        }
        
        log.debug("Successfully retrieved JWT from cache using one-time token");
        return entry.getJwt();
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
            return Optional.of(new Institution(inst.getInstnr(), inst.getInstnavn(), Collections.emptyList()));
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
                // Read raw JSON first to see what we're getting
                String rawJson = new String(response.getEntity().getContent().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                log.info("Raw UserInfo JSON response: {}", rawJson);
                
                // Parse JSON
                UserInfoResponse userInfo = getObjectMapper().readValue(rawJson, UserInfoResponse.class);
                
                // Debug logging for testing
                if (userInfo != null && userInfo.getInstBrugere() != null) {
                    log.info("UserInfo endpoint returned {} institution(s) for user", userInfo.getInstBrugere().size());
                    for (int i = 0; i < userInfo.getInstBrugere().size(); i++) {
                        var inst = userInfo.getInstBrugere().get(i);
                        log.info("UserInfo instBrugere[{}]: inTnr='{}', inTnavn='{}', roller={}, ansatRoller={}, elevRoller={}, eksternRoller={}", 
                            i, inst.getInTnr(), inst.getInTnavn(), inst.getRoller(), 
                            inst.getAnsatRoller(), inst.getElevRoller(), inst.getEksternRoller());
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
     * Extracts roles from UserInfo if available, otherwise uses aktørgruppe from session as fallback.
     * 
     * @param userInfo The UserInfo response from OIDC endpoint
     * @param session The HTTP session (may contain aktørgruppe for fallback)
     * @return List of Institution objects
     */
    private List<Institution> convertUserInfoToInstitutions(UserInfoResponse userInfo, HttpSession session) {
        if (userInfo == null || userInfo.getInstBrugere() == null) {
            log.warn("convertUserInfoToInstitutions: userInfo or instBrugere is null");
            return Collections.emptyList();
        }
        
        log.info("Converting {} institution affiliation(s) to Institution objects", userInfo.getInstBrugere().size());
        
        // Get aktørgruppe from session as fallback for roles
        String aktorgruppe = session != null ? (String) session.getAttribute("aktoer_gruppe") : null;
        log.debug("Aktørgruppe from session: {}", aktorgruppe);
        
        // Log raw UserInfo data first
        for (int i = 0; i < userInfo.getInstBrugere().size(); i++) {
            var inst = userInfo.getInstBrugere().get(i);
            log.info("UserInfo institution[{}]: inTnr='{}', inTnavn='{}', roller={}, ansatRoller={}, elevRoller={}, eksternRoller={}", 
                i, inst.getInTnr(), inst.getInTnavn(), inst.getRoller(), 
                inst.getAnsatRoller(), inst.getElevRoller(), inst.getEksternRoller());
        }
        
        List<Institution> institutions = userInfo.getInstBrugere().stream()
            .map(inst -> {
                String id = inst.getInTnr();
                String name = inst.getInTnavn();
                
                // Extract roles from UserInfo
                List<String> roles = new ArrayList<>();
                
                // Format 1: Direct "roller" array with strings like "PÆDAGOG@EMPLOYEE"
                if (inst.getRoller() != null && !inst.getRoller().isEmpty()) {
                    roles.addAll(inst.getRoller());
                    log.debug("Found {} role(s) in 'roller' field for institution: {}", inst.getRoller().size(), id);
                }
                
                // Format 2: Separate arrays for ansat_roller, elev_roller, ekstern_roller
                if (inst.getAnsatRoller() != null && !inst.getAnsatRoller().isEmpty()) {
                    roles.addAll(inst.getAnsatRoller());
                    log.debug("Found {} role(s) in 'ansatRoller' field for institution: {}", inst.getAnsatRoller().size(), id);
                }
                
                if (inst.getElevRoller() != null && !inst.getElevRoller().isEmpty()) {
                    roles.addAll(inst.getElevRoller());
                    log.debug("Found {} role(s) in 'elevRoller' field for institution: {}", inst.getElevRoller().size(), id);
                }
                
                if (inst.getEksternRoller() != null && !inst.getEksternRoller().isEmpty()) {
                    roles.addAll(inst.getEksternRoller());
                    log.debug("Found {} role(s) in 'eksternRoller' field for institution: {}", inst.getEksternRoller().size(), id);
                }
                
                // Fallback: Use aktørgruppe if no roles found in UserInfo
                if (roles.isEmpty() && aktorgruppe != null && !aktorgruppe.isEmpty()) {
                    roles.add(aktorgruppe);
                    log.warn("⚠️ No specific roles found in UserInfo for institution: {}. Using aktørgruppe '{}' as fallback. " +
                        "To get specific roles (Lærer, Pædagog, etc.), activate 'Roller' in Udbyderportalen under 'Ekstra attributter'.", 
                        id, aktorgruppe);
                } else if (roles.isEmpty()) {
                    log.warn("⚠️ No roles found in UserInfo for institution: {} and no aktørgruppe available. " +
                        "Activate 'Roller' in Udbyderportalen to get specific roles.", id);
                }
                
                log.info("Creating Institution - id: '{}', name: '{}', roles: {} (id is null: {}, name is null: {})", 
                    id, name, roles, id == null, name == null);
                
                // Use constructor directly instead of builder to ensure values are set
                Institution institution = new Institution(id, name, roles);
                
                log.info("Created Institution object - id: '{}', name: '{}', roles: {}, class: {}, fields accessible: id={}, name={}", 
                    institution.id, institution.name, institution.roles, institution.getClass().getName(),
                    institution.id != null, institution.name != null);
                
                // Verify the values were actually set
                if (institution.id == null || institution.name == null) {
                    log.error("ERROR: Institution fields are null after creation! id: {}, name: {}", 
                        institution.id, institution.name);
                }
                
                return institution;
            })
            .collect(Collectors.toList());
        
        log.info("Successfully converted {} Institution object(s)", institutions.size());
        return institutions;
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
                    String OID_BASE_URL = getOidcBaseUrl();
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

    /**
     * Builds the RP-initiated logout URL that redirects to UniLogin's end_session endpoint.
     * This should be called when the user initiates logout from your application.
     * 
     * @param postLogoutRedirectUri The URL to redirect to after logout (optional)
     * @param idTokenHint The ID token hint (optional, but recommended for better UX)
     * @return The logout URL to redirect the user to
     */
    public String getLogoutUrl(String postLogoutRedirectUri, String idTokenHint) {
        var OID_BASE_URL = getOidcBaseUrl();
        var endSessionUrl = OID_BASE_URL + "logout";
        
        StringBuilder url = new StringBuilder(endSessionUrl);
        
        // Add client_id (required by some OIDC providers)
        var clientId = System.getenv("UL_CLIENT_ID");
        if (clientId != null && !clientId.isEmpty()) {
            url.append("?client_id=").append(URLEncoder.encode(clientId, java.nio.charset.StandardCharsets.UTF_8));
        }
        
        // Add post_logout_redirect_uri (optional but recommended)
        if (postLogoutRedirectUri != null && !postLogoutRedirectUri.isEmpty()) {
            url.append(clientId != null ? "&" : "?")
               .append("post_logout_redirect_uri=")
               .append(URLEncoder.encode(postLogoutRedirectUri, java.nio.charset.StandardCharsets.UTF_8));
        } else {
            // Default to FAFNIR_URL if not provided
            var fafnirUrl = System.getenv("FAFNIR_URL");
            if (fafnirUrl != null && !fafnirUrl.isEmpty()) {
                url.append(clientId != null ? "&" : "?")
                   .append("post_logout_redirect_uri=")
                   .append(URLEncoder.encode(fafnirUrl, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        
        // Add id_token_hint (optional but recommended for better UX)
        if (idTokenHint != null && !idTokenHint.isEmpty()) {
            url.append("&id_token_hint=").append(URLEncoder.encode(idTokenHint, java.nio.charset.StandardCharsets.UTF_8));
        }
        
        return url.toString();
    }

    /**
     * Handles back-channel logout event from UniLogin.
     * This endpoint receives POST requests from UniLogin when a user logs out.
     * 
     * @param logoutToken The logout token from UniLogin (JWT)
     * @return true if logout was successful, false otherwise
     */
    public boolean handleBackChannelLogout(String logoutToken) {
        try {
            log.info("Received back-channel logout event from UniLogin");
            
            // Validate the logout token
            // In a production system, you should:
            // 1. Verify the JWT signature
            // 2. Check the issuer (iss claim)
            // 3. Check the audience (aud claim)
            // 4. Extract the session ID (sid claim) or subject (sub claim)
            // 5. Invalidate the corresponding session in your system
            
            // For now, we'll log the token and return success
            // TODO: Implement proper JWT validation and session invalidation
            log.debug("Logout token received: {}", logoutToken != null ? logoutToken.substring(0, Math.min(50, logoutToken.length())) + "..." : "null");
            
            // In a real implementation, you would:
            // - Parse the JWT to get the session ID or user ID
            // - Invalidate the session in your session store (e.g., Hazelcast)
            // - Clear any cached tokens or user data
            
            return true;
        } catch (Exception e) {
            log.error("Error handling back-channel logout", e);
            return false;
        }
    }

    /**
     * Invalidates the local session by clearing session attributes.
     * 
     * @param session The HTTP session to invalidate
     */
    public void invalidateSession(HttpSession session) {
        if (session != null) {
            try {
                log.debug("Invalidating session: {}", session.getId());
                session.invalidate();
            } catch (Exception e) {
                log.warn("Error invalidating session", e);
            }
        }
    }
}
