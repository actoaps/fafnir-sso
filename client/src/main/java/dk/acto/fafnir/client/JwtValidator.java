package dk.acto.fafnir.client;

import dk.acto.fafnir.api.model.FafnirUser;
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
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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
    private final FafnirClient fafnirClient;
    private final AuthoritiesProvider ap;

    public JwtAuthentication decodeToken(String authHeader) {
        JwtParser decoder = Try.of(fafnirClient::getPublicKey)
            .map(x -> Jwts.parser().setSigningKey(x))
            .get();

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
                .subject(claims.getSubject())
                .name(claims.get("name", String.class))
                .locale(Optional.ofNullable(claims.get("locale", String.class)).map(Locale::forLanguageTag).orElse(null))
                .provider(claims.getIssuer())
                .metaId(claims.get("mId", String.class))
                .organisationId(claims.get("org_id", String.class))
                .organisationName(claims.get("org_name", String.class))
                .created(claims.getIssuedAt().toInstant())
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
