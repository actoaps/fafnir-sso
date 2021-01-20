package dk.acto.fafnir.client;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.util.CryptoUtil;
import io.vavr.control.Try;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

@Value
public class FafnirClient {
    HazelcastInstance hazelcastInstance;
    HazelcastConf hazelcastConf;
    @NonFinal PublicKey publicKey;
    PublicKeyProvider publicKeyProvider;

    public FafnirClient(HazelcastInstance hazelcastInstance, PublicKeyProvider publicKeyProvider, HazelcastConf hazelcastConf) {
        this.hazelcastInstance = hazelcastInstance;
        this.hazelcastConf = hazelcastConf;
        this.publicKeyProvider = publicKeyProvider;
    }

    public void exportToFafnir(FafnirUser user) {
        IMap<String, FafnirUser> userMap = hazelcastInstance.getMap(hazelcastConf.getMapName());
        var key= hazelcastConf.isUsernameIsEmail() ? user.getSubject().toLowerCase() : user.getSubject();
        userMap.put(key, user);
    }

    public void deleteFromFafnir (FafnirUser user) {
        IMap<String, String> userMap = hazelcastInstance.getMap(hazelcastConf.getMapName());
        userMap.remove(user.getSubject());
    }

    public FafnirUser toSecureUser(FafnirUser source) {
        return hazelcastConf.isPasswordIsEncrypted() ?
                source.toBuilder()
                        .password(CryptoUtil.encryptPassword(source.getPassword(), this.getPublicKey()))
                        .build() :
                source.toBuilder()
                        .password(CryptoUtil.hashPassword(source.getPassword()))
                        .build();
    }

    public PublicKey getPublicKey () {
        return Optional.ofNullable(this.publicKey)
                .or(() -> Try.of(publicKeyProvider::getPublicKey)
                        .map(x -> Base64.getDecoder().decode(x))
                        .mapTry(x -> KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(x)))
                        .onSuccess(x -> this.publicKey = x)
                        .toJavaOptional())
                .orElse(null);
    }
}
