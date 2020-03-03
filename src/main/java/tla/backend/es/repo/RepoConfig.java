package tla.backend.es.repo;

import java.net.InetSocketAddress;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import lombok.extern.slf4j.Slf4j;

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

    @Override
    public RestHighLevelClient elasticsearchClient() {
        log.info("create elasticsearch client for local instance at port " + env.getProperty("tla.es.port"));
        return RestClients.create(
            ClientConfiguration.create(
                InetSocketAddress.createUnresolved(
                    "localhost", Integer.parseInt(env.getProperty("tla.es.port", "9200"))
                )
            )
        ).rest();
    }

}