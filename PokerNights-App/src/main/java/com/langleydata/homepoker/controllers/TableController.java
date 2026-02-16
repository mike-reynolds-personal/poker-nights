package com.langleydata.homepoker.controllers;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.ActiveGame;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.MoneyType;
import com.langleydata.homepoker.api.CardGame.PermisibleSettingChangeFields;
import com.langleydata.homepoker.api.CardGame.ShuffleOption;
import com.langleydata.homepoker.api.GameMessage;
import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.exception.GameStartException;
import com.langleydata.homepoker.exception.InvalidPlayerException;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.RoundHistory;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.SystemPlayer;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemGame;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings.RebuyType;
import com.langleydata.homepoker.message.AllocateFundsMessage;
import com.langleydata.homepoker.message.CashOutMessage;
import com.langleydata.homepoker.message.ChatMessage;
import com.langleydata.homepoker.message.CompleteGameMessage;
import com.langleydata.homepoker.message.EvictPlayerMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.JoinerMessage;
import com.langleydata.homepoker.message.LeaverMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.Messaging;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.message.PlayerStatsMessage;
import com.langleydata.homepoker.message.PrivateJoinerMessage;
import com.langleydata.homepoker.message.SettingChangeRequest;
import com.langleydata.homepoker.message.SettingsUpdate;
import com.langleydata.homepoker.message.StatusMessage;
import com.langleydata.homepoker.message.TransferFundsMessage;
import com.langleydata.homepoker.persistence.MessageHistoryProvider;
import com.langleydata.homepoker.persistence.RoundHistoryProvider;
import com.langleydata.homepoker.persistence.SettingsProvider;

/** The main Controller for all actions on a game table. This is intended to be 
 * generic for all implemented card games and therefore have no specific logic 
 * to any one type of game.
 * 
 * @author reynolds_mj
 *
 */
@Controller
public class TableController implements GenericTableController {
	public static final long EVICT_REJECT_PERIOD = 30 * 1000L;
	private final Logger logger = LoggerFactory.getLogger(TableController.class);
	
	Map<String, AbstractCardGame<?>> activeGames = new HashMap<>();
	final Map<String, ToEvict> evictStates = new ConcurrentHashMap<>();

	@Value("${game-server.name:Tewkesbury}")
	private String gameServerName;
	
	@Autowired
	private Messaging msgUtils;
	@Autowired
	private RoundHistoryProvider historyProvider;
	@Autowired
	private SettingsProvider settingProvider;
	@Autowired
	private MessageHistoryProvider actionProvider;
	@Autowired
	private AccountService accService;

	
	@Override
	public PrivateJoinerMessage addPlayerToGame(@DestinationVariable final String gameId, final Player newPlayer) throws Exception {
		final AbstractCardGame<?> theGame = getActiveGame(newPlayer.getSessionId(), gameId);
		if (theGame==null) {
			return null;
		}
		
		if (StringUtils.isBlank(newPlayer.getSessionId()) || StringUtils.isBlank(newPlayer.getSessionId())) {
			PrivateJoinerMessage fail = new PrivateJoinerMessage(newPlayer.getSessionId());
			fail.setSuccessful(false);
			fail.setMessage("No player or session id provided");
			fail.setPicture(SystemPlayer.PICTURE);
			return fail;
		}
		
		final PrivateJoinerMessage privateJoiner = new PrivateJoinerMessage(newPlayer.getSessionId());
		privateJoiner.setPlayer(newPlayer);
		
		// Add the player to the game and set-up private info
		try {
			theGame.addPlayer(newPlayer);
		} catch (InvalidPlayerException ipe) {
			logger.warn("{} adding to game {}", ipe.getMessage(), gameId);
			privateJoiner.setSuccessful(false).setMessage(ipe.getMessage());
			return privateJoiner;
		}

		final String playerHandle = newPlayer.getPlayerHandle();
		
		privateJoiner.setCurrentGame(theGame)
					.setSuccessful(true)
					.setMessage( getWelcome(newPlayer, theGame.getSettings())
							);
		
		if (theGame.getSettings().isHostControlledWallet() && theGame.getPlayers().getHost() != null) {
				msgUtils.sendPrivateMessage(
						theGame.getPlayers().getHost().getSessionId(),
						MessageUtils.PRIVATE_QUEUE,
						new ChatMessage(newPlayer.getSessionId(), newPlayer.getPlayerHandle() + " has joined and is waiting on their wallet being allocated."),
						250L);
		}
		logger.info("Added player {} ({}) to game {}", newPlayer.getPlayerId(), newPlayer.getPlayerHandle(), gameId);
		
		// Additional broadcast the fact to all users
		final JoinerMessage joiner = new JoinerMessage(newPlayer);
		joiner.setMessage(playerHandle + " has joined the game");
		joiner.setCurrentPlayers(theGame.getPlayers());
		msgUtils.sendBroadcastToTable(gameId, joiner, 0);

		return privateJoiner;
	}

	/** Get the formatted new player welcome message
	 * 
	 * @param handle
	 * @param wallet
	 * @param settings
	 * @return
	 */
	private String getWelcome(final Player newPlayer, final GameSettings settings) {
		final StringBuffer sb = new StringBuffer();
		
		if (settings.isHostControlledWallet()) {
			if (newPlayer.getState().isHost()) {
				sb.append("You can allocate your wallet at any time, but don't forget other players!");
			} else {
				sb.append("Please wait for ")
					.append(settings.getHostName())
					.append(" to allocate your wallet...");
			}
		} else {
			final int wallet = (int) (newPlayer.getCurrentStack().getWallet() + newPlayer.getCurrentStack().getStack());
			
			if (settings.getFormat()==GameFormat.TOURNAMENT) {
				sb.append( MessageUtils.formatMoney(wallet, settings) )
				.append(" of chips have been transfered to your stack. There are no re-buys.");
			} else {
				sb.append("You have added ")
					.append( MessageUtils.formatMoney(wallet, settings) )
					.append(" to your game wallet and the buy-in has been transfered to your stack!");
			}
		}
		return HtmlUtils.htmlEscape(sb.toString());
	}
	
	@Override
	public GameSettings getGameSettings(@PathVariable final String gameId) {
		if (StringUtils.isEmpty(gameId)) {
			return null;
		}
		final AbstractCardGame<?> theGame = getActiveGame(gameId);
		if (theGame==null) {
			return null;
		}
		return theGame.getSettings();
	}
	
	@Override
	public void sendDirectMessage(final ChatMessage chatMessage) {
		
		if (chatMessage.getMessage().equalsIgnoreCase("NUDGE")) {
			chatMessage.setReceiptSound("train_horn");
			chatMessage.setMessage("You've been nudged by " + chatMessage.getPlayerHandle() + "!");
		}
		msgUtils.sendPrivateMessage(chatMessage.getSessionId(), chatMessage);
	}
	
	@Override
	public void sendToAllUsers(@DestinationVariable String gameId, final ChatMessage chatMessage) {
		
		if (gameId.endsWith("#")) {
			gameId = gameId.substring(0, gameId.length()-1);
		}
		
		final AbstractCardGame<?> theGame = getActiveGame(gameId);
		if (theGame==null) {
			return;
		}
		
		String msg = chatMessage.getMessage(); 
		
		// Check if this is for a specific user
		if (msg !=null && msg.startsWith("@")) {
			
			final String toUser = msg.substring(1, msg.indexOf(" "));
			chatMessage.setMessage( msg.substring(msg.indexOf(" ")+1) );
			Optional<String> toUserId = theGame.getPlayers().stream()
				.filter(p->p.getPlayerHandle().equalsIgnoreCase(toUser))
				.map(Player::getSessionId)
				.findFirst();
			
			if (toUserId.isPresent()) {
				msgUtils.sendPrivateMessage(toUserId.get(), chatMessage);
			}
			return;
		}
		msgUtils.sendBroadcastToTable(gameId, chatMessage, 0);
	}

	@Override
	public GameUpdateMessage startNextRound(@DestinationVariable final String gameId, @RequestParam boolean moveDealer) {

		final AbstractCardGame<?> aGame = getActiveGame(gameId);
		if (aGame==null) {
			return new GameUpdateMessage("No game found with id: " + gameId);
		}
		
		logger.debug("Starting new game for '{}'. moveDealer={}", gameId, moveDealer);
		
		try {
			
			aGame.startNextRound(moveDealer);
			
			final GameUpdateMessage gum = new GameUpdateMessage(aGame, true);
			if (aGame.getSettings().getShuffleOption()==ShuffleOption.ALWAYS) {
				gum.setActionSound("shuffle");
			}
			
			// Store the fact the game was played and send start message
			if (aGame.getRound()==1) {
				aGame.getSettings().setGameStartedAt(System.currentTimeMillis());
				msgUtils.sendBroadcastToTable(gameId, new ChatMessage("We hope you're ready, as the game is starting!"));
			}
			
			saveDealMessage(aGame, true);
			return gum;
			
		} catch ( GameStartException gse) {
			logger.error(gse.getMessage());
			saveDealMessage(aGame, false);
			return new GameUpdateMessage(gse.getMessage());
		}
	}

	@Override
	public CompleteGameMessage completeGame(@DestinationVariable final String gameId, @Header("sessionId") final String sessionId) {
		
		final AbstractCardGame<?> aGame = getActiveGame(sessionId, gameId);
		if (aGame==null) {
			return null;
		}
		
		// Check that it is the host requesting the update
		if (validateHostAction(aGame, sessionId) == null) {
			return null;
		}
		
		if (aGame.getGameState()!= TexasGameState.COMPLETE) {
			msgUtils.sendPrivateMessage(sessionId, new ChatMessage("Please complete the current round before finishing the game"));
			return null;
		}

		final CompleteGameMessage cgm = new CompleteGameMessage(aGame.getPlayerStats());
		aGame.completeGame();
		
		return cgm;
	}
	
	@Override
	public PlayerStatsMessage getPlayerStats(@DestinationVariable final String gameId, @Header("sessionId") final String sessionId, @Header("playerId") final String playerId) {
		
		final AbstractCardGame<?> aGame = getActiveGame(sessionId, gameId);
		if (aGame==null) {
			return null;
		}
		
		return new PlayerStatsMessage(aGame.getPlayerStats());
	}

	@Override
	public GameUpdateMessage shuffle(@DestinationVariable final String gameId) {

		final AbstractCardGame<?> aGame = getActiveGame(gameId);
		if (aGame==null) {
			return new GameUpdateMessage("No game found with id: " + gameId);
		}
		
		if (aGame.shuffle()) {
			GameUpdateMessage gum = new GameUpdateMessage(aGame);
			gum.setActionSound("shuffle");
			return gum;
		}
		return new GameUpdateMessage("Card shuffle failed");
	}
	
	@Override
	public GameUpdateMessage transferStack(@DestinationVariable final String gameId, final TransferFundsMessage transfer, @Header("sessionId") final String sessionId) {
		
		final AbstractCardGame<?> aGame = getActiveGame(sessionId, gameId);
		if (aGame==null) {
			return null;
		}
		
		logger.debug("Host '{}' attempting action {}'; Round: {}, game: {}", 
				transfer.getPlayerHandle(),
				transfer.getAction(),
				aGame.getRound(),
				gameId);
		
		if (validateHostAction(aGame, sessionId) == null) {
			return null;
		}
		
		if (transfer.getMessageType() != MessageTypes.PLAYER_ACTION || transfer.getAction() != PlayerActionType.TRANSFER_FUNDS) {
			return sendGameErrorAction(transfer, "Incorrect actions provided");
		}

		
		final Player fromPlayer = aGame.getPlayers().getPlayerById(transfer.getFromId());
		final Player toPlayer = aGame.getPlayers().getPlayerById(transfer.getToId());
		final float toTransfer = transfer.getAmount();
		
		if (fromPlayer==null || toPlayer==null ) {
			return sendGameErrorAction(transfer, "No From or To playerId provided");
		}
		if (toPlayer.equals(fromPlayer)) {
			return sendGameErrorAction(transfer, "To and from players must be different");
		}
		if (fromPlayer.getCurrentStack().getStack() <= toTransfer) {
			return sendGameErrorAction(transfer, "'From' player has insufficient funds in their wallet");
		}
		
		fromPlayer.getCurrentStack().reduceStack(toTransfer);
		toPlayer.getCurrentStack().transferWinAmount(toTransfer);
		
		// Inform each player involved in the transfer
		final String mValue = MessageUtils.formatMoney(transfer.getAmount(), aGame.getSettings());
		transfer.setSuccessful(true);
		final String msg = String.format("The host transfered %s from %s to %s", mValue, fromPlayer.getPlayerHandle(), toPlayer.getPlayerHandle());
		transfer.setMessage(msg);
		final boolean hostInTransfer = transfer.getSessionId().equals(fromPlayer.getSessionId()) || transfer.getSessionId().equals(toPlayer.getSessionId());
		if (!hostInTransfer) {
			msgUtils.sendPrivateMessage(transfer.getSessionId(), transfer);
		}
		msgUtils.sendPrivateMessage(fromPlayer.getSessionId(), transfer);
		msgUtils.sendPrivateMessage(toPlayer.getSessionId(), transfer);
		
		// Update action with game state
		transfer.setGameState(aGame.getGameState())
				.setGameId(aGame.getSettings().getGameId())
				.setRound(aGame.getRound());
		actionProvider.addPlayerAction(transfer);
		
		return new GameUpdateMessage(aGame);
		
	}
	
	@Scheduled(fixedDelay = 30 * 1000)
	public void evictPlayersProcess() {
		
		final Set<String> removeState = new HashSet<>();
		final Map<String, GameUpdateMessage> gameUpdates = new HashMap<>();
		
		for (Entry<String, ToEvict> eviction : evictStates.entrySet()) {
			
			final ToEvict toEvict = eviction.getValue();
			final Player evictPlayer = toEvict.toEject;
			
			// Has the player had enough time to reject this?
			if ( (toEvict.ejectStart + EVICT_REJECT_PERIOD) > System.currentTimeMillis()) {
				continue;
			}
			
			final AbstractCardGame<?> aGame = getActiveGame(toEvict.gameId);
			if (aGame==null) {
				removeState.add(eviction.getKey());
				continue;
			}
			
			final LeaverMessage leaveMsg = new LeaverMessage(
					evictPlayer.getSessionId(), 
					evictPlayer.getPlayerHandle(), 
					evictPlayer.getSeatingPos()
					);
			
			// Fold the player first - Use original action but relabel as FOLD
			final PlayerAction doFold = new PlayerActionMessage(evictPlayer.getSessionId())
				.setAction(PlayerActionType.FOLD)
				.setPlayerHandle(evictPlayer.getPlayerHandle())
				.setPlayerId(evictPlayer.getPlayerId());
			
			GameUpdateMessage response = aGame.doGameUpdateAction(doFold);
			String errorMessage = null;
			
			// now cash the player out
			if (doFold.isSuccessful()) {
				final PlayerAction cashOut = new PlayerActionMessage(evictPlayer.getSessionId())
					.setAction(PlayerActionType.CASH_OUT)
					.setPlayerHandle(evictPlayer.getPlayerHandle());
				
				final GameMessage resp = doPlayerAction(cashOut, aGame);
				if (resp!=null && resp instanceof CashOutMessage) {
					response = new GameUpdateMessage(aGame);
					toEvict.evictMsg.setMessage("Player " + evictPlayer.getPlayerHandle() + " evicted from the game by the host");
					msgUtils.sendPrivateMessage(evictPlayer.getSessionId(), resp);
				} else {
					errorMessage = "Unable to cash the player out of the game";
				}
			} else {
				errorMessage = "Unable to fold the player";
			}
			
			removeState.add(eviction.getKey());
			final Player host = aGame.getPlayers().getHost();
			// If we couldn't process the ejection, continue 
			if (StringUtils.isNotBlank(errorMessage)) {
				msgUtils.sendPrivateMessage(host.getSessionId(), new EvictPlayerMessage(errorMessage));
				continue;
			}
				
			actionProvider.addPlayerAction(toEvict.evictMsg.toPlayerAction(
					host.getSessionId(),
					aGame.getRound(), 
					aGame.getGameState())
					);
			
			// Broadcast the leaver, so the table state is updated correctly
			// and then store the game update
			msgUtils.sendBroadcastToTable(toEvict.gameId, leaveMsg);
			gameUpdates.put(toEvict.gameId, response );
		}
		
		// Remove processed states and send unique game updates
		removeState.forEach(evictStates::remove);
		gameUpdates.forEach( (e,v) -> msgUtils.sendBroadcastToTable(e, v, 500) );
		
	}

	@Override
	public void evictPlayer(@DestinationVariable final String gameId, final EvictPlayerMessage evictMsg, @Header("sessionId") final String sessionId) {

		final AbstractCardGame<?> aGame = getActiveGame(sessionId, gameId);
		if (aGame == null) {
			return;
		}
		
		final String evictSessionId = evictMsg.getToEvictId();
		String errorMessage = null;

		final Player host = validateHostAction(aGame, sessionId, !evictMsg.isReject());
		if (host == null && evictMsg.isReject()==false) {
			return;
		}
		
		if (evictMsg.getMessageType() != MessageTypes.EVICT_PLAYER || StringUtils.isBlank(evictSessionId)) {
			errorMessage = "No playerId or action provided";
		}
		
		final Player toEvict = aGame.getPlayers().getPlayerBySessionId(evictSessionId);
		if (toEvict == null) {
			errorMessage = "No player found with the provided session id";
		}
		
		if (StringUtils.isNotBlank(errorMessage)) {
			msgUtils.sendPrivateMessage(sessionId, new EvictPlayerMessage(errorMessage));
			return;
		}
		
		if( evictMsg.isReject() ) {
			evictStates.remove(evictSessionId);
			if (host !=null) {
				evictMsg.setMessage(toEvict.getPlayerHandle() + " rejected your eviction request");
				msgUtils.sendPrivateMessage(aGame.getPlayers().getHost().getSessionId(), evictMsg);
			}
		} else {
			// inform the player their going to be evicted
			evictMsg.setMessage("The host is evicting you from the game. You have " + (EVICT_REJECT_PERIOD / 1000)  + " seconds to cancel the eviction.");
			msgUtils.sendPrivateMessage(evictSessionId, evictMsg);
			evictStates.put(evictSessionId, new ToEvict(gameId, evictMsg, toEvict));
		}
	}
	

	@Override
	public GameUpdateMessage allocateFunds(@DestinationVariable final String gameId, final AllocateFundsMessage afMessage, @Header("sessionId") final String sessionId) {

		final AbstractCardGame<?> aGame = getActiveGame(sessionId, gameId);
		if (aGame==null) {
			return null;
		}
		
		logger.debug("Player '{}' attempting action '{}'; Round: {}, game: {}", 
				sessionId,
				"Allocate funds",
				aGame.getRound(),
				gameId);
		
		// Check that it is the host requesting the update
		if (validateHostAction(aGame, sessionId)==null) {
			return new GameUpdateMessage("Only the Host (" + aGame.getSettings().getHostName() + ") can allocate funds to players");
		}
		
		// Is a virtual money game? (In Real-money, the player has to deposit more cash)
		final GameSettings settings = aGame.getSettings();
		if (settings.isHostControlledWallet() && settings.getMoneyType() != MoneyType.VIRTUAL ) {
			logger.debug("Attempting to allocate funds in non-virtual money game");
			return new GameUpdateMessage("Wallet allocation is only applicable to virtual money games");
		}
		
		final int buyInAmount = settings.getBuyInAmount();
		
		// For each player, allocate a wallet, which automatically updates their stack, and sit them in to the game
		afMessage.getAllocations().forEach(alloc -> {
			final Player p = aGame.getPlayers().getPlayerById(alloc.getPlayerId());
			
			if (p !=null) {
				if (p.getCurrentStack().assignWallet(alloc.getWallet(), buyInAmount)) {
					// Sit player in and inform them
					p.getState().toggleSittingOut(aGame.getGameState(), false);
					final String msg = String.format("%s has transferred %s to your wallet.", aGame.getSettings().getHostName(),
							MessageUtils.formatMoney(alloc.getWallet(), 
							settings));
					
					// Don't send message to host
					if (p.getSessionId().equals(sessionId)==false) {
						msgUtils.sendPrivateMessage(p.getSessionId(), new ChatMessage(sessionId, msg));
					}
				}
			}
		});
		
		return new GameUpdateMessage(aGame);
	}
	
	@Override
	public GameUpdateMessage sentFromPlayer( @DestinationVariable final String gameId, final PlayerActionMessage playerAction) {

		final AbstractCardGame<?> aGame = getActiveGame(gameId);
		if (aGame==null) {
			return sendGameErrorAction(playerAction, "No game found with id: " + gameId);
		}
		
		logger.debug("Player '{}' attempting action '{}'; Round: {}, game: {}", 
				playerAction.getPlayerHandle(),
				playerAction.getAction(),
				aGame.getRound(),
				gameId);
		
		if (playerAction.getMessageType() != MessageTypes.PLAYER_ACTION) {
			return sendGameErrorAction(playerAction, "Invalid message type");
		}

		GameUpdateMessage updateMessage = null;
		
		// Update action with game state, before the states
		// are changed by the action
		playerAction.setGameState(aGame.getGameState())
					.setGameId(aGame.getSettings().getGameId())
				    .setRound(aGame.getRound());
		
		
		// Generic toggles and non-turn actions
		Object toMsgPlayer = null;
		if (!playerAction.getAction().onTurnOnly() && !playerAction.getAction().isFold()) {
			toMsgPlayer = doPlayerAction(playerAction, aGame);
			if (playerAction.getAction().sendUpdate()) {
				updateMessage = new GameUpdateMessage(aGame);
			}
		} else {
			updateMessage = aGame.doGameUpdateAction(playerAction);
		}

		actionProvider.addPlayerAction(playerAction);
		
		// Inform the player about the outcome of the action
		msgUtils.sendPrivateMessage(playerAction.getSessionId(), toMsgPlayer==null ? playerAction : toMsgPlayer);
		
		if (playerAction.isSuccessful()) {
			msgUtils.sendBroadcastToTable(aGame.getSettings().getGameId(), new StatusMessage(null, playerAction));
		}
		return updateMessage; 
	}

	/** Perform an 'out-of-turn' action on a player
	 * 
	 * @param playerAction
	 * @param aGame
	 */
	private GameMessage doPlayerAction(final PlayerAction playerAction, final AbstractCardGame<?> aGame) {
		
		final Player p = aGame.getPlayers().getPlayerBySessionId(playerAction.getSessionId());
		if (p==null) {
			sendGameErrorAction(playerAction, "Unable to find player in game");
			return null;
		}

		/* To ensure a dis-connected player can get back in, check current sit-out state
		 * and only respond if not sitting out */
		if (playerAction.getAction()==PlayerActionType.SIT_OUT 
				&& aGame.getPlayers().size() < 3
				&& p.getState().isSittingOut()==false 
				&& p.getState().isSONR()==false ) {
			playerAction.setSuccessful(false);
			return new ChatMessage("You cannot use sit-out with less than 3 players");
		}
		
		if (aGame.getSettings().getFormat()==GameFormat.TOURNAMENT) {
			String error = null;
			switch (playerAction.getAction()) {
			case RE_BUY:
				error = "You cannot re-buy in a tournament game";
				break;
			case CASH_OUT:
				error = "You cannot cash-out in a tournament game";
				break;
			case REVEAL:
				error = "You cannot reveal your cards in a tournament game";
				break;
			default:
				break;
			}
			if (StringUtils.isNotBlank(error)) {
				sendGameErrorAction(playerAction, error);
				return null;
			}
		}
		
		float rebuy = aGame.getSettings().getBuyInAmount();
		
		if (RebuyType.LARGEST_STACK.name().equals(aGame.getSettings().getRebuyOption())) {
			rebuy = aGame.getPlayers().getMaxBetPossible(null);
		}
		
		// Allow the game to handle a fold as its more complicated
		if (!playerAction.getAction().isFold()) {
			
			p.doPlayerAction(playerAction, aGame.getGameState(), aGame.getSettings(), rebuy);
			
			// If cashing them out, also remove from the game
			if (playerAction.getAction()==PlayerActionType.CASH_OUT) {
				final Player left = aGame.removePlayer(p.getPlayerId());
				return new CashOutMessage(left, aGame.getSettings());
			}
		}
		return null;
	}

	@Override
	public void playerRemoved(final String gameId, final Player player) {
		if (player==null) {
			return;
		}

		// Inform the table
		final LeaverMessage leaver = new LeaverMessage(
				player.getSessionId(), 
				player.getPlayerHandle(), 
				player.getSeatingPos());
		leaver.setMessage(player.getPlayerHandle() + " has left the game.");
		msgUtils.sendBroadcastToTable(gameId, leaver, 0);
		
		// If the host details have been transfered, inform that player
		if (player.getState().isHost()) {
			final AbstractCardGame<?> game = getActiveGame(gameId);
			if (game != null) {
				final Player newDealer = game.getPlayers().getDealer();
				if (newDealer!=null && newDealer.getState().isHost()) {
					final StatusMessage statMsg = new StatusMessage(newDealer.getSessionId(), "Host controls have been transfered to you!");
					statMsg.setHostTransfer(true);
					msgUtils.sendPrivateMessage(newDealer.getSessionId(), statMsg);
				}
			}
		}

		// Update the player's game stats
		try {
			if ( !accService.updateAccountStats(player, player.getCurrentStack().getGameStats(), gameId) ) {
				logger.warn("Unable to save game stats for player {}", player.getPlayerId());
			}
		} catch (GameSchedulingException e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public SettingsUpdate changeSetting(@DestinationVariable final String gameId, final SettingChangeRequest request, @Header("sessionId") final String sessionId) {

		final AbstractCardGame<?> aGame = getActiveGame(sessionId, gameId);
		if (aGame==null) {
			return null;
		}
		
		// Check that it is the host requesting the update
		if (validateHostAction(aGame, sessionId)==null) {
			return null;
		}
		
		// Validate the change
		PermisibleSettingChangeFields toChange = null;
		try {
			toChange = PermisibleSettingChangeFields.valueOf(request.getSettingName());
		} catch (Exception e) {}
		if (toChange==null || request.getSettingValue()==null) {
			return sendSetSettingError(sessionId, "The provided setting '" + request.getSettingName() + "' cannot be changed in-game");
		}
		
		try {
			Field toSet = ReflectionUtils.findField(aGame.getSettings().getClass(), request.getSettingName());
			toSet.setAccessible(true);
			ReflectionUtils.setField(toSet, aGame.getSettings(), request.getSettingValue());
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return sendSetSettingError(sessionId, "Unable to set field " + request.getSettingName());
		}
		
		return new SettingsUpdate(aGame.getSettings());
	}
	
	private SettingsUpdate sendSetSettingError(final String sessionId, final String error) {
		msgUtils.sendPrivateMessage(sessionId, new SettingsUpdate(error));
		return null;
	}
	
	@Override
	public void roundComplete(RoundHistory roundDetail) {
		if (roundDetail.getGameState()==TexasGameState.COMPLETE) {
			return;
		}
		logger.debug("Round complete, storing history. Round: {}, game: {}", roundDetail.getRound(), roundDetail.getGameId());
		
		historyProvider.addGameRound(roundDetail);
		final String msg = String.format("Round %s complete (%s)", roundDetail.getRound(), roundDetail.getShuffleSeed());
		msgUtils.sendBroadcastToTable(roundDetail.getGameId(), new StatusMessage(msg), 100);
	}
	
	/** Store the dealer triggered Deal cards action
	 * 
	 * @param aGame
	 */
	private void saveDealMessage(final AbstractCardGame<?> aGame, final boolean success) {
		
		final Player dealer = aGame.getPlayers().getDealer();
		PlayerActionMessage pa = new PlayerActionMessage(dealer.getSessionId());
		
		pa.setGameState(aGame.getGameState())
			.setPlayerId(dealer.getPlayerId())
			.setPlayerHandle(dealer.getPlayerHandle())
			.setGameId(aGame.getSettings().getGameId())
			.setRound(aGame.getRound())
			
			.setAction(PlayerActionType.DEAL)
			.setSuccessful(success);
		
		actionProvider.addPlayerAction(pa);
	}
	
	/** Send a non-successful player action to the sender
	 * 
	 * @param action
	 * @param error
	 */
	private GameUpdateMessage sendGameErrorAction(final PlayerAction action, final String error) {
		PlayerAction toSend =  action;
		if (action instanceof TransferFundsMessage) {
			toSend = new TransferFundsMessage( (TransferFundsMessage) action);
		}
		toSend.setSuccessful(false).setMessage(error);
		msgUtils.sendPrivateMessage(action.getSessionId(), toSend);
		return null;
	}

	/** Get a map of all the active games in memory
	 * 
	 * @return
	 */
	public Map<String, AbstractCardGame<?>> getActiveGames() {
		return activeGames;
	}
	
	@Override
	public GameServer getGamesRunning() {

		/* TODO Should build GameServer with the list of game formats being available as well.
		* This needs several changes, but would allow a single server to provide Texas + cash + Tournament, Omaha etc */
		
		logger.trace("Received request for active game info");
		final URI server = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null).build().toUri();
		final GameServer gameServer = new GameServer(server.toString(), GameType.TEXAS_HOLDEM);
		gameServer.setServerName(gameServerName);
		
		List<ActiveGame> beingServed = new ArrayList<>();
		getActiveGames().values().forEach(ag -> {
			final GameSettings set = ag.getSettings();
			final ActiveGame toAdd = new ActiveGame(set.getGameId());
			toAdd.setGameFormat(set.getFormat().name());
			toAdd.setPlayers(ag.getPlayers().size());
			beingServed.add(toAdd);
		});

		gameServer.setActiveGames(beingServed);
		logger.debug("Returning game server info");
		return gameServer;
	}
	
	/** Add a new game to the list of active games
	 * 
	 * @param newGame
	 */
	void addActiveGame(AbstractCardGame<?> newGame) {
		AbstractCardGame<?> current = activeGames.putIfAbsent(newGame.getSettings().getGameId(), newGame);

		logger.info("Setting game {} to active!", newGame.getSettings().getGameId());
		
		if (current==null) {
			newGame.addRoundListener(this);
			newGame.addRemovePlayerListener(this);
		}
	}
	/** Validate that the provided sessionId is the host for the game.
	 * If the player is not the host, then a message is sent to the sessionId.
	 * 
	 * @param aGame The card game
	 * @param hostSessionId The sessionId to validate
	 * @return The Host player, or null if not the host
	 */
	Player validateHostAction(final AbstractCardGame<?> aGame, final String hostSessionId) {
		return validateHostAction(aGame, hostSessionId, true);
	}
	
	/** Validate that the provided sessionId is the host for the game.
	 * If the player is not the host, then a message is sent to the sessionId.
	 * 
	 * @param aGame The card game
	 * @param hostSessionId The sessionId to validate
	 * @param doMessage Send a message to the person?
	 * @return The Host player, or null if not the host
	 */
	Player validateHostAction(final AbstractCardGame<?> aGame, final String hostSessionId, final boolean doMessage) {
		final Player requestor = aGame.getPlayers().getPlayerBySessionId(hostSessionId);
		if ((requestor == null || requestor.getState().isHost()==false) && doMessage) {
			msgUtils.sendPrivateMessage(hostSessionId, new ChatMessage("Only the host can perform this action"));
			return null;
		}
		return requestor;
	}
	
	/** Retrieve an active game, if it is. If this is a test table, return that game.
	 * 
	 * @param gameId
	 * @return The game or Null if it doesn't exist
	 */
	AbstractCardGame<?> getActiveGame(final String gameId) {
		return activeGames.get(gameId);
	}
	
	/** Update the game settings, store them and set the game to 'Active'
	 * 
	 * @param storedSet The settings to use
	 * @param url The sserver baseUrl
	 * @return The active game or null
	 */
	AbstractCardGame<?> setGameActive(GameSettings storedSet, final String url) {
		if (storedSet.getGameType() !=GameType.TEXAS_HOLDEM) {
			return null;
		}
		
		AbstractCardGame<?> theGame = new TexasHoldemGame((TexasHoldemSettings) storedSet, msgUtils);
		storedSet.setWasPlayed();
		storedSet.setServerName(gameServerName);
		storedSet.setAssignedServer(url);
		settingProvider.storeSettings((TexasHoldemSettings)storedSet);
		addActiveGame(theGame);
		return theGame;

	}
	/** Get the active game, and if there isn't one, send a message to the sessionId
	 * 
	 * @param sessionId
	 * @param gameId
	 * @return
	 */
	AbstractCardGame<?> getActiveGame(final String sessionId, final String gameId) {
		AbstractCardGame<?> aGame = activeGames.get(gameId);
		if (aGame==null && StringUtils.isNotBlank(sessionId) ) {
			msgUtils.sendPrivateMessage(sessionId, new ChatMessage("No game found with id: " + gameId));
		}
		return aGame;
	}
	@Override
	public void handleMessageException(Exception exec) {
		logger.error("Message Exception: ", exec);
	}

	static class ToEvict {
		final EvictPlayerMessage evictMsg;
		final Player toEject;
		final String gameId;
		long ejectStart = System.currentTimeMillis();
		
		ToEvict(String gameId, EvictPlayerMessage msg, Player toEject) {
			this.gameId = gameId;
			this.evictMsg = msg;
			this.toEject = toEject;
		}
	}

}
