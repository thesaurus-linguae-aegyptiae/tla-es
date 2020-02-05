package tla.backend.es.repo;

import java.net.InetSocketAddress;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories
public class RepoConfig extends AbstractElasticsearchConfiguration {

    @Bean
    public ElasticsearchRestTemplate elasticsearchRestTemplate() {
        return new ElasticsearchRestTemplate(
            elasticsearchClient()
        );
    }

    @Override
    public RestHighLevelClient elasticsearchClient() {
        return RestClients.create(
            ClientConfiguration.create(InetSocketAddress.createUnresolved("localhost", 9201))
        ).rest();
    }

}