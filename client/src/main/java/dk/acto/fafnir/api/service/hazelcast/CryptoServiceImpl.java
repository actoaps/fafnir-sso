package dk.acto.fafnir.api.service.hazelcast;

import dk.acto.fafnir.api.service.CryptoService;
import dk.acto.fafnir.api.util.CryptoUtil;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

@Service
public class CryptoServiceImpl implements CryptoService {
    public final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public String encodePassword(final String password, final PublicKey publicKey) {
        var hashedPass = passwordEncoder.encode(password);

        return Optional.ofNullable(publicKey)
                .map(x -> CryptoUtil.encryptPassword(hashedPass, x))
                .orElse(hashedPass);
    }

    public Boolean matches(final String password, final String encoded, final PrivateKey privateKey) {
        var decoded = Optional.ofNullable(privateKey)
                .map(x -> CryptoUtil.decryptPassword(encoded, x))
                .orElse(encoded);

        return passwordEncoder.matches(password, decoded);
    }
}
