package com.langleydata.homepoker.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameType;

/** Find all services registered with Eureka
 * 
 */
public class EurekaServiceDiscovery extends AbstractGameServiceDiscovery {

	@Autowired
	private DiscoveryClient discoveryClient;
	
	@Override
	public List<GameServer> lookupServices(GameType gameType) {
		
		List<ServiceInstance> services = discoveryClient.getInstances(gameType.name() + " SERVER");

		return services.stream()
			.map(si ->  new GameServer(si.getUri().toString(), gameType))
			.collect(Collectors.toList());
	}

}
