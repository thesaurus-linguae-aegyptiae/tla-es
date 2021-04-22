package tla.backend.search;

import java.util.Collection;
import java.util.Collections;

import lombok.Getter;
import lombok.Setter;
import tla.domain.command.SearchCommand;
import tla.domain.dto.meta.AbstractDto;

@Getter
@Setter
public class SearchTestSpecs {

    String name;

    SearchCommand<? extends AbstractDto> cmd;

    Collection<String> valid;

    Collection<String> invalid;

    public SearchTestSpecs() {
        this.valid = Collections.emptyList();
        this.invalid = Collections.emptyList();
    }

}
