package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.AuthenticationResult;
import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.UniLoginProvider;
import dk.acto.fafnir.sso.provider.unilogin.UniloginTokenCredentials;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("unilogin")
@ConditionalOnProperty(name = {"UL_CLIENT_ID", "UL_SECRET", "FAFNIR_URL", "UL_WS_USER", "UL_WS_PASS"})
public class UniLoginController {
    private final UniLoginProvider provider;
    private final FafnirConf uniloginConf;

    @GetMapping
    public RedirectView authenticate(HttpSession session) throws NoSuchAlgorithmException {
        return new RedirectView(provider.authenticate(session));
    }

    @PostMapping("callback")
    public RedirectView callback(@RequestParam("code") String code, HttpSession session) throws IOException {
        return new RedirectView(provider.callback(UniloginTokenCredentials.builder()
            .code(code)
            .build(), session).getUrl(uniloginConf));
    }

    @GetMapping("org")
    public String getOrg(@RequestParam String user, Model model, HttpServletResponse response, HttpSession session) throws IOException {
        log.info("getOrg called for user: {}", user);
        
        // Try to get institutions from UserInfo in session first, fallback to deprecated web service
        var institutionList = Try.of(() -> provider.getInstitutionListFromSession(user, session)).getOrElse(Collections.emptyList());

        log.info("Retrieved {} institution(s) for user: {}", institutionList.size(), user);
        
        // Log each institution's details
        for (int i = 0; i < institutionList.size(); i++) {
            var inst = institutionList.get(i);
            log.info("Institution[{}]: id={}, name={}, roles={}, class={}", 
                i, inst.id, inst.name, inst.roles, inst.getClass().getName());
            
            // Verify fields are accessible
            try {
                var idValue = inst.id;
                var nameValue = inst.name;
                var rolesValue = inst.roles;
                log.debug("Field access test - id: {}, name: {}, roles: {}", idValue, nameValue, rolesValue);
            } catch (Exception e) {
                log.error("Error accessing Institution fields", e);
            }
        }

        if (institutionList.isEmpty()) {
            log.warn("No institutions found for user: {}, redirecting to failure URL", user);
            response.sendRedirect(provider.getFailureUrl(FailureReason.CONNECTION_FAILED));
            return null;
        } else {
            log.info("Adding {} institution(s) to model for template rendering", institutionList.size());
            model.addAttribute("user", user);
            model.addAttribute("institutionList", institutionList);
            log.info("Model attributes set - user: {}, institutionList size: {}", user, institutionList.size());
            return "uni_login_oidc_institution_chooser_da";
        }
    }


    @PostMapping("org")
    @ResponseBody
    public RedirectView postOrg(@RequestParam String user, @RequestParam String institution, 
                                 HttpSession session, HttpServletResponse response) {
        AuthenticationResult result = provider.callbackWithInstitution(user, institution, null, session);
        String redirectUrl = result.getUrl(uniloginConf);
        
        // If redirecting to logout, also set a cookie with the token as backup (in case session is invalidated)
        if (redirectUrl != null && redirectUrl.contains("/unilogin/logout-complete")) {
            String token = session != null ? (String) session.getAttribute("logout_token") : null;
            if (token != null) {
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("fafnir_logout_token", token);
                cookie.setPath("/");
                cookie.setMaxAge(300); // 5 minutes
                cookie.setHttpOnly(true);
                cookie.setSecure(true); // Use secure cookies in production
                response.addCookie(cookie);
                log.debug("Set logout token cookie as backup");
            }
        }
        
        return new RedirectView(redirectUrl);
    }

    /**
     * Completion endpoint after UniLogin logout.
     * This is called after the user has been logged out from UniLogin.
     * Retrieves the JWT from cache using one-time token stored in session or cookie and redirects to the success page.
     * 
     * Access at: GET /unilogin/logout-complete
     * Uses session/cookie to store token (no token in URL) to avoid UniLogin redirect URI validation issues.
     */
    @GetMapping("logout-complete")
    public RedirectView logoutComplete(HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
        log.info("Logout complete callback - retrieving JWT from cache");
        
        // Try to get token from session first
        String token = session != null ? (String) session.getAttribute("logout_token") : null;
        
        // Fallback to cookie if session doesn't have it
        if ((token == null || token.isEmpty()) && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("fafnir_logout_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    log.debug("Retrieved logout token from cookie");
                    break;
                }
            }
        }
        
        if (token == null || token.isEmpty()) {
            log.warn("No logout token found in session or cookie after logout, redirecting to failure");
            return new RedirectView(uniloginConf.getFailureRedirect() + "#" + dk.acto.fafnir.api.model.FailureReason.AUTHENTICATION_FAILED.getErrorCode());
        }
        
        // Clear the token from session and cookie
        if (session != null) {
            session.removeAttribute("logout_token");
        }
        
        // Retrieve the JWT from cache using one-time token
        String jwt = provider.retrieveJwtFromCache(token);
        
        if (jwt != null && !jwt.isEmpty()) {
            log.debug("JWT retrieved from cache, redirecting to success page");
            // Redirect to success page with JWT in hash
            return new RedirectView(uniloginConf.getSuccessRedirect() + "#" + jwt);
        } else {
            log.warn("No valid JWT found in cache after logout (token may be expired or invalid), redirecting to failure");
            return new RedirectView(uniloginConf.getFailureRedirect() + "#" + dk.acto.fafnir.api.model.FailureReason.AUTHENTICATION_FAILED.getErrorCode());
        }
    }

    /**
     * RP-initiated logout endpoint.
     * Redirects the user to UniLogin's end_session endpoint to log them out.
     * After logout at UniLogin, the user will be redirected back to the post_logout_redirect_uri.
     * 
     * Access at: GET /unilogin/logout
     * Optional parameters:
     *   - post_logout_redirect_uri: URL to redirect to after logout (defaults to FAFNIR_URL)
     *   - id_token_hint: ID token hint for better UX (optional)
     */
    @GetMapping("logout")
    public RedirectView logout(
            @RequestParam(required = false) String post_logout_redirect_uri,
            @RequestParam(required = false) String id_token_hint,
            HttpSession session) {
        log.info("RP-initiated logout requested");
        
        // Invalidate local session first
        provider.invalidateSession(session);
        
        // Build logout URL and redirect to UniLogin
        String logoutUrl = provider.getLogoutUrl(post_logout_redirect_uri, id_token_hint);
        log.debug("Redirecting to UniLogin logout endpoint: {}", logoutUrl);
        
        return new RedirectView(logoutUrl);
    }

    /**
     * Back-channel logout endpoint.
     * Receives logout events from UniLogin via HTTP POST when a user logs out.
     * This endpoint should be registered in Udbyderportalen as the "Back-Channel Logout URI".
     * 
     * Access at: POST /unilogin/logout/backchannel
     * 
     * According to OIDC Back-Channel Logout specification:
     * - The request contains a "logout_token" parameter (JWT)
     * - The token should be validated (signature, issuer, audience)
     * - The session identified by the token should be invalidated
     */
    @PostMapping("logout/backchannel")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> backChannelLogout(
            @RequestParam("logout_token") String logoutToken) {
        log.info("Received back-channel logout event from UniLogin");
        
        boolean success = provider.handleBackChannelLogout(logoutToken);
        
        if (success) {
            // Return 200 OK as per OIDC Back-Channel Logout spec
            return org.springframework.http.ResponseEntity.ok().build();
        } else {
            // Return 400 Bad Request if logout token is invalid
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }

    /**
     * Front-channel logout endpoint.
     * Handles browser-based logout notifications from UniLogin.
     * This is used when UniLogin sends logout notifications via iframe or redirect.
     * 
     * Access at: GET /unilogin/logout/frontchannel
     * 
     * According to OIDC Front-Channel Logout specification:
     * - The request may contain "iss" (issuer) and "sid" (session ID) parameters
     * - The session should be invalidated
     * - A simple HTML page should be returned (typically blank or with a script to clear cookies)
     */
    @GetMapping("logout/frontchannel")
    public String frontChannelLogout(
            @RequestParam(required = false) String iss,
            @RequestParam(required = false) String sid,
            HttpSession session) {
        log.info("Received front-channel logout event from UniLogin - iss: {}, sid: {}", iss, sid);
        
        // Invalidate local session
        provider.invalidateSession(session);
        
        // Return a simple HTML page that clears any cookies and closes iframe if needed
        // The template will be created to handle this
        return "uni_login_logout_frontchannel";
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Unilogin OIDC Endpoint...");
    }
}
