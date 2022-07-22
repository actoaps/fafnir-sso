package dk.acto.fafnir.client.providers.builtin;

import dk.acto.fafnir.api.exception.InvalidConfiguration;
import dk.acto.fafnir.api.exception.InvalidPublicKey;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
public class RestfulPublicKeyProvider implements PublicKeyProvider {
    final private String fafnirUrl;
    final private String fafnirPort;

    @Override
    public String getPublicKey() {
        var url = Try.of(() -> new URL(String.format("http://%s:%s/public-key", fafnirUrl, fafnirPort)))
                        .toJavaOptional()
                                .orElseThrow(InvalidConfiguration::new);

        return Try.withResources(url::openStream)
                        .of(InputStream::readAllBytes)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                                .toJavaOptional()
                .orElseThrow(InvalidPublicKey::new);
    }
}
