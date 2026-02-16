package com.langleydata.homepoker.controllers;

import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.PlayerRemovedListener;
import com.langleydata.homepoker.game.RoundCompleteListener;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.message.AllocateFundsMessage;
import com.langleydata.homepoker.message.ChatMessage;
import com.langleydata.homepoker.message.CompleteGameMessage;
import com.langleydata.homepoker.message.EvictPlayerMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.message.PlayerStatsMessage;
import com.langleydata.homepoker.message.PrivateJoinerMessage;
import com.langleydata.homepoker.message.SettingChangeRequest;
import com.langleydata.homepoker.message.SettingsUpdate;
import com.langleydata.homepoker.message.TransferFundsMessage;

/** All generic functions and messaging that a TableController has to perform
 * 
 * @author reynolds_mj
 *
 */
public interface GenericTableController extends RoundCompleteListener, PlayerRemovedListener {
	public static final String PATH = "/texas/{gameId}/";
	
	
	/** The dealer has requested for the next round within the current game to start - Must be called to initiate a new round.
	 * 
	 * @param gameId
	 * @param moveDealer
	 * @return
	 */
	@MessageMapping(PATH + "startNextRound")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public GameUpdateMessage startNextRound(final String gameId, @RequestParam boolean moveDealer);
	
	/** Get the settings for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	@GetMapping(path= PATH + "settings", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public GameSettings getGameSettings(final String gameId);
	
	/**
	 * Receive a message from a user and send back to all users
	 * 
	 * @param chatMessage The incoming message
	 * @return The same message
	 */
	@MessageMapping(PATH + "chat")
	public void sendToAllUsers(final String gameId, final ChatMessage chatMessage);
	
	/** Send a direct private message from one user to another
	 * 
	 * @param chatMessage
	 * @return
	 */
	@MessageMapping(PATH + "privatechat")
	public void sendDirectMessage(final ChatMessage chatMessage);
	
	/** Allocate funds to users. Only available to the Host
	 * 
	 * @param gameId
	 * @param afMessage
	 * @param sessionId
	 * @return A GameUpdateMessage to the table topic
	 */
	@MessageMapping(PATH + "allocate")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public GameUpdateMessage allocateFunds(final String gameId, final AllocateFundsMessage afMessage, final String sessionId);
	
	/** Add a player to a specific game
	 * 
	 * @param gameId
	 * @param newPlayer
	 * @return
	 * @throws Exception
	 */
	@MessageMapping(PATH + "addplayer")
	@SendToUser(value = MessageUtils.PRIVATE_QUEUE, broadcast = false)
	public PrivateJoinerMessage addPlayerToGame(final String gameId, final Player newPlayer) throws Exception;
	
	
	/** Complete the game and return all player statistics
	 * 
	 * @param gameId
	 * @return
	 */
	@MessageMapping(PATH + "completeGame")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public CompleteGameMessage completeGame(final String gameId, final String sessionId);
	
	/** Get statistics for a specific Player
	 * 
	 * @param gameId
	 * @param playerId
	 * @return
	 */
	@MessageMapping(PATH + "myStats")
	@SendToUser(value = MessageUtils.PRIVATE_QUEUE, broadcast = false)
	public PlayerStatsMessage getPlayerStats(final String gameId, final String sessionId, final String playerId);
	
	/**
	 * The dealer has requested the deck to be shuffled
	 * 
	 * @return GameUpdateMessage to the broadcast group
	 */
	@MessageMapping(PATH + "shuffle")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public GameUpdateMessage shuffle(final String gameId);
	
	/** Receive a request from a client to change a game setting. Only available to the host
	 * 
	 * @param gameId
	 * @param request
	 * @return A SettingsUpdate which may or may not be successful
	 */
	@MessageMapping(PATH + "changeSetting")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public SettingsUpdate changeSetting(final String gameId, final SettingChangeRequest request, final String sessionId);
	/**
	 * A player has performed an action
	 * 
	 * @param gameId The gameId
	 * @param playerAction The action the player requested
	 * @return A PlayerAction to the table broadcast group
	 */
	@MessageMapping(PATH + "player/action")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public GameUpdateMessage sentFromPlayer(final String gameId, final PlayerActionMessage playerAction);
	
	/**
	 * Forcibly fold and cash out a player in the game. This method only registers the action
	 * and notifies the user. The actual ejection is done on a scheduled task. Only available to the host
	 * 
	 * @param gameId The gameId
	 * @param ejectMsg The EjectPlayerMessage from the host
	 * @param sessionId 
	 */
	@MessageMapping(PATH + "player/eject")
	public void evictPlayer(final String gameId, final EvictPlayerMessage ejectMsg, final String sessionId);
	
	/** Transfer an amount from one player's stack to another's.
	 * Can only be performed by the game host
	 * 
	 * @param gameId The Game id
	 * @param transfer The TransferFundsMessage
	 * @return A game update if it was successful, otherwise null
	 */
	@MessageMapping(PATH + "player/stackTransfer")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public GameUpdateMessage transferStack(final String gameId, final TransferFundsMessage transfer, final String sessionId);
	
	@MessageExceptionHandler
	public void handleMessageException(Exception exec);
	
	/** Provide a list of all currently active games on the server, along with information about the server,
	 * as a REST endpoint
	 * 
	 * @return
	 */
	@GetMapping(path = "/info/active", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public GameServer getGamesRunning();
}
