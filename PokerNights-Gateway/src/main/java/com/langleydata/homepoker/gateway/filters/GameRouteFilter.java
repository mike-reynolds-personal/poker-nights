package com.langleydata.homepoker.gateway.filters;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.exception.NoKnownServerException;
import com.langleydata.homepoker.services.AbstractGameServiceDiscovery;

/** The filter is invoked when there are two parts to the uri path. These parts
 * are used to lookup a game server for the game type that has been requested.
 * <p>If the path parts do not equate to a game and gameId then the root url of
 * a default server is returned.
 * 
 * @author Mike Reynolds
 *
 */
@Component
public class GameRouteFilter implements Function<ServerWebExchange, Optional<URI>> {

    final Logger logger = LoggerFactory.getLogger(GameRouteFilter.class);
	
	@Autowired
	private AbstractGameServiceDiscovery serviceDiscovery;
	@Autowired
	private DefaultServiceFilter defaultSerivceFilter;

	@Override
	public Optional<URI> apply(ServerWebExchange exchange) {
		
		Map<String, String> uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
		URI forwardTo = null;

		final String sGameType = uriVariables.get("gameType");
		final String gameId = uriVariables.get("gameId");
		final GameType gameType = GameType.fromPathPart(sGameType);
		GameServer server = null;
		
		try {
			
			server = serviceDiscovery.getByGameId(gameId, gameType);
			forwardTo  = UriComponentsBuilder.fromUriString(server.getUri()).pathSegment(sGameType, gameId).build().toUri();
			logger.debug("Directing to service: {}", forwardTo.toString());

		} catch (NoKnownServerException e) {
			
			return defaultSerivceFilter.apply(exchange);

		}
		
		return forwardTo !=null ? Optional.of(forwardTo) : Optional.empty();
	}


}
