package com.langleydata.homepoker.gateway.filters;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.services.AbstractGameServiceDiscovery;

/** This is a default server filter that catches all requests that aren't for a specific game
 * table and forwards them to any** service that can process them.
 * 
 * <p>Currently this only supports getting from the Texas Hold'em services, as its the only one that exists, 
 * but it should be changed to get a specific game type server, but that will still be based on a part
 * of the uri path.
 * 
 * @author Mike Reynolds
 *
 */
@Component
public class DefaultServiceFilter implements Function<ServerWebExchange, Optional<URI>> {

    final Logger logger = LoggerFactory.getLogger(GameRouteFilter.class);
	
	@Autowired
	private AbstractGameServiceDiscovery serviceDiscovery;
	
	@Override
	public Optional<URI> apply(ServerWebExchange exchange) {
		//TODO Capture the requested game type and then lookup just for that
		final GameServer server = serviceDiscovery.getLeastLoaded(GameType.TEXAS_HOLDEM, true);
		
		// not working as should only redirect the page being looked for, not all other assets
//		if (server.getUri().endsWith("unknown")) {
//			// didn't find a server, so show error page
//			return Optional.of(URI.create(exchange.getRequest().getURI().toString() + "/noserver"));
//		}
//		
		final URI forwardTo = UriComponentsBuilder.fromUriString(server.getUri())
			.path(exchange.getRequest().getPath().toString())
			.queryParams(exchange.getRequest().getQueryParams())
			.build()
			.toUri();
		return server==null ? Optional.empty() : Optional.of(forwardTo);
	}

}
