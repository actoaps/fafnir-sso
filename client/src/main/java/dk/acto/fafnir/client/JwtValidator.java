package dk.acto.fafnir.client;

import com.google.common.io.BaseEncoding;
import dk.acto.fafnir.api.exception.InvalidPublicKey;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.client.providers.AuthoritiesProvider;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class JwtValidator {
    private static final Pattern auth = Pattern.compile("^([Bb]earer\\s+)?(.+)$");
    private final PublicKeyProvider publicKeyProvider;
    private final AuthoritiesProvider ap;

    public JwtAuthentication decodeToken(String authHeader) {
        var decoder = Optional.of(publicKeyProvider.getPublicKey())
                .map(BaseEncoding.base64()::decode)
                .map(X509EncodedKeySpec::new)
                .map(x -> Try.of(() -> KeyFactory.getInstance("RSA"))
                        .mapTry(y -> y.generatePublic(x))
                        .toJavaOptional()
                        .orElseThrow(InvalidPublicKey::new))
                .map(Jwts.parser()::setSigningKey)
                .orElseThrow(InvalidPublicKey::new);

        var claims = Try.of(() -> auth.matcher(authHeader))
            .filter(Matcher::matches)
            .map(x -> x.group(2))
            .mapTry(decoder::parseClaimsJws)
            .map(Jwt::getBody).getOrNull();

        return Optional.ofNullable(claims).map(c -> JwtAuthentication.builder()
                .details(mapClaims(claims))
                .authorities(ap.mapAuthorities(claims))
                .build())
                .orElse(null);
    }

    private FafnirUser mapClaims(Claims claims) {
        return FafnirUser.builder()
                .data(UserData.builder()
                        .subject(claims.getSubject())
                        .name(claims.get("name", String.class))
                        .locale(Optional.ofNullable(claims.get("locale", String.class)).map(Locale::forLanguageTag).orElse(null))
                        .metaId(claims.get("mId", String.class))
                        .created(claims.getIssuedAt().toInstant())
                        .build())
                .organisationId(claims.get("org_id", String.class))
                .organisationName(claims.get("org_name", String.class))
                .provider(claims.getIssuer())
                .roles(mapRoles(claims.get("role")))
                .build();
    }

    private String[] mapRoles (Object roles) {
        var builder = Stream.<String>builder();

        Optional.ofNullable(roles)
                .ifPresent(r -> Try.of(() -> (List<?>) r)
                        .forEach(list -> list
                                .forEach(each -> Try.of(() -> (String) each)
                                        .forEach(builder))));

        return builder.build().toArray(String[]::new);
    }
}
