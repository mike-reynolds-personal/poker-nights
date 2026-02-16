package com.langleydata.homepoker.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;

@Profile("test")
@Configuration
public class LocalElasticConfig extends CommonElasticConfig {
	
	@Value("${spring.data.elasticsearch.cluster-nodes}")
	private String elasticHost;
	
    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        final ClientConfiguration clientConfiguration 
            = ClientConfiguration.builder()
                .connectedTo(elasticHost)
                .build();

        return RestClients.create(clientConfiguration).rest();
        
    }

}
