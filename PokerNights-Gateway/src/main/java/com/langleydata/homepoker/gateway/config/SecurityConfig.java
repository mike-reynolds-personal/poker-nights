package com.langleydata.homepoker.gateway.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import com.langleydata.homepoker.gateway.controller.UserAccountController;

import reactor.core.publisher.Mono;

/** Configure the Gateway security which secures specific endpoints, or more acurately
 * requires that they have authorization (aka a login) in order to access them.
 * This additionally adds a request for offline scope to the authorization requests 
 * 
 * @author Mike Reynolds
 *
 */
@Configuration
public class SecurityConfig {
	private final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	@Autowired
	private ReactiveClientRegistrationRepository clientRegRepo;
	
	@Value("${app.logout.redirect:/}")
	private String logoutRedirect;
	
	@Autowired
	private UserAccountController userController;
	
	static final String[] REQUIRES_AUTH = new String[] {
			"/schedule-game/**",
			"/texas/**",
			"/admin/**",
			"/account/**",
			"/settings/**"
	};

	/** Prevents the default /login?logout redirect
	 */
    private ServerLogoutSuccessHandler logoutSuccess() {
    	 RedirectServerLogoutSuccessHandler successHandler = new RedirectServerLogoutSuccessHandler();
    	 successHandler.setLogoutSuccessUrl(URI.create(logoutRedirect));
    	 return successHandler;
    }
    
    /** Extend the default OAuth request resolver (from Spring Boot) to add additional parameters
     * 
     */
    private class CustomAuthorizationRequestResolver extends DefaultServerOAuth2AuthorizationRequestResolver {
		public CustomAuthorizationRequestResolver() {
			super(clientRegRepo);
			
			setAuthorizationRequestCustomizer((builder) -> {
				Map<String, Object> props = new HashMap<>();
				props.put("access_type", "offline");
				builder.additionalParameters(props);
			});
		}
    }
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    	http
	   		.csrf().disable()// Enables $.post() available in js
	   		.authorizeExchange()
	   			.pathMatchers(null, REQUIRES_AUTH).authenticated()
	   			.anyExchange().permitAll();
	   
   	  // Configure custom login
      http.formLogin(login -> login.loginPage("/signin"))
	      .logout(logout ->  
	      	logout.requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/signout"))
	      	.logoutSuccessHandler(logoutSuccess()));
	      

      // enable OAuth2/OIDC
      http.oauth2Client()
          .and().oauth2Login()
          	.authenticationSuccessHandler(userController)
          	.authorizationRequestResolver(new CustomAuthorizationRequestResolver())
	   		.authenticationFailureHandler((we, ex)-> {
	   			logger.error(ex.getMessage());
	   			ServerHttpResponse response = we.getExchange().getResponse();
	   			DataBuffer buf = we.getExchange().getResponse().bufferFactory().wrap(ex.getMessage().getBytes(StandardCharsets.UTF_8));
	   			return response.writeWith(Mono.just(buf));
	   		});

	   return http.build();
    }

}