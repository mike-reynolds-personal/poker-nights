package com.langleydata.homepoker.config;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer  {
	final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
	
	@Value("${websocket.connector}")
	private String brokerConnector;
	
	@Value("${websocket.jsLib}")
	private String jsLibrary;
	
	@Override
	protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) { 
	    //messages.anyMessage().authenticated(); // All messages must be authenticated
	}

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
    
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		final int brokerPort = Integer.parseInt(brokerConnector.substring(brokerConnector.lastIndexOf(":")+1));
		final String brokerRelay = brokerConnector.substring(brokerConnector.lastIndexOf("/")+1, brokerConnector.lastIndexOf(":"));
        config
        	.setApplicationDestinationPrefixes("/app")
        	.setPreservePublishOrder(true)
	        .setApplicationDestinationPrefixes("/app")
	        .enableStompBrokerRelay("/topic/", "/queue/")
		        .setRelayHost(brokerRelay)
		        .setRelayPort(brokerPort);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/home-poker-websocket")
				.setAllowedOrigins("*")
				.withSockJS()
				.setClientLibraryUrl(jsLibrary)
				.setStreamBytesLimit(512 * 1024)
				.setHttpMessageCacheSize(1000)
				.setDisconnectDelay(60 * 1000);
	}
	
    @Bean(initMethod = "start", destroyMethod = "stop")
    public BrokerService broker() throws Exception {
        final BrokerService broker = new BrokerService();
        broker.addConnector(brokerConnector
        		+ "?transport.useInactivityMonitor=true"
        		+ "&enableStatusMonitor=true"
        		+ "&transport.hbGracePeriodMultiplier=1.5");
        
        broker.setPersistent(false);

        final ManagementContext managementContext = new ManagementContext();
        managementContext.setCreateConnector(true);
        
        broker.setManagementContext(managementContext);

        return broker;
    }
}