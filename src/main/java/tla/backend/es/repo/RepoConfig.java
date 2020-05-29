package tla.backend.es.repo;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import lombok.extern.slf4j.Slf4j;
import tla.domain.model.Passport;

@Slf4j
@Configuration
@EnableElasticsearchRepositories
public class RepoConfig extends AbstractElasticsearchConfiguration {

    @Autowired
    private Environment env;

    @Bean
    @Primary
    public ElasticsearchRestTemplate elasticsearchRestTemplate() {
        return new ElasticsearchRestTemplate(
            elasticsearchClient()
        );
    }

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        log.info
            ("create elasticsearch client for local instance at {}:{}",
            env.getProperty("tla.es.host"),
            env.getProperty("tla.es.port")
        );
        return RestClients.create(
            ClientConfiguration.create(
                InetSocketAddress.createUnresolved(
                    env.getProperty(
                        "tla.es.host",
                        "localhost"
                    ),
                    Integer.parseInt(
                        env.getProperty(
                            "tla.es.port",
                            "9200"
                        )
                    )
                )
            )
        ).rest();
    }

    @Bean
    public RepoPopulator repoPopulator() {
        return new RepoPopulator();
    }

    @Bean
    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
            List.of(new PassportToMap(), new MapToPassport())
        );
    }

    @WritingConverter
    @SuppressWarnings("unchecked")
    public static class PassportToMap implements Converter<Passport, Map<String, Object>> {

        private static ObjectMapper mapper = new ObjectMapper();

        @Override
        public Map<String, Object> convert(Passport source) {
            try {
                Map<String, Object> res = mapper.readValue(
                    mapper.writeValueAsString(source),
                    Map.class
                );
                return res;
            } catch (Exception e) {
                log.warn(
                    String.format(
                        "passport to map conversion failed for passport %s",
                        source
                    ),
                    e
                );
            }
            return null;
        }
    }

    @ReadingConverter
    public static class MapToPassport implements Converter<Map<String, Object>, Passport> {

        @Override
        public Passport convert(Map<String, Object> source) {
            return Passport.of(source);
        }

    }
}
