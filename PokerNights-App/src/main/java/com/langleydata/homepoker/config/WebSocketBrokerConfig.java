package com.langleydata.homepoker.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {
	final Logger logger = LoggerFactory.getLogger(WebSocketBrokerConfig.class);

//	@Autowired
//	private JwtDecoder jwtDecoder;

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(preSendAuthInterceptor);
	}

	/** Channel interceptor to handle checking authorization header and JWT decode
	 * 
	 */
	ChannelInterceptor preSendAuthInterceptor = new ChannelInterceptor() {
		
		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
					StompHeaderAccessor.class);

			if (StompCommand.CONNECT.equals(accessor.getCommand())) {
				final List<String> authorization = accessor.getNativeHeader("X-Authorization");

				if (authorization == null || authorization.size() == 0) {
					return notAuthorized("No authorization header provided");
				}

				logger.debug("Received ({}) authorization headers", authorization.size());
				final String parts[] = authorization.get(0).split(" ");

				if (parts.length < 2) {
					return notAuthorized("No authorization header provided");
				}

				// Now attempt the decode/ validation
//				try {
					// TODO Re-implement authentication of websocket
//					final Jwt jwt = jwtDecoder.decode("Bearer " + parts[1]);
//					accessor.setUser(new JwtAuthenticationConverter().convert(jwt));
					return message;
//				} catch (JwtException je) {
//					logger.error(je.getMessage());
//					return notAuthorized(je.getMessage());
//				}
			}
			return message;
		}
	};

	/** Log the error and return null. Would like to return an actual message, but not working!
	 * 
	 * @param error
	 * @return
	 */
	private Message<?> notAuthorized(final String error) {
		logger.warn(error);
		return null;
	}
}
