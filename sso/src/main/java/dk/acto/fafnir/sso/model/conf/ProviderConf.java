package dk.acto.fafnir.sso.model.conf;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderConf {
    Boolean lowercaseSubject;

    public String applySubjectRules(final String subject) {
        if (Boolean.TRUE.equals(lowercaseSubject)) {
            return subject.toLowerCase();
        }

        return subject;
    }
}
