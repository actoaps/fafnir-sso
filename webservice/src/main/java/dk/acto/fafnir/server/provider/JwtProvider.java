package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.model.UserData;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Lazy
public class JwtProvider {


    private final TokenFactory tokenFactory;
    public CallbackResult callback(UserData userData) {

        var subject = userData.getEmail();
        if (subject == null || subject.isEmpty()) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        return CallbackResult.success(tokenFactory.generateToken(FafnirUser.builder()
                .subject(subject)
                .provider("facebook")
                .name(userData.getName())
                .metaId(userData.getUserId())
                .build()));

    }
}
