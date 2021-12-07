package dk.acto.fafnir.server.crypto;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.client.FafnirClientConfiguration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {"KEYSTORE_PASS=keystorepassword", "KEY_PASS=keypassword"})
@Import(FafnirClientConfiguration.class)
class KeyStoreKeyManagerTest {
    @Autowired
    private RsaKeyManager rsaKeyManager;

    @BeforeAll
    static void beforeAll() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void isRightBean() {
        assertThat(rsaKeyManager).isInstanceOf(KeyStoreKeyManager.class);
    }
}
