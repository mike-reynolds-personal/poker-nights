package com.langleydata.homepoker.gateway.filters;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import com.langleydata.homepoker.api.UserAccount;

import reactor.core.publisher.Mono;

/** Used to add the authenticated user's userId to all inbound requests, which
 * can then be used by the downstream services to authenticate users against the 
 * account service.
 * 
 * @author Mike Reynolds
 *
 */
@Component
public class AuthenticationHeaderFilter implements GlobalFilter {
	static final String gatewayUrl = "https://localhost:8090";
    final Logger logger = LoggerFactory.getLogger(AuthenticationHeaderFilter.class);
    
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		
		String authName = null;
		try {
			WebSession ws = exchange.getSession().toFuture().get();
			SecurityContextImpl context = ws.getAttribute("SPRING_SECURITY_CONTEXT");
			logger.trace("Got Spring context={}", context!=null);
			if (context!=null) {
				authName = context.getAuthentication().getName();
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		exchange.getRequest().mutate()
			.header(UserAccount.AUTH_USER_HEADER, authName)
			.header("gatewayUrl", gatewayUrl);// Is this actually used?
		
		return chain.filter(exchange);
	}

}
