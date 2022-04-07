package dk.acto.fafnir.api.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@Builder
public class Slice<T> {
    private static final Integer PAGE_SIZE= 50;
    BigInteger totalPages;
    List<T> pageData;

    public static Long getOffset(Long pageNumber) {
        return pageNumber*PAGE_SIZE;
    }

    public static <T,R> Slice<R> fromPartial(final Stream<T> source, Long totalElements, Function<T,R> transform) {
        return Slice.<R>builder()
                .totalPages(BigDecimal.valueOf(totalElements).divide(BigDecimal.valueOf(PAGE_SIZE), RoundingMode.CEILING).toBigInteger())
                .pageData(source.map(transform)
                        .collect(Collectors.toList()))
                .build();
    }
}
