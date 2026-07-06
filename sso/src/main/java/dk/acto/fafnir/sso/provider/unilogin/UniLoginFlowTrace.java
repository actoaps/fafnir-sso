package dk.acto.fafnir.sso.provider.unilogin;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public final class UniLoginFlowTrace {

    public static final String TRACE_UNIID_SESSION_KEY = "trace_uniid";

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
        jwt_built,
        jwt_cached,
        logout_redirect,
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
