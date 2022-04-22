package dk.acto.fafnir.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

@Builder
@Value
@AllArgsConstructor
public class PageData {
    BigInteger number;
    String url;
}
