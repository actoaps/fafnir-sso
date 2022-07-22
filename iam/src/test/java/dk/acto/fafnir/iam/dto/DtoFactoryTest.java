package dk.acto.fafnir.iam.dto;

import dk.acto.fafnir.api.model.Slice;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DtoFactoryTest {

    @Test
    void calculatePageData() {
        var subject = new DtoFactory();
        var result = subject.calculatePageData(0L, Slice.PAGE_SIZE-1, "");
        assertThat(result).isNotNull();
        assertThat(result.get("nextUrl")).isNull();
        assertThat(result.get("prevUrl")).isNull();
        assertThat((Object[])result.get("pageData")).isEmpty();

        result = subject.calculatePageData(0L, Slice.PAGE_SIZE, "");
        assertThat(result).isNotNull();
        assertThat(result.get("nextUrl")).isNull();
        assertThat(result.get("prevUrl")).isNull();
        assertThat((Object[])result.get("pageData")).isEmpty();

        result = subject.calculatePageData(0L, Slice.PAGE_SIZE+1, "");
        assertThat(result).isNotNull();
        assertThat(result.get("nextUrl")).isNotNull();
        assertThat(result.get("prevUrl")).isNull();
        assertThat((Object[])result.get("pageData")).hasSize(1);

        result = subject.calculatePageData(1L, Slice.PAGE_SIZE+1, "");
        assertThat(result).isNotNull();
        assertThat(result.get("nextUrl")).isNull();
        assertThat(result.get("prevUrl")).isNotNull();
        assertThat((Object[])result.get("pageData")).hasSize(1);

        result = subject.calculatePageData(1L, Slice.PAGE_SIZE*2-1, "");
        assertThat(result).isNotNull();
        assertThat(result.get("nextUrl")).isNull();
        assertThat(result.get("prevUrl")).isNotNull();
        assertThat((Object[])result.get("pageData")).hasSize(1);

        result = subject.calculatePageData(1L, Slice.PAGE_SIZE*2+1, "");
        assertThat(result).isNotNull();
        assertThat(result.get("nextUrl")).isNotNull();
        assertThat(result.get("prevUrl")).isNotNull();
        assertThat((Object[])result.get("pageData")).hasSize(2);
    }
}
