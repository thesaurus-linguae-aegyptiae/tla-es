package tla.backend.es.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacetSpec {

    private String name;
    private String field;
    private String script;

    public FacetSpec(String name) {
        this.name = name;
    }

    public FacetSpec field(String field) {
        this.field = field;
        return this;
    }

    public FacetSpec script(String script) {
        this.script = script;
        return this;
    }

    public static FacetSpec field(String name, String field) {
        return new FacetSpec(name).field(field);
    }

    public static FacetSpec script(String name, String script) {
        return new FacetSpec(name).script(script);
    }

}