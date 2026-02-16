package com.langleydata.homepoker.config;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;

@Configuration
@Profile("prod")
public class AWSElasticsearchConfig extends CommonElasticConfig {
	
	@Value("${aws.es.service:es}")
	private String serviceName = null;
	
	@Value("${aws.es.endpoint}")
	private String endpoint = null;

	@Value("${aws.region:eu-west-2}")
	private String region = null;

	@Autowired
	private AWSCredentialsProvider credentialsProvider = null;

	@Bean
	@Override
	public RestHighLevelClient elasticsearchClient() {
		logger.info("Got props: {}, {}, {}", serviceName, region, endpoint);
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(serviceName);
		signer.setRegionName(region);
		
		logger.info("Creating Elastic Client with credentials: {}", credentialsProvider.getCredentials().getAWSAccessKeyId());
		
		final HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
		return new RestHighLevelClient(
				RestClient.builder(HttpHost.create(endpoint))
				.setHttpClientConfigCallback(e -> e.addInterceptorLast(interceptor))
				);
	}
	
    @Bean
    public AWSCredentialsProvider amazonAWSCredentialsProvider() {
        return new EC2ContainerCredentialsProviderWrapper();
    }
}
