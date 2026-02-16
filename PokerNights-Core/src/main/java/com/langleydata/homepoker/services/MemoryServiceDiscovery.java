package com.langleydata.homepoker.services;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameType;

/** Memory backed service discovery service.
 * <p>Need to provide an @Bean to enable
 * 
 * @author Mike Reynolds
 *
 */
public class MemoryServiceDiscovery extends AbstractGameServiceDiscovery {

	@Value("${game-server.test-server:http://localhost:8080}")
	private String gameServer;
	
	@Override
	public List<GameServer> lookupServices(GameType gameType) {
		return Collections.singletonList(new GameServer(gameServer, GameType.TEXAS_HOLDEM));
	}

}
