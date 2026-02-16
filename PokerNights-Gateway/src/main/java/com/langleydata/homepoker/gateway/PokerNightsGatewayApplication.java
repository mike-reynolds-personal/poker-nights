package com.langleydata.homepoker.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.langleydata.homepoker.gateway.filters.DefaultServiceFilter;
import com.langleydata.homepoker.gateway.filters.GameRouteFilter;
import com.langleydata.homepoker.services.AbstractGameServiceDiscovery;
import com.langleydata.homepoker.services.EurekaServiceDiscovery;
import com.langleydata.homepoker.services.MemoryServiceDiscovery;


@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.langleydata.homepoker")
public class PokerNightsGatewayApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(PokerNightsGatewayApplication.class, args);
	}
	
	@Autowired
	private GameRouteFilter gameRoute;
	@Autowired
	private DefaultServiceFilter defaultService;
	
	@Bean
	RouteLocator getGatewayRoutes(RouteLocatorBuilder builder) {

		return builder.routes()
				// Handle game table based urls to the same game server
				.route("RedirectGames", p ->
					p.path("/{gameType}/{gameId}")
					.filters(f -> f.changeRequestUri(gameRoute))
					.uri("no://op") )
				
				// This route handles the root context of the url (to any game server)
				.route("RootContent", p ->
					p.path("/**")
					.filters(f -> f.changeRequestUri(defaultService))
					.uri("no://op") )
				.build();
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

	/** Create a bean to serve static resources. All access (from pages) should be prefixed
	 * with 'gateway-content' so it isn't confused trying to server stuff routed for the microservices
	 */
	@Bean
	RouterFunction<ServerResponse> staticResourceRouter(){
	    return RouterFunctions.resources("/gateway-content/**", new ClassPathResource("static/"));
	}
}

