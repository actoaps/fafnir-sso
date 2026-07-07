package dk.acto.fafnir.sso.provider.unilogin;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UniLoginFlowTraceTest {

    @Test
    void instnrList_extractsNonBlankIds() {
        List<Institution> institutions = List.of(
            new Institution("12345", "Skole A", List.of()),
            new Institution("", "Blank", List.of()),
            new Institution("67890", "Skole B", List.of())
        );

        assertThat(UniLoginFlowTrace.instnrList(institutions)).containsExactly("12345", "67890");
    }

    @Test
    void instnrList_returnsEmptyForNullOrEmpty() {
        assertThat(UniLoginFlowTrace.instnrList(null)).isEmpty();
        assertThat(UniLoginFlowTrace.instnrList(List.of())).isEmpty();
    }

    @Test
    void sessionId_returnsPrefixOrDash() {
        assertThat(UniLoginFlowTrace.sessionId(null)).isEqualTo("-");
    }

    @Test
    void tokenPrefix_returnsPrefixOrDash() {
        assertThat(UniLoginFlowTrace.tokenPrefix(null)).isEqualTo("-");
        assertThat(UniLoginFlowTrace.tokenPrefix("")).isEqualTo("-");
        assertThat(UniLoginFlowTrace.tokenPrefix("AbCdEfGhIj")).isEqualTo("AbCdEfGh");
    }

    @Test
    void hasUserInfoInSession_returnsFalseWhenSessionNull() {
        assertThat(UniLoginFlowTrace.hasUserInfoInSession(null)).isFalse();
    }

    @Test
    void hasLogoutTokenInSession_returnsFalseWhenSessionNull() {
        assertThat(UniLoginFlowTrace.hasLogoutTokenInSession(null)).isFalse();
    }

    @Test
    void hasLogoutTokenInSession_returnsTrueWhenTokenPresent() {
        HttpSession session = Mockito.mock(HttpSession.class);
        when(session.getAttribute(UniLoginFlowTrace.LOGOUT_TOKEN_SESSION_KEY)).thenReturn("abc123");

        assertThat(UniLoginFlowTrace.hasLogoutTokenInSession(session)).isTrue();
    }

    @Test
    void sanitizeUrlForLog_redactsIdTokenHint() {
        String sanitized = UniLoginFlowTrace.sanitizeUrlForLog(
            "https://broker.unilogin.dk/logout?client_id=abc&id_token_hint=secret.jwt.value&post_logout_redirect_uri=https%3A%2F%2Fexample.com%2Fdone");

        assertThat(sanitized).contains("id_token_hint=[redacted]");
        assertThat(sanitized).doesNotContain("secret.jwt.value");
        assertThat(sanitized).contains("broker.unilogin.dk");
    }

    @Test
    void cookieSummary_reportsPresenceFlagsOnly() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[] {
            new Cookie("JSESSIONID", "session-value"),
            new Cookie(UniLoginFlowTrace.LOGOUT_TOKEN_COOKIE_NAME, "token-value")
        });

        assertThat(UniLoginFlowTrace.cookieSummary(request)).isEqualTo("jsessionid=yes,fafnir_logout_token=yes");
        assertThat(UniLoginFlowTrace.hasLogoutCookie(request)).isTrue();
    }

    @Test
    void cookieSummary_returnsNoneWhenNoCookies() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        assertThat(UniLoginFlowTrace.cookieSummary(request)).isEqualTo("none");
        assertThat(UniLoginFlowTrace.hasLogoutCookie(request)).isFalse();
    }

    @Test
    void classifyCallbackRedirect_detectsBrokerLogout() {
        assertThat(UniLoginFlowTrace.classifyCallbackRedirect("https://broker.unilogin.dk/logout?client_id=x"))
            .isEqualTo("broker_logout");
    }

    @Test
    void logoutRedirectMissing_listsAbsentHandoffFields() {
        List<String> missing = UniLoginFlowTrace.logoutRedirectMissing(null, "", "", null);

        assertThat(missing).contains(
            "session",
            "fafnir_url",
            "post_logout_redirect_uri",
            "id_token_hint"
        );
    }

    @Test
    void logoutCompleteMissing_listsAbsentTokenAndCookies() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        List<String> missing = UniLoginFlowTrace.logoutCompleteMissing(null, request, null, null);

        assertThat(missing).contains(
            "trace_uniid",
            "session",
            "logout_token",
            "fafnir_logout_cookie",
            "jsessionid_cookie",
            "referer"
        );
        assertThat(UniLoginFlowTrace.missingCsv(missing)).doesNotContain("none");
    }

    @Test
    void missingCsv_returnsNoneWhenEmpty() {
        assertThat(UniLoginFlowTrace.missingCsv(List.of())).isEqualTo("none");
    }
}
