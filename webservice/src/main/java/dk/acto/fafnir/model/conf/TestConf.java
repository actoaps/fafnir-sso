package dk.acto.fafnir.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.AssertTrue;

@Value
@AllArgsConstructor
public class TestConf {
    @AssertTrue
    boolean enabled;
}
