package dk.acto.fafnir.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DataUrl {
    private String dataUrl;
}
