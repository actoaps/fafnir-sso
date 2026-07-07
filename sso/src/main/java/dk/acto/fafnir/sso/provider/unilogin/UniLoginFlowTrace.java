package dk.acto.fafnir.sso.provider.unilogin;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public final class UniLoginFlowTrace {

    public static final String TRACE_UNIID_SESSION_KEY = "trace_uniid";
    public static final String LOGOUT_TOKEN_SESSION_KEY = "logout_token";
    public static final String LOGOUT_TOKEN_COOKIE_NAME = "fafnir_logout_token";

    public enum Branch {
        intro_failed,
        userinfo_ok,
        userinfo_empty,
        soap_fallback,
        dedup_applied,
        error_no_institutions,
        direct_jwt,
        direct_jwt_no_org,
        org_picker,
        org_empty_list,
        org_render,
        org_post,
        org_missing_user,
        callback_redirect,
        jwt_built,
        jwt_cached,
        logout_redirect,
        logout_complete_entry,
        logout_complete_no_token,
        logout_complete_cache_miss,
        logout_complete_success
    }

    private UniLoginFlowTrace() {
    }

    public static void log(String step, Branch branch, String uniid, HttpSession session, Map<String, Object> extra) {
        StringBuilder message = new StringBuilder("UniLogin trace: step=")
            .append(step)
            .append(" branch=")
            .append(branch)
            .append(" uniid=")
            .append(uniid != null ? uniid : "-")
            .append(" session=")
            .append(sessionId(session));

        if (session != null) {
            Object aktoerGruppe = session.getAttribute("aktoer_gruppe");
            if (aktoerGruppe != null) {
                message.append(" aktoer_gruppe=").append(aktoerGruppe);
            }
        }

        if (extra != null) {
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                message.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }

        log.info(message.toString());
    }

    public static String sessionId(HttpSession session) {
        if (session == null || session.getId() == null) {
            return "-";
        }
        String id = session.getId();
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    public static boolean hasUserInfoInSession(HttpSession session) {
        if (session == null) {
            return false;
        }
        UserInfoResponse userInfo = (UserInfoResponse) session.getAttribute("userInfo");
        return userInfo != null
            && userInfo.getInstBrugere() != null
            && !userInfo.getInstBrugere().isEmpty();
    }

    public static boolean hasLogoutTokenInSession(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object token = session.getAttribute(LOGOUT_TOKEN_SESSION_KEY);
        return token != null && !token.toString().isEmpty();
    }

    public static boolean hasLogoutCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie : request.getCookies()) {
            if (LOGOUT_TOKEN_COOKIE_NAME.equals(cookie.getName())
                && cookie.getValue() != null
                && !cookie.getValue().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static String cookieSummary(HttpServletRequest request) {
        if (request == null || request.getCookies() == null || request.getCookies().length == 0) {
            return "none";
        }
        boolean hasJsession = false;
        boolean hasLogoutCookie = false;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName() == null) {
                continue;
            }
            String name = cookie.getName().toLowerCase(Locale.ROOT);
            if ("jsessionid".equals(name)) {
                hasJsession = true;
            }
            if (LOGOUT_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                hasLogoutCookie = true;
            }
        }
        return "jsessionid=" + (hasJsession ? "yes" : "no")
            + ",fafnir_logout_token=" + (hasLogoutCookie ? "yes" : "no");
    }

    public static String sanitizeUrlForLog(String url) {
        if (url == null || url.isEmpty()) {
            return "-";
        }
        try {
            URI uri = new URI(url);
            StringBuilder sanitized = new StringBuilder();
            if (uri.getScheme() != null) {
                sanitized.append(uri.getScheme()).append("://");
            }
            if (uri.getHost() != null) {
                sanitized.append(uri.getHost());
            }
            if (uri.getPort() > 0) {
                sanitized.append(':').append(uri.getPort());
            }
            if (uri.getPath() != null) {
                sanitized.append(uri.getPath());
            }
            if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
                sanitized.append('?');
                String[] pairs = uri.getRawQuery().split("&");
                for (int i = 0; i < pairs.length; i++) {
                    if (i > 0) {
                        sanitized.append('&');
                    }
                    String pair = pairs[i];
                    int eq = pair.indexOf('=');
                    String paramName = eq >= 0 ? pair.substring(0, eq) : pair;
                    sanitized.append(paramName).append('=');
                    if ("id_token_hint".equals(paramName)) {
                        sanitized.append("[redacted]");
                    } else if (eq >= 0 && eq < pair.length() - 1) {
                        String value = pair.substring(eq + 1);
                        sanitized.append(value.length() > 32 ? value.substring(0, 32) + "..." : value);
                    }
                }
            }
            return sanitized.toString();
        } catch (URISyntaxException e) {
            return "[invalid-url]";
        }
    }

    public static String classifyCallbackRedirect(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            return "unknown";
        }
        String lower = redirectUrl.toLowerCase(Locale.ROOT);
        if (lower.contains("/unilogin/org")) {
            return "org_picker";
        }
        if (lower.contains("loginerror") || lower.contains("#400") || lower.contains("#500")) {
            return "failure";
        }
        if (lower.contains("/logout") || lower.contains("unilogin.dk")) {
            return "broker_logout";
        }
        if (lower.contains("loginredirect")) {
            return "success_jwt";
        }
        return "other";
    }

    public static String instanceId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String missingCsv(List<String> missing) {
        if (missing == null || missing.isEmpty()) {
            return "none";
        }
        return String.join(",", missing);
    }

    public static List<String> logoutRedirectMissing(
            HttpSession session,
            String fafnirUrl,
            String postLogoutRedirectUri,
            String idTokenHint) {
        List<String> missing = new ArrayList<>();
        if (session == null) {
            missing.add("session");
        } else {
            if (!hasLogoutTokenInSession(session)) {
                missing.add("logout_token_in_session");
            }
            if (uniidFromSession(session) == null) {
                missing.add("trace_uniid");
            }
            Object idToken = session.getAttribute("id_token");
            if (idToken == null || idToken.toString().isEmpty()) {
                missing.add("id_token_in_session");
            }
        }
        if (fafnirUrl == null || fafnirUrl.isBlank()) {
            missing.add("fafnir_url");
        }
        if (postLogoutRedirectUri == null || postLogoutRedirectUri.isBlank()) {
            missing.add("post_logout_redirect_uri");
        }
        if (idTokenHint == null || idTokenHint.isEmpty()) {
            missing.add("id_token_hint");
        }
        return missing;
    }

    public static List<String> logoutCompleteMissing(
            HttpSession session,
            HttpServletRequest request,
            String uniid,
            String token) {
        List<String> missing = new ArrayList<>();
        if (uniid == null || uniid.isBlank()) {
            missing.add("trace_uniid");
        }
        if (session == null) {
            missing.add("session");
        } else if (session.isNew()) {
            missing.add("session_not_established");
        }
        if (token == null || token.isEmpty()) {
            missing.add("logout_token");
        }
        if (request == null) {
            missing.add("request");
        } else {
            if (!hasLogoutCookie(request)) {
                missing.add("fafnir_logout_cookie");
            }
            String summary = cookieSummary(request);
            if (summary.equals("none") || summary.contains("jsessionid=no")) {
                missing.add("jsessionid_cookie");
            }
            String referer = request.getHeader("Referer");
            if (referer == null || referer.isBlank()) {
                missing.add("referer");
            }
        }
        return missing;
    }

    public static List<String> callbackRedirectMissing(String redirectUrl, String uniid, HttpSession session) {
        List<String> missing = new ArrayList<>();
        if (redirectUrl == null || redirectUrl.isBlank()) {
            missing.add("redirect_url");
        }
        if (uniid == null || uniid.isBlank()) {
            missing.add("trace_uniid");
        }
        if (session == null) {
            missing.add("session");
        }
        return missing;
    }

    public static void warnIfMissing(String step, String uniid, HttpSession session, List<String> missing) {
        if (missing == null || missing.isEmpty()) {
            return;
        }
        log.warn("UniLogin trace: step={} uniid={} session={} handoff_incomplete missing={}",
            step, uniid != null ? uniid : "-", sessionId(session), missingCsv(missing));
    }

    public static Map<String, Object> logoutCompleteEntryFields(HttpSession session, HttpServletRequest request) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("session_is_new", session != null && session.isNew());
        fields.put("session_has_logout_token", hasLogoutTokenInSession(session));
        fields.put("has_fafnir_logout_cookie", hasLogoutCookie(request));
        fields.put("cookie_summary", cookieSummary(request));
        if (request != null) {
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                fields.put("referer", sanitizeUrlForLog(referer));
            }
            String query = request.getQueryString();
            if (query != null && !query.isEmpty()) {
                fields.put("query_string", query);
            }
            fields.put("remote_addr", request.getRemoteAddr());
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                fields.put("x_forwarded_for", forwarded);
            }
        }
        fields.put("instance", instanceId());
        fields.put("missing", missingCsv(logoutCompleteMissing(session, request, uniidFromSession(session), null)));
        return fields;
    }

    public static List<String> instnrList(List<Institution> institutions) {
        if (institutions == null || institutions.isEmpty()) {
            return Collections.emptyList();
        }
        return institutions.stream()
            .map(inst -> inst.id)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toList());
    }

    public static String tokenPrefix(String token) {
        if (token == null || token.isEmpty()) {
            return "-";
        }
        return token.substring(0, Math.min(8, token.length()));
    }

    public static void rememberUniid(HttpSession session, String uniid) {
        if (session != null && uniid != null) {
            session.setAttribute(TRACE_UNIID_SESSION_KEY, uniid);
        }
    }

    public static String uniidFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(TRACE_UNIID_SESSION_KEY);
        return value != null ? value.toString() : null;
    }
}
