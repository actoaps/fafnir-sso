package dk.acto.fafnir.client;

import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.util.CryptoUtil;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import io.vavr.control.Try;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

@Value
@Deprecated(forRemoval = true, since = "3.0")
public class FafnirClient {
    HazelcastInstance hazelcastInstance;
    HazelcastConf hazelcastConf;
    @NonFinal
    PublicKey publicKey;
    PublicKeyProvider publicKeyProvider;

    public FafnirClient(HazelcastInstance hazelcastInstance, PublicKeyProvider publicKeyProvider, HazelcastConf hazelcastConf) {
        this.hazelcastInstance = hazelcastInstance;
        this.hazelcastConf = hazelcastConf;
        this.publicKeyProvider = publicKeyProvider;
    }

    public void exportToFafnir(FafnirUser user) {
//        IMap<String, FafnirUser> userMap = hazelcastInstance.getMap(hazelcastConf.getMapName());
//        var key = hazelcastConf.isUsernameIsEmail() ? user.getSubject().toLowerCase() : user.getSubject();
//        userMap.put(key, user);
    }

    public FafnirUser toSecureUser(FafnirUser source) {
        return source.toBuilder()
                .data(secureUser(source.getData()))
                .build();
    }

    public UserData secureUser(UserData source) {
        var crypto = hazelcastConf.isPasswordIsEncrypted() ? CryptoUtil.encryptPassword(source.getPassword(), this.getPublicKey()) : CryptoUtil.hashPassword(source.getPassword());
        return source.toBuilder()
                .password(crypto)
                .build();
    }

    public PublicKey getPublicKey() {
        return Optional.ofNullable(this.publicKey)
                .or(() -> Try.of(publicKeyProvider::getPublicKey)
                        .map(x -> Base64.getDecoder().decode(x))
                        .mapTry(x -> KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(x)))
                        .onSuccess(x -> this.publicKey = x)
                        .toJavaOptional())
                .orElse(null);
    }
}
