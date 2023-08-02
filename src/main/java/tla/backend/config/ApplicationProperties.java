package tla.backend.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "tla")
public class ApplicationProperties implements WebMvcConfigurer {

    private ElasticsearchProperties es;

    @Data
    public static class ElasticsearchProperties {
        /**
         * Elasticsearch port
         */
        private int port;
        /**
         * Elasticsearch host address
         */
        private String host;
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
    	 System.out.println("Adding CORS mappings...");
        registry.addMapping("/**") // Allow CORS for all paths
                .allowedOrigins("http://localhost:8080") // Specify the allowed origin
                .allowedMethods("GET", "POST", "PUT", "DELETE") // Specify allowed HTTP methods
                .allowedHeaders("*") // Allow all headers
                 .allowCredentials(true);
    }
}
