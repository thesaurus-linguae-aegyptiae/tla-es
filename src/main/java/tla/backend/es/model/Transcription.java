package tla.backend.es.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transcription {

    private String unicode;
    private String mdc;

    //TODO hieroglyphs?
}