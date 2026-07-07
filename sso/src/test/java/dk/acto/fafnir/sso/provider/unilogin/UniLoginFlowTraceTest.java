package dk.acto.fafnir.sso.provider.unilogin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
