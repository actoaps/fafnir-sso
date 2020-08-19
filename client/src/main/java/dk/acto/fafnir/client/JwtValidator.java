package dk.acto.fafnir.client;

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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class JwtValidator {
    private static final Pattern auth = Pattern.compile("^([Bb]earer\\s+)?(.+)$");
    private final PublicKeyProvider pkp;
    private final AuthoritiesProvider ap;

    public JwtAuthentication decodeToken(String authHeader) {
        JwtParser decoder = Try.of(pkp::getPublicKey)
            .map(x -> Base64.getDecoder().decode(x))
            .mapTry(x -> KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(x)))
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

    private Map<String, String> mapClaims(Claims claims) {
        var result =  new TreeMap<String, String>();
        claims.forEach((k, v) -> result.put(k, String.valueOf(v)));
        return result;
    }
}
