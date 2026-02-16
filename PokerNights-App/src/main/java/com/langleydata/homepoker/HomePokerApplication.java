package com.langleydata.homepoker;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.langleydata.homepoker.services.AbstractGameServiceDiscovery;
import com.langleydata.homepoker.services.EurekaServiceDiscovery;
import com.langleydata.homepoker.services.MemoryServiceDiscovery;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class HomePokerApplication {
	final Logger logger = LoggerFactory.getLogger(HomePokerApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(HomePokerApplication.class, args);
	}
	// Public IP: 3.8.126.171
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		final RestTemplate template = builder
	            .setConnectTimeout(Duration.ofMillis(3000))
	            .setReadTimeout(Duration.ofMillis(3000))
	            .build();
	    
		return template;
	}

	@Bean
	@Profile( value = {"prod", "test"})
	public AbstractGameServiceDiscovery getEurekaServiceDiscovery() {
		return new EurekaServiceDiscovery();
	}
	
	@Bean
	@Profile( value = {"dev"})
	public AbstractGameServiceDiscovery getMemoryServiceDiscovery() {
		return new MemoryServiceDiscovery();
	}
}
