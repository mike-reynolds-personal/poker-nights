package com.langleydata.homepoker.message;

import java.text.NumberFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.langleydata.homepoker.api.GameSettings;

import io.micrometer.core.instrument.util.StringUtils;

@Component
public class MessageUtils implements Messaging {
	public static final String TABLE_TOPIC = "/topic/texas/";
	public static final String PRIVATE_QUEUE = "/queue/private";
	
	private SimpMessagingTemplate messagingTemplate;


	@Autowired
	public void LRTStatusListener(SimpMessagingTemplate messagingTemplate) {
	    this.messagingTemplate = messagingTemplate;
	}
	
	/** As an alternative to @SendToUser or to be used inside methods that use @SendTo
	 * sends to default queue of '/queue/private'
	 * @param sessionId The user session id
	 * @param payload The message payload
	 */
	@Override
	public void sendPrivateMessage(final String sessionId, final Object payload) {
		sendPrivateMessage(sessionId, PRIVATE_QUEUE, payload, 0);
	}

	/** As an alternative to @SendToUser or to be used inside methods that use @SendTo
	 * 
	 * @param sessionId The user session id
	 * @param queue The private queue to send to 
	 * @param payload The message payload
	 * @param delay Delay before sending message, in milliseconds
	 */
	@Override
	public void sendPrivateMessage(final String sessionId, final String queue, final Object payload, final long delay) {
		SimpMessageHeaderAccessor ha = SimpMessageHeaderAccessor
				.create(SimpMessageType.MESSAGE);
				ha.setSessionId(sessionId);
				ha.setLeaveMutable(true);
				
		// Delay the sending so method returns happen first, user gets message last
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {}
				messagingTemplate.convertAndSendToUser(sessionId, queue, payload, ha.getMessageHeaders());
			}}).start();
	}
	
	/** Send a message to a specific game table
	 * 
	 * @param gameId The game's id
	 * @param message The message payload
	 */
	@Override
	public void sendBroadcastToTable(final String gameId, final Object message) {
		sendBroadcastToTable(gameId, message, 0);
	}
	
	/** Send a message to a specific game table with a delay
	 * 
	 * @param gameId The game's id
	 * @param message The message payload
	 * @param delay the delay in milliseconds
	 */
	@Override
	public void sendBroadcastToTable(final String gameId, final Object message, final long delay) {
		if (StringUtils.isBlank(gameId)) {
			return;
		}
		new Thread(()->{
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {}
			messagingTemplate.convertAndSend(TABLE_TOPIC + gameId, message);	
		}).start();
	}
	
	/** Format a value as money for the locale provided by the game settings
	 * 
	 * @param value The value to format
	 * @param settings The current game settings
	 * @return The value formatted as a monetary value
	 */
	public static String formatMoney(final double value, final GameSettings settings) {
		return formatMoney((float)value, settings);
	}
	
	/** Format a value as money for the locale provided by the game settings
	 * 
	 * @param value The value to format
	 * @param settings The current game settings
	 * @return The value formatted as a monetary value
	 */
	public static String formatMoney(final float value, final GameSettings settings) {
		return NumberFormat.getCurrencyInstance(settings.getLocale()).format(value);
	}
}
