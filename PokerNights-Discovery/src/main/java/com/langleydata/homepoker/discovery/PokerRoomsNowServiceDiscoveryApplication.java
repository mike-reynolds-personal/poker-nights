package com.langleydata.homepoker.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.netflix.appinfo.AmazonInfo;

@SpringBootApplication
@EnableEurekaServer
public class PokerRoomsNowServiceDiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokerRoomsNowServiceDiscoveryApplication.class, args);
	}

	@Bean
	@Profile("prod")
	public EurekaInstanceConfigBean eurekaInstanceConfig(InetUtils inetUtils) {
	  final EurekaInstanceConfigBean b = new EurekaInstanceConfigBean(inetUtils);
	  final AmazonInfo info = AmazonInfo.Builder.newBuilder().autoBuild("eureka");
	  b.setDataCenterInfo(info);
	  return b;
	}
	
    @Bean
    public AWSCredentialsProvider amazonAWSCredentialsProvider() {
        return new EC2ContainerCredentialsProviderWrapper();
    }
}
