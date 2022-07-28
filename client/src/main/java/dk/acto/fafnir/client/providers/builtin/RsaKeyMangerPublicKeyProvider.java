package dk.acto.fafnir.client.providers.builtin;

import com.google.common.io.BaseEncoding;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import lombok.AllArgsConstructor;

import java.util.Base64;

@AllArgsConstructor
public class RsaKeyMangerPublicKeyProvider implements PublicKeyProvider {
    private final RsaKeyManager rsaKeyManager;

    @Override
    public String getPublicKey() {
        return Base64.getEncoder().withoutPadding().encodeToString(
                rsaKeyManager.getPublicKey().getEncoded());
    }
}
