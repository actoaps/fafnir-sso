package dk.acto.fafnir.iam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

@Builder
@Value
public class PageData {
    BigInteger number;
    String url;
}
