package dk.acto.fafnir.client;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.model.conf.HazelcastConf;
import dk.acto.fafnir.util.CryptoUtil;
import io.vavr.control.Try;
import lombok.Value;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Value
public class FafnirClient {
    HazelcastInstance hazelcastInstance;
    HazelcastConf hazelcastConf;
    PublicKey publicKey;

    public FafnirClient(HazelcastInstance hazelcastInstance, PublicKeyProvider publicKeyProvider, HazelcastConf hazelcastConf) {
        this.hazelcastInstance = hazelcastInstance;
        this.hazelcastConf = hazelcastConf;
        this.publicKey = Try.of(publicKeyProvider::getPublicKey)
                .map(x -> Base64.getDecoder().decode(x))
                .mapTry(x -> KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(x)))
                .get();
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
                        .password(CryptoUtil.encryptPassword(source.getPassword(), publicKey))
                        .build() :
                source.toBuilder()
                        .password(CryptoUtil.hashPassword(source.getPassword()))
                        .build();
    }
}
