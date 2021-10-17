package dk.acto.fafnir.server.crypto;

import dk.acto.fafnir.client.FafnirClientConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(FafnirClientConfiguration.class)
class InMemoryKeyManagerTest {
    @Autowired
    private RsaKeyManager rsaKeyManager;

    @Test
    void isRightBean() {
        assertThat(rsaKeyManager).isInstanceOf(InMemoryKeyManager.class);
    }
}
