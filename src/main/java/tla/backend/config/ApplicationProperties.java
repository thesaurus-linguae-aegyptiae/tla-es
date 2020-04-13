package tla.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "tla")
public class ApplicationProperties {

    private ElasticsearchProperties es;

    @Data
    public static class ElasticsearchProperties {
        private int port;
        private String host;
    }
}
