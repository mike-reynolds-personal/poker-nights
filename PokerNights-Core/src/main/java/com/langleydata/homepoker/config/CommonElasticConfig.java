package com.langleydata.homepoker.config;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import com.langleydata.homepoker.persistence.ElasticSettings;

@EnableElasticsearchRepositories(basePackages = "com.langleydata.homepoker.persistence")
public abstract class CommonElasticConfig extends AbstractElasticsearchConfiguration {
	protected final Logger logger = LoggerFactory.getLogger(CommonElasticConfig.class);
    
    @PostConstruct
    protected void createDefaultIndices() {
    	logger.info("Checking for required ES indices...");
    	
        // Create any missing indices
        for (String IDX : ElasticSettings.INDEXES) {
        	try {
				if (!elasticsearchClient().indices().exists(new GetIndexRequest(IDX), RequestOptions.DEFAULT)) {
					createIndex(elasticsearchClient(), IDX);
				}
        	} catch (IOException e) {
        		logger.error("Getting indices", e);
        	}
        }
    }
    
    
    /** Create an ES index
     * 
     * @param esClient
     * @param index
     */
    protected void createIndex(RestHighLevelClient esClient, final String index) {
    	esClient.indices().createAsync(new CreateIndexRequest(index), RequestOptions.DEFAULT, new ActionListener<CreateIndexResponse>() {
			@Override
			public void onResponse(CreateIndexResponse response) {
				logger.info(response.toString());
			}
			@Override
			public void onFailure(Exception e) {
				logger.error("Creating ES index", e);
			}
		});
    }
    
    @Bean(name = {"elasticsearchOperations", "elasticsearchTemplate"})
	public ElasticsearchOperations elasticsearchOperations() {
	    return new ElasticsearchRestTemplate(elasticsearchClient());
	}
}
