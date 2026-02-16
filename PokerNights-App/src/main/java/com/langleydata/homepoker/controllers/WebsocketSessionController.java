package com.langleydata.homepoker.controllers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.google.common.annotations.VisibleForTesting;
import com.langleydata.homepoker.exception.ReconnectException;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.message.DealUserMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.Messaging;
import com.langleydata.homepoker.message.PrivateJoinerMessage;
import com.langleydata.homepoker.message.SubscribeMessage;

import io.micrometer.core.instrument.util.StringUtils;

/**  This class handles connection, subscription and disconnection events based on 
 * what the client does.<p> 
 * This class is set-up with multiple Components and auto-wiring as we cannot implement 
 * more than one ApplicationListener at a time
 */
@Controller
public class WebsocketSessionController {
	final Logger logger = LoggerFactory.getLogger(WebsocketSessionController.class);
	/** The longest disconnected period before an action is performed on a user */
	final static long NO_ACTION_DELAY = 60 * 1000;
	/** The longest disconnected period before the user is automatically sat-out */
	final static long SIT_OUT_DELAY = 2 * 60 * 1000;
	/** The longest disconnected period before the player is removed from the game */
	final static long REMOVE_DELAY = 30 * 60 * 1000;
	final static int INACTIVITY_DELAY = 5 * 60;
	final Map<String, Disconnected> disConnStates = new ConcurrentHashMap<>();
	
	@Autowired
	private TableController tableController;
	@Autowired
	private Messaging msgUtils;

	@Value("${game-server.name:Tewkesbury}")
	private String gameServerName;
	
    /**
     * Create an ApplicationListener that listens for successful logins and simply just logs the principal name.
     * @return a new listener
     */
    @Bean
    protected ApplicationListener<AuthenticationSuccessEvent> authenticationSuccessEventApplicationListener() {
        return event -> logger.debug("Authentication Success with principal: {}", event.getAuthentication().getPrincipal());
    }
    
	/**
	 * Handle session disconnection, whether temporary outages or by user request
	 * 
	 */
    @Bean
    ApplicationListener<SessionDisconnectEvent> disconnectEvent() {
    	return event -> {
			final SimpMessageHeaderAccessor sma = SimpMessageHeaderAccessor.wrap(event.getMessage());
			if (event.getCloseStatus() != CloseStatus.NORMAL) {
				logger.debug("Disconnecting Session: reason {}", event.getCloseStatus().toString());
			}
			// Is 1000 an actual disconnect?
			final String sessionId = sma.getSessionId();
//			2021-01-15 21:13:25,082 WARN org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver [http-nio-0.0.0.0-8080-exec-1] Resolved [org.springframework.http.converter.HttpMessageNotWritableException: No converter for [class java.util.LinkedHashMap] with preset Content-Type 'application/javascript;charset=UTF-8']

			try {
				updatePlayerConnectState(sessionId);
			} catch (ReconnectException re) {
				logger.error(re.getMessage());
			}
    	};
    }
    
    /** Handle subscription events to all queues/ topics
     * 
     * @return
     */
    @Bean
    ApplicationListener<SessionSubscribeEvent> subscribeEvent() {
    	return event -> {
    		
    		final SimpMessageHeaderAccessor sma = SimpMessageHeaderAccessor.wrap(event.getMessage());
			final AbstractAuthenticationToken token = (AbstractAuthenticationToken) event.getUser();
			onSubscribe(sma, sma.getNativeHeader("playerId").get(0));
			// TODO Re-implement authentication of websocket
//			if (token instanceof JwtAuthenticationToken) {
//				final JwtAuthenticationToken jwt = (JwtAuthenticationToken)token;
//				final String playerId = (String) jwt.getTokenAttributes().get("uid");
//				onSubscribe(sma, playerId);
//			} else {
//				logger.warn("SubscribeEvent: No JWT token available. User not connected to table");
//			}
			
    	};
    }

	/**
	 * @param sma
	 * @param playerId
	 */
	void onSubscribe(final SimpMessageHeaderAccessor sma, final String playerId) {
		// Get subscription headers
		final String sessionId = sma.getSessionId();//websocket session id for sending messages
		final String whichTopic = sma.getNativeHeader("to").get(0);
		final String gameId = sma.getNativeHeader("gameId").get(0);
		final String playerHandle = sma.getNativeHeader("playerHandle").get(0);

		final AbstractCardGame<?> theGame = tableController.getActiveGame(gameId);
		if (theGame==null) {
			logger.warn("Player {} trying to access game {} which does not exist", playerId, gameId);
			sendPlayerErrorMsg(sessionId, "No game is still active with the provided ID");
			return;
		}

		boolean isSeated = false;
		
		switch (whichTopic.toLowerCase()) {
		case "private": // happens first - send initial info

			// Private queue should be first. The table subscription is
			// triggered by the receipt of this message on the client-side
			msgUtils.sendPrivateMessage(sessionId, new SubscribeMessage(sessionId, playerId, playerHandle));
			break;
			
		case "table": // 2nd - Send an update of the table info
			
			isSeated = sendInitialGameInfo(sessionId,  playerId, playerHandle, theGame);
			
			if (isSeated) {
				// If the player was seated, this was probably a reconnect, so update the state
				try {
					updatePlayerConnectState(sessionId, playerId, theGame, false);
				} catch (ReconnectException re) {
					logger.error(re.getMessage());
					sendPlayerErrorMsg(sessionId, re.getMessage());
					return;
				}
				// Broadcast a game update to refresh other player's information about this player
				msgUtils.sendBroadcastToTable(gameId, new GameUpdateMessage(theGame));
			}
			break;
			
		}

		logger.info("Session='{}', userId='{}', handle='{}' subscribed to {} (seated={}). Game={}",
				sessionId, 
				playerId,
				playerHandle,
				whichTopic, 
				isSeated, 
				gameId);
	}

	/** Send a failure message to the user
	 * 
	 * @param sessionId
	 * @param message
	 */
	private void sendPlayerErrorMsg(final String sessionId, final String message) {
		final PrivateJoinerMessage privateJoiner = new PrivateJoinerMessage(sessionId);
		privateJoiner.setSuccessful(false);
		privateJoiner.setMessage(message);
		msgUtils.sendPrivateMessage(sessionId, privateJoiner);
	}
	
    /** Refresh all of the re-joining player's table information and card
     * 
     * @param userGame The game the user is trying to access
     * @param playerId The player's persistent id
     * @return True if the player is already seated in the game
     */
    boolean sendInitialGameInfo(final String sessionId, final String playerId, final String handle, final AbstractCardGame<?> userGame) {
		final Player player = userGame.getPlayers().getPlayerById(playerId);
		
		if (player==null) {
			// Send the current game state as not in game
			final GameUpdateMessage gum = new GameUpdateMessage(userGame);
			gum.setMessage(
					String.format("Welcome to %s's game on the %s server!\n Please 'Take a Seat' to join the game",
						userGame.getSettings().getHostName(),
						gameServerName)
					);
			msgUtils.sendPrivateMessage(sessionId, gum);
			logger.debug("Player not found seated in game: {}", userGame.getSettings().getGameId());
			return false;
		}
		
		// If the player is in the game, then they are rejoining to their seat
		final PrivateJoinerMessage privateJoiner = new PrivateJoinerMessage(sessionId);
		privateJoiner.setCurrentGame(userGame);
		privateJoiner.setPlayer(player);
		privateJoiner.setMessage(player.getPlayerHandle() + ", you're back in the game!");
		privateJoiner.setSuccessful(true);
		privateJoiner.setReconnect(true);
		
		msgUtils.sendPrivateMessage(sessionId, privateJoiner);
		
		// If the player cards have been dealt, re-send them
		if (player.getCards().size() > 0) {
			msgUtils.sendPrivateMessage(sessionId, MessageUtils.PRIVATE_QUEUE,  new DealUserMessage(sessionId, player.getCards()), 100);
		}
		
		return true;
    }
	
    /** Disconnect a player based on their session id
     * 
     * @param sessionId
     * @return True if successful
     * @throws ReconnectException 
     */
    void updatePlayerConnectState(final String sessionId) throws ReconnectException {
    	updatePlayerConnectState(sessionId, null, null, true);
    }
    
	/** Update the connected state of a user based on their sessionId.<p>
	 * If the user is set as 'disconnected' they will be monitored for pause or removal from the active
	 * game. If they re-connect then this function should be called with disconnect = false so monitoring 
	 * can stop.
	 * 
	 * @param sessionId The transient session id (used during disconnect). Will be new during re-connect
	 * @param persistId The user's persistient Id
	 * @param userGame The game the user is trying to join/ leave
	 * @param disconnect If true, the player will be paused and eventually removed from the game after specified time-periods.
	 * @return True if the player/ game is updated. Otherwise false.
	 * @throws ReconnectException 
	 */
	void updatePlayerConnectState(final String sessionId, final String persistId, final AbstractCardGame<?> userGame, final boolean disconnect) throws ReconnectException {
		
		// Find the correct game based on the sessionId
		Player exist = null;
		String gameId = null;
		
		if (disconnect) {
			/* when disconnecting, we only have the session id
			 *  so go through all of the game's to find the user.
			 *  The player will have a different sessionId for each game!
			 */
			for (AbstractCardGame<?> game : tableController.getActiveGames().values()) {
				exist = game.getPlayers().stream()
						.filter(p -> p.getSessionId().equals(sessionId))
						.findFirst()
						.orElse(null);
				if (exist!=null) {
					gameId = game.getSettings().getGameId();
					break;
				}
			}
		} else if (userGame!=null) {
			// During a re-connect, the sessionId is different but we have the user's persistent id. 
			// Exclude anyone cashed out
			exist = userGame.getPlayers().getPlayerById(persistId);
			if (exist!=null && exist.getState().isCashedOut()) {
				throw new ReconnectException("You have been cashed out");
			}
			gameId = userGame.getSettings().getGameId();
		}

		// We didn't find a game that this user is in. Could be they're just watching
		if ( exist == null || StringUtils.isBlank(gameId) ) {
			return;
		}
		
		final String trackId = exist.getPlayerId() + "|" + gameId;
		
		if (disconnect && disConnStates.get(trackId)==null) {
			// Disconnecting and not already tracked
			disConnStates.put(trackId, new Disconnected(exist, gameId));
			logger.info("Session '{}' disconnected. Player {} ({}) will be paused in 30 secs in game {}",
					sessionId,
					exist.getPlayerId(),
					exist.getPlayerHandle(),
					gameId);
			
		} else if (!disconnect) {
			// must be a new player being sitting at the table, or re-connecting to table
			logger.info("Player {} ({}) connected to game {}", exist.getPlayerId(), exist.getPlayerHandle(), gameId);
			disConnStates.remove(trackId);
			exist.setSessionId(sessionId);// update to the new session id
		} else {
			// Player disconnecting, but already being tracked
		}
		
	}
	
	/** Check all currently disconnected users and either pause or remove them 
	 * from the game, based on the amount of time they've been disconnected.
	 */
	@Scheduled(fixedDelay = 10000)
	@VisibleForTesting
	void updatePlayerState() {
		final long now = System.currentTimeMillis();
		
		logger.trace("Checking on {} disconnected players", disConnStates.size());
		
		for (Disconnected cs : disConnStates.values()) {
			final long disconTime = now-cs.disConStart;
			final String pId = cs.player.getPlayerId();
			final AbstractCardGame<?> game = tableController.getActiveGame(cs.gameId);
			
			if (game == null) {
				// The game could have been purged, so remove state
				disConnStates.remove(pId + "|" + cs.gameId);
				continue;
			}
			/*
			 * If player can re-connect within:
			 *  - 60 seconds, no change
			 *  - 2 mins, sit them out and move action on
			 *  - 30 mins, disconnect and remove
			 */
			if (disconTime < NO_ACTION_DELAY) {
				continue;
			} else if (disconTime >= SIT_OUT_DELAY && disconTime < REMOVE_DELAY && !cs.actioned) {
				logger.warn("Player {} being auto sat-out from game '{}' as > {} secs", cs.player.getPlayerHandle(), cs.gameId, (SIT_OUT_DELAY/1000));
				game.pausePlayer(pId, cs.player.getSessionId());
				cs.actioned = true;
			} else if (disconTime >= REMOVE_DELAY) {
				logger.warn("Player {} being removed from game '{}' as > {} secs", cs.player.getPlayerHandle(), cs.gameId, (REMOVE_DELAY/1000));
				if (game.removePlayer(pId) == null) {
					logger.warn("Player {} was not in game {} when trying to remove them!", pId, cs.gameId);
				}
				msgUtils.sendBroadcastToTable(game.getSettings().getGameId(), new GameUpdateMessage(game));
				disConnStates.remove(pId + "|" + game.getSettings().getGameId());
			}
		}

	}
	
	/** A simple class to hold disconnected users */
	class Disconnected {
		final long disConStart = System.currentTimeMillis();
		final Player player;
		final String gameId;
		boolean actioned = false;
		
		Disconnected(final Player player, final String gameId) {
			this.player = player;
			this.gameId = gameId;
		}
	}
}
