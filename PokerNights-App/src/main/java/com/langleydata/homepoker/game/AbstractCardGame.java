package com.langleydata.homepoker.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.ShuffleOption;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.deck.Deck;
import com.langleydata.homepoker.exception.GameStartException;
import com.langleydata.homepoker.exception.InvalidPlayerException;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.PlayerStack;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.message.GameUpdateMessage;

import io.micrometer.core.instrument.util.StringUtils;


/** The basis of all implemented card games.
 * Each type of game should extend this class to ensure basic functionality
 * 
 * @author reynolds_mj
 *
 */
public abstract class AbstractCardGame<T extends GameSettings> {
	static final int WAIT_REBUY_MILLI = 30 * 1000;
	private final Logger logger = LoggerFactory.getLogger(AbstractCardGame.class);
	
	private final List<RoundCompleteListener> roundCallbacks = new ArrayList<>();
	private final List<PlayerRemovedListener> removedCallbacks = new ArrayList<>();
	private final Set<Player> removedPlayers = new HashSet<>();
	
	/** This games current state. Really needs to be a more generic state if we're looking to make this properly abstract */
	protected TexasGameState gameState = TexasGameState.COMPLETE;
	
	/** This game's settings, including the game type */
	protected final T settings;

	protected Deck deck = new Deck();

	/** All the players currently in the game */
	protected Players players = new Players();

	/** All the cards currently visible on the table (face-up) */
	protected List<Card> cardsOnTable = new ArrayList<>();

	/** The total amount that can be won */
	protected float currentPot = 0;
	
	/** The total bet made by a player */
	private float requiredBet = 0, minRaise = 0, lastRaise = 0;
	
	/** The current round of play */
	private int roundNum = 0;
	
	/** The money pots for this game */
	protected GamePots gamePot = new GamePots();
	
	/** The timestamp the game was first created (via access of lobby) */
	private final long createdTime = System.currentTimeMillis();
	
	private long startPlayingTime = 0;
	
	/** The time that there was last some activity in the game.
	 * This is used for purging in-active games after a specified period.
	 * Implementations should also set this on user-controlled actions
	 */
	protected long lastActivityTime = System.currentTimeMillis();
	/** The time the last round completed */
	protected long lastRoundCompletedTime = System.currentTimeMillis();
	
	private int tournamentPrizeFund = 0;
	
	/** Construct a new card game
	 * 
	 * @param settings
	 */
	public AbstractCardGame(final T settings) {
		this.settings = settings;
	}

	/**
	 * Starts the next game round (clear the table and shuffle the cards, if over shuffle
	 * threshold).
	 * <p>
	 * This only has effect if the gameState == COMPLETE.</p>
	 * If successful the gameState is always set to PRE_DEAL
	 * @param moveDealer Should the dealer be moved to the next player?
	 * @return The new dealer
	 * 
	 */
	public Player startNextRound(boolean moveDealer) throws GameStartException {
		
		if (gameState !=TexasGameState.COMPLETE) {
			throw new GameStartException("Invalid game state (round not complete): " + gameState);
		}
		
		cardsOnTable.clear();
		final Player actionOn = players.getActionOn();
		players.forEach(p -> p.resetForNewRound(settings.getBuyInAmount()));

		if (!gotPlayerNumbers()) {
			actionOn.getState().setActionOn(true);// just in case!
			throw new GameStartException("Not enough players to start game");
		}

		// Wait x seconds to allow re-buys
		if ((System.currentTimeMillis() - lastRoundCompletedTime) < WAIT_REBUY_MILLI) {
			
			final long zeroStacks = players.stream()
				.filter(p -> PokerMathUtils.floatEquals(p.getCurrentStack().getStack(), 0f))
				.filter(p -> PokerMathUtils.rd(p.getCurrentStack().getWallet()) >= settings.getBuyInAmount() )
				.count();
			if (zeroStacks > 0) {
				logger.debug("Waiting {} seconds for rebuy. {} stacks ==0", WAIT_REBUY_MILLI, zeroStacks);
				/* resetForNewGame changes the action state. If we throw an exception here, then the new
				 * game doesn't start, therefore we have to re-apply the actionOn state to the same player */
				actionOn.getState().setActionOn(true);
				throw new GameStartException("Waiting " + (WAIT_REBUY_MILLI / 1000) + " seconds for re-buys");
			}
		}
		
		if (startPlayingTime == 0) {
			startPlayingTime = System.currentTimeMillis();	
		}

		if (getSettings().getShuffleOption()==ShuffleOption.ALWAYS || roundNum == 0) {
			deck.shuffle();
		}
		
		if (moveDealer) {
			if (moveDealerPosition() == null) {
				throw new GameStartException("No dealer located - Are there players in the game?!");
			}
		}

		roundNum++;
		currentPot = 0;
		gameState = TexasGameState.PRE_DEAL;
		lastActivityTime = System.currentTimeMillis();
		
		return players.getDealer();
	}

	/** Get the count/ id of the round within this game
	 * 
	 * @return
	 */
	@JsonProperty
	public int getRound() {
		return roundNum;
	}
	
	/**
	 * @return the tournamentPrizeFund
	 */
	public int getTournamentPrizeFund() {
		return tournamentPrizeFund;
	}
	
	/**
	 * @param addValue the value to add to the tournamentPrizeFund
	 */
	public void addToTournamentPrizeFund(int addValue) {
		if (addValue > 0) {
			this.tournamentPrizeFund += addValue;
		}
	}
	
	/** Get the time the host started the first game
	 * 
	 * @return
	 */
	@JsonIgnore
	public long getStartedPlayingTime() {
		return this.startPlayingTime;
	}
	/** Get the time of the last activity against this game
	 * 
	 * @return
	 */
	@JsonIgnore
	public long getLastActivityTime() {
		return this.lastActivityTime;
	}
	/** Get the time the game was first accessed (created from settings access)
	 * 
	 * @return
	 */
	@JsonIgnore
	public long getCreatedTime() {
		return createdTime;
	}
	
	/** Get the players that have been removed from the game, for whatever reason
	 * 
	 * @return
	 */
	@JsonIgnore
	public Set<Player> getRemovedPlayers() {
		return this.removedPlayers;
	}
	/**
	 * Move the dealer position forward by one, or to the first position 
	 * if no dealer is currently set. Also sets actionOn = true for the dealer
	 * so the UI can enable his controls
	 * 
	 * @return The new dealer
	 */
	public Player moveDealerPosition() {
		lastActivityTime = System.currentTimeMillis();
		logger.debug("Attempting to move dealer");
		
		final Player curDealer = players.getDealer();
		if (curDealer==null) {
			return null;
		}
		
		Player newDealer = curDealer;

		if (curDealer.getState().isDealer()==false) {
			curDealer.getState().setDealer(true);
		} else {
			newDealer = players.getPlayerRelativeTo(curDealer, 1, false);
			
			// In-case the next player is sitting out, or going to sit out
			int next = 2;
			while (newDealer.getState().isSittingOut() || newDealer.getState().isSONR()) {
				newDealer = players.getPlayerRelativeTo(curDealer, next, false);
				next++;
				if (next > players.size()) {
					break;
				}
			}
			curDealer.getState().setDealer(false);
			newDealer.getState().setDealer(true);
		}

		// Set the action on the dealer so it can be moved forward
		// from their position easily and the UI can enable their controls
		newDealer.getState().setActionOn(true);
		
		return newDealer;
	}

	@JsonIgnore
	protected abstract String getActionSound(final TexasGameState prevState, final PlayerAction action);

	/** Perform an action by a player on the active game.
	 * 
	 * @param playerAction This must include the <b>sessionId</b> for the player performing the action
	 * @return Must always return an update message, with a relevant message
	 */
	public abstract GameUpdateMessage doGameUpdateAction(PlayerAction playerAction);
	
	/**
	 * @return the currentPot
	 */
	@JsonProperty
	public float getCurrentPot() {
		return PokerMathUtils.rd(currentPot);
	}
	
	/**
	 * @return the lastBet
	 */
	@JsonProperty
	public float getRequiredBet() {
		return PokerMathUtils.rd(requiredBet);
	}

	/** Reset the minimum, last and required bets
	 * 
	 * @param minRaise
	 */
	public void resetBets(float minRaise) {
		requiredBet = 0;
		lastRaise = 0;
		this.minRaise = minRaise;
	}
	/** Get the last raise
	 * 
	 * @return
	 */
	@JsonProperty
	public float getLastRaise() {
		return PokerMathUtils.rd(lastRaise);
	}
	
	/** Set the last raise value
	 * 
	 * @param lastRaise
	 */
	@JsonIgnore
	public void setLastRaise(float lastRaise) {
		this.lastRaise = lastRaise;
	}

	/**
	 * @return the finalPots
	 */
	public GamePots getFinalPots() {
		return gamePot;
	}
	
	/** Get stats for all users
	 * 
	 * @return
	 */
	@JsonIgnore
	public Map<String, GameStats> getPlayerStats() {
		logger.debug("Getting player stats");
		lastActivityTime = System.currentTimeMillis();
		return Stream.of(players, removedPlayers)
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(Player::getPlayerHandle, (p)-> p.getCurrentStack().getGameStats() ));
	}
	
	/** The required bet value to stay in the game
	 * 
	 * @param requiredBet the requiredBet to set
	 */
	@JsonIgnore
	public void setRequiredBet(float requiredBet) {
		lastActivityTime = System.currentTimeMillis();
		this.requiredBet = requiredBet;
	}
	
	/** Get the minimum raise value, which is above the required value to continue
	 * in the game. Used by the UI
	 * 
	 * @return
	 */
	@JsonProperty
	public float getMinRaise() {
		return PokerMathUtils.rd(minRaise);
	}
	
	/** Set the minimum permissible raise; used for the UI */
	@JsonIgnore
	public void setMinRaise(float minRaise) {
		this.minRaise = minRaise;
	}
	
	/**
	 * Add a new player to the game. Can only be done pre-deal
	 * 
	 * @param newPlayer The player to add
	 * @throws InvalidPlayerException 
	 */
	public void addPlayer(final Player newPlayer) throws InvalidPlayerException {
		lastActivityTime = System.currentTimeMillis();
		
		// Validate if detail blank or player handle exists
		if (	StringUtils.isBlank(newPlayer.getPlayerId()) || 
				StringUtils.isBlank(newPlayer.getPlayerHandle())
				|| newPlayer.getPlayerHandle().contains(" ")) {
			throw new InvalidPlayerException("No player ID, handle or handle contains a space");
		}
		logger.trace("Attempting to add player {} to game {}", newPlayer.getPlayerId(), settings.getGameId());
		
		// Make sure we can't create any duplicates
		final String newId = newPlayer.getPlayerId();
		final String newHandle = newPlayer.getPlayerHandle();
		final boolean handleExist = players.stream()
			.anyMatch(p -> p.getPlayerHandle().equalsIgnoreCase(newHandle));
		final boolean idExist = players.stream()
				.anyMatch(p -> p.getPlayerId().equalsIgnoreCase(newId));

		if (handleExist || idExist) {
			throw new InvalidPlayerException("Player with same details already at table");
		}
		
		// Limit max players
		if (players.size() == settings.getGameType().getMaxPlayers()) {
			throw new InvalidPlayerException("Max players reached - rejecting user");
		}
		
		final PlayerStack newStack = newPlayer.getCurrentStack();
		
		if (newStack.getStack() < settings.getBuyInAmount() && !settings.isHostControlledWallet()) {
			throw new InvalidPlayerException("Player has insufficient funds in wallet");
		}

		// Cash sent as 'stack', so transfer to wallet (because wallet isn't visible)
		logger.trace("Initialising player '{}' stack", newPlayer.getPlayerId());
		float initialDeposit = newPlayer.getCurrentStack().getStack();
		
		// Transfer initial stake from wallet
		if (settings.getFormat()==GameFormat.TOURNAMENT) {
			addToTournamentPrizeFund( settings.getBuyInAmount() );
			
			// the whole opening stack, so nothing in the player's wallet
			newStack.initialise(0, false );
			newStack.setStack(settings.getOpeningStack());
			
			if (getRound() > settings.getMaxRoundForTournamentEntry()) {
				throw new InvalidPlayerException("Sorry, you cannot join this tournament after round " + settings.getMaxRoundForTournamentEntry());
			}
		} else {
			newStack.initialise(initialDeposit, settings.isHostControlledWallet() );
			newStack.reBuy(settings.getBuyInAmount(), 0);
		}
		
		
		// Initialise the player state
		logger.trace("Initialising player '{}' state", newPlayer.getPlayerId());
		newPlayer.getState().initialise(gameState, settings);

		final boolean isTest = "test".equals(settings.getGameId()) || "test-tour".equals(settings.getGameId());
		final boolean isHostEmail = settings.getHostEmail().equalsIgnoreCase(newPlayer.getEmail());
		
		// Set as host...
		if (isHostEmail || (isTest && players.size()==0) ) {
			newPlayer.getState().setHost(true);
			
			// Set host as dealer - This enables the host to start the game
			if (players.getActionOn()==null || roundNum==0) {
				newPlayer.getState().setDealer(true);
				/* 
				 * Need to confirm everything works fine without this (removed due to setting the auto-fold timer on host joining
				 * newPlayer.getState().setActionOn(true);
				 */
			}
		}
		
		players.add(newPlayer);
		
		// if a player re-joining, transfer their stats
		if (removedPlayers.contains(newPlayer)) {
			final Player old = removedPlayers.stream().filter(newPlayer::equals).findFirst().orElse(null);
			newPlayer.getCurrentStack().getGameStats().applyStats(old.getCurrentStack().getGameStats());
			removedPlayers.remove(old);
		}
		logger.trace("Player {} added to game {}", newPlayer.getPlayerId(), settings.getGameId());
	}

	/**
	 * Remove a player from the current game and sends a broadcast message to the chat window
	 * 
	 * @param playerId The player's persistent ID
	 * 
	 * @return The removed player if successful, otherwise null
	 */
	@JsonIgnore
	public Player removePlayer(final String playerId) {
		if (StringUtils.isBlank(playerId)) {
			return null;
		}
		logger.debug("Removing player {} from game {}", playerId, settings.getGameId());
		final Player toRemove = players.getPlayerById(playerId);
		
		if (toRemove==null) {
			return null;
		}

		/* Reset so the stats are correct, and then cash the player out.
		 * Messaging and storage must be handled by the callback */
		toRemove.resetForNewRound(0);
		toRemove.cashOut();
		players.remove(toRemove);

		Player dealer = null;
		// If required, move the dealer
		if (toRemove.getState().isDealer()) {
			dealer = moveDealerPosition();
		} else {
			dealer = players.getDealer();
		}
		
		if (toRemove != null) {
			removedPlayers.add(toRemove);
			// Transfer the host options to the new dealer. If there is no dealer, 
			// try and get the next player in turn 
			if (toRemove.getState().isHost()) {
				if (dealer != null) {
					dealer.getState().setHost(true);
				} else if (players.size() > 0) {
					// fallback option, just get the next player
					players.getPlayerRelativeTo(toRemove, 1, false).getState().setHost(true);
				}
			}
			removedCallbacks.forEach(rc-> rc.playerRemoved(settings.getGameId(), toRemove));
		}
		logger.trace("Removed player {} from game {}", playerId, settings.getGameId());
		
		if (!gotPlayerNumbers()) {
			logger.debug("Not enough players to continue game {}. Completing", settings.getGameId());
			gameState = TexasGameState.COMPLETE;
			roundCompleted(null);
		}
		
		return toRemove;
	}
	
	/**
	 * If the game is pre-deal, shuffle the deck
	 * 
	 * @return True if the cards were shuffled
	 */
	public boolean shuffle() {
		lastActivityTime = System.currentTimeMillis();
		
		if (gameState == TexasGameState.PRE_DEAL || gameState == TexasGameState.COMPLETE) {
			deck.shuffle();
			return true;
		}

		return false;
	}

	/**
	 * Get the game's current state
	 * 
	 * @return The state
	 */
	@JsonProperty
	public TexasGameState getGameState() {
		return gameState;
	}

	/**
	 * Perform the next deal
	 * 
	 * @return The result of the deal attempt
	 */
	public abstract DealResult deal();

	/**
	 * Get whether we have the requisite number of players. This assumes a 'resetForNewGame' on players
	 * has been performed so anyone that is going to sit out, has been set as sat-out
	 * 
	 * @return True if the number of players >= min and <= max
	 */
	@JsonIgnore
	protected boolean gotPlayerNumbers() {
		int inHand = players.getPlayersInHand(false).size();
		return inHand >= settings.getGameType().getMinPlayers()
				&& inHand <= settings.getGameType().getMaxPlayers();
	}

	/** Pause the player by sitting them out and moving the action
	 * to the next player.
	 * 
	 * @param playerId The player's persistent ID
	 * @param sessionId The player's session id
	 * 
	 * @return The player being 'paused'
	 */
	@JsonIgnore
	public abstract Player pausePlayer(final String playerId, final String sessionId);
	
	/** Call to complete and close a game correctly, releasing any resources
	 * 
	 */
	public abstract void completeGame();

	/** Add a listener for when a player is removed from the game
	 * 
	 * @param listener
	 */
	public void addRemovePlayerListener(final PlayerRemovedListener listener) {
		this.removedCallbacks.add(listener);
	}
	/**
	 * @return the gameType
	 */
	@JsonProperty
	public GameType getGameType() {
		return settings.getGameType();
	}

	/**
	 * @return the deck
	 */
	@JsonIgnore
	public Deck getDeck() {
		return deck;
	}

	/**
	 * Get the cards currently visible on the table
	 * 
	 * @return The set of cards
	 */
	@JsonProperty
	public List<Card> getCardsOnTable() {
		return cardsOnTable;
	}

	/**
	 * @return the players
	 */
	@JsonProperty
	public Players getPlayers() {
		lastActivityTime = System.currentTimeMillis();
		return players;
	}

	/**
	 * @return the settings
	 */
	@JsonProperty
	public T getSettings() {
		return settings;
	}
	
	/** Add a listener for when a round within a game completes
	 * 
	 * @param listener
	 */
	public void addRoundListener(final RoundCompleteListener listener) {
		if (listener!=null) {
			this.roundCallbacks.add(listener);
		}
	}
	
	/** Should be called every time a round is complete
	 * 
	 * @param round The RoundHistory at the current time. If Null the 
	 * default round history information is built and sent to the callbacks 
	 */
	protected void roundCompleted(final RoundHistory round) {
		logger.debug("Calling round complete on game {}", settings.getGameId());
		lastRoundCompletedTime = System.currentTimeMillis();
		final RoundHistory useRound = round == null ? buildGameRound() : round;
		roundCallbacks.forEach(l -> l.roundComplete(useRound));
	}
	
	/** Build game round (history) information at the current state
	 * 
	 * @return a new GameRound
	 */
	protected GameRound buildGameRound() {
		final String[] tCards = getCardsOnTable()
				.stream()
				.map(Card::getCode)
				.collect(Collectors.toList())
				.toArray(new String[0]);
		
		return new GameRound.Builder()
				.gameId(settings.getGameId())
				.round(getRound())
				.state(gameState)
				.players(players.clone())
				.pots(gamePot)
				.seed(deck.getLastSeed())
				.tableCards(tCards)
				.build();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AbstractCardGame [gameState=");
		builder.append(gameState);
		builder.append(", settings=");
		builder.append(settings);
		builder.append(", onTheTable=");
		builder.append(cardsOnTable);
		builder.append("]");
		return builder.toString();
	}

}
