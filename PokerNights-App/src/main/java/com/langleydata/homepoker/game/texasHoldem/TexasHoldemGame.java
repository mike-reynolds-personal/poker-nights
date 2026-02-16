package com.langleydata.homepoker.game.texasHoldem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.exception.GameStartException;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.DealCompleteCallback;
import com.langleydata.homepoker.game.PokerMathUtils;
import com.langleydata.homepoker.game.RoundHistory;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.PlayerStack;
import com.langleydata.homepoker.game.players.PlayerState;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;
import com.langleydata.homepoker.message.AnteDueMessage;
import com.langleydata.homepoker.message.CompleteGameMessage;
import com.langleydata.homepoker.message.DealUserMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.Messaging;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.message.ShowCardsMessage;
import com.langleydata.homepoker.message.StatusMessage;
import com.langleydata.homepoker.player.PlayerInfo;


/** All the logic encapsulated within Texas Hold'em poker
 * 
 * @author reynolds_mj
 *
 */
public class TexasHoldemGame extends AbstractCardGame<TexasHoldemSettings> {
	private final Logger logger = LoggerFactory.getLogger(TexasHoldemGame.class);
	private boolean autoCompletingGame = false;
	private static Lock lock = new ReentrantLock();
	protected PokerHandEvaluator sevenCardEvaluator;
	private Player bigBind;
	private List<DealCompleteCallback> dealCallbacks = new ArrayList<>();
	private boolean anteAutoUpBeforeDeal = false;
	final Messaging msgUtils;
	private long blindIncreaseAt = -1;
	Timer inActionTimer = null, blindIncreaseTimer = null;
	
	/** This constructor is used for creation from a pre-configured set of settings
	 * 
	 * @param settings The settings
	 */
	public TexasHoldemGame(TexasHoldemSettings settings, final Messaging msgUtils) {
		super(settings);
		this.msgUtils = msgUtils;
		sevenCardEvaluator = new PokerHandEvaluator();
		if (settings.getActionTimeout() > 0) {
			inActionTimer = new Timer("InAction Timer");
			inActionTimer.scheduleAtFixedRate( new TimerTask() {
				@Override
				public void run() {
					doInactionProcess();
				}
			}, 30000, 1000);
		}
	}

	@VisibleForTesting
	@JsonIgnore
	void setGameState(TexasGameState state) {
		this.gameState = state;
	}
	
	@VisibleForTesting
	@JsonIgnore
	protected void setEvaluator(PokerHandEvaluator eval) {
		this.sevenCardEvaluator = eval;
	}

	@Override
	public Player startNextRound(boolean moveDealer) throws GameStartException {
		
		final boolean isTournament = getSettings().getFormat()==GameFormat.TOURNAMENT;
		Player dealer = null;
		if (!lock.tryLock()) {
			return null;
		}
		try {

			dealer = super.startNextRound(moveDealer);
			
			if (getRound()==1 && isTournament) {
				blindIncreaseAt = System.currentTimeMillis() + settings.getBlindIncreaseInterval();
				blindIncreaseTimer = new Timer("Ante Increase");
				blindIncreaseTimer.scheduleAtFixedRate(
						new IncreaseAnteTimer(this), 
						settings.getBlindIncreaseInterval(),
						settings.getBlindIncreaseInterval()
						);
				dealer = players.setRandomDealer(false);
			}
			
			setMinRaise(settings.getBigBlind());
			gamePot.clear();
	
			// Set the blinds for the next round
			final Player small = players.getPlayerRelativeTo(dealer, 1, !isTournament);
			bigBind = players.getPlayerRelativeTo(dealer, 2, !isTournament);
			small.getState().setBlindsDue(Blinds.SMALL);
			bigBind.getState().setBlindsDue(Blinds.BIG);
			
			
			// What to do with blinds...
			if (isTournament) {
				
				// automatically post blinds
				small.getState().setActionOn(true);
				doGameUpdateAction( new PlayerActionMessage(small.getSessionId(), PlayerActionType.POST_BLIND) );
				doGameUpdateAction( new PlayerActionMessage(bigBind.getSessionId(), PlayerActionType.POST_BLIND) );
				
			} else {
				
				// Done here because player maybe sat-out, and in tournament they still post a blind
				if (players.moveActionToNextPlayer(dealer)==false) {
					throw new GameStartException("Failed to move action to next player");
				}
				
				// Notify the users
				msgUtils.sendPrivateMessage( 
						small.getSessionId(), 
						new AnteDueMessage(small.getSessionId(), Blinds.SMALL)
						);
				msgUtils.sendPrivateMessage( 
						bigBind.getSessionId(), 
						new AnteDueMessage(bigBind.getSessionId(), Blinds.BIG)
						);
			}
			
		} catch (GameStartException gse) {
			gameState = TexasGameState.COMPLETE;// So the action can be tried again
			throw gse;
		} finally {
			try {
				lock.unlock();
			} catch (IllegalMonitorStateException ignore) {}
		}
	
		return dealer;
	}

	/** This method is called each time the game is serialised and automatically
	 * increases the ante if required
	 * 
	 * @return the time in milliseconds of the next increase in blinds
	 */
	public long getBlindIncreaseAt() {
		return blindIncreaseAt;
	}

	/** Validate whether the big blind has;
	 * <ul>
	 * <li> Met or exceeded the big blind value and checked and the pot has equalised, or
	 * <li> Has folded, or
	 * <li> We're auto-completing the game
	 * </ul>
	 * 
	 * @return A DealResult containing the state, or Null if all checks pass
	 */
	DealResult validateBigBlindCheck() {

		if (bigBind == null) {
			logger.error("Unable to find who was the big blind in game '" + settings.getGameId() + "' Completing game to reset");
			currentPot = 0;
			gameState = TexasGameState.COMPLETE;
			return DealResult.GENERIC_ERROR;
		}
		
		final PlayerState bbState = bigBind.getState();
		boolean bbEqual = bigBind.getCurrentStack().getOnTable() >= settings.getBigBlind();
		if (anteAutoUpBeforeDeal) {
			/* If in a tournament game and the blind is increased automatically
			 * after the game has started, but before the BB has checked, then assume
			 * their bet is equal. If this doesn't happen then this test never passes and
			 * we end-up in a loop */
			bbEqual = true;
		}
		final boolean bbChecked =  bbState.getLastAction() == PlayerActionType.CHECK ||  bbState.getLastAction().isValueBet();
		final boolean stillInHand = bbState.isFolded()==false && bbState.isSittingOut() == false;
		
		if ( (bbEqual && bbChecked)==false && !autoCompletingGame && stillInHand) {
			if (!hasPotEqualized()) {
				return DealResult.WAITING_ON_BETS;
			} else {
				return DealResult.WAITING_BB_CHECK;
			}
		}
		return DealResult.SUCCESS;
	}
	
	@Override
	public DealResult deal() {

		DealResult result = DealResult.SUCCESS;
		
		switch (gameState) {
		case PRE_DEAL: // Waiting blinds to be posted
		case COMPLETE:
			if (!gotPlayerNumbers()) {
				result = DealResult.NOT_ENOUGH_PLAYERS; 
				break;
			}
 
			// Have blinds been posted?
			final Player dueFrom = players.getActionOn();
			if (dueFrom!=null && dueFrom.getState().getBlindsDue()!=Blinds.NONE) {
				msgUtils.sendPrivateMessage( 
						dueFrom.getSessionId(), 
						MessageUtils.PRIVATE_QUEUE,
						new AnteDueMessage( dueFrom.getSessionId(), dueFrom.getState().getBlindsDue() ),
						250L);
				result = DealResult.BLINDS_DUE; 
				break;
			}
			
			// If blinds were posted, we can deal the cards, but we're still in the first
			// round, so have to wait for everyone else to bet and equalise
			deck.dealCardsToPlayers(players, settings.getGameType().getStartCards(), true);

			// Send the player their cards privately (!)
			players.stream()
				.filter(Player::isStillInHand)
				.forEach(p -> msgUtils.sendPrivateMessage(p.getSessionId(),
								MessageUtils.PRIVATE_QUEUE, 
								new DealUserMessage(p.getSessionId(), p.getCards()),
								2000L)
						);
			
			gameState = TexasGameState.POST_DEAL;
			break;
			
		case POST_DEAL: // Blinds posted, dealt cards to players, waiting on betting
			
			if (autoCompleteIfNoBets()==DealResult.AUTOCOMPLETING) {
				return DealResult.AUTOCOMPLETING;
			}
			
			// Has the big blind checked?
			final DealResult bbRes = validateBigBlindCheck();
			if (bbRes != DealResult.SUCCESS) {
				result = bbRes;
				break;
			}
			
			if (!tryCollectBetsAndReset()) {
				result = DealResult.WAITING_ON_BETS;
				break;
			}
			anteAutoUpBeforeDeal = false;// reset the auto ante increase flag
			// Deal the flop
			cardsOnTable = IntStream.range(0, 3).mapToObj(i -> deck.getNextCard()).collect(Collectors.toList());
			gameState = TexasGameState.FLOP;
			players.resetActionPositionForDeal();
			break;
		case FLOP:
			
			if (autoCompleteIfNoBets()==DealResult.AUTOCOMPLETING) {
				result = DealResult.AUTOCOMPLETING;
				break;
			}
			
			if (!tryCollectBetsAndReset()) {
				result = DealResult.WAITING_ON_BETS;
				break;
			}
			
			cardsOnTable.add(deck.getNextCard());
			players.resetActionPositionForDeal();
			gameState = TexasGameState.TURN;
			break;
		case TURN:
			
			if (autoCompleteIfNoBets()==DealResult.AUTOCOMPLETING) {
				result = DealResult.AUTOCOMPLETING;
				break;
			}
			
			if (!tryCollectBetsAndReset()) {
				result = DealResult.WAITING_ON_BETS;
				break;
			}
			
			cardsOnTable.add(deck.getNextCard());
			players.resetActionPositionForDeal();
			gameState = TexasGameState.RIVER;
			break;
		case RIVER:
			
			if (autoCompleteIfNoBets()==DealResult.AUTOCOMPLETING) {
				result = DealResult.AUTOCOMPLETING;
				break;
			}
			
			if (!tryCollectBetsAndReset()) {
				result = DealResult.WAITING_ON_BETS;
				break;
			}

			calculateAndNotifyWinners();

			// Store the history by calling the call-back
			roundCompleted(null);
			
			// Move the dealer, and wait for them to start the game
			moveDealerPosition();

			players.autoExcludeZeroStacks();
			
			gameState = TexasGameState.COMPLETE;
			break;
			
		default:
			result = DealResult.NO_DEAL;
		}
		
		final DealResult sResult = result;
		dealCallbacks.forEach(c -> c.dealCompleted(cardsOnTable, sResult));
		return result;
	}

	/** Add a callback to be notified when a deal is comlpete
	 * 
	 * @param callback
	 */
	public void addDealCompleteCallback(DealCompleteCallback callback) {
		this.dealCallbacks.add(callback);
	}
	
	/** Calculate the round winner and game pot(s) and then reveal the cards appropriately
	 * TODO: Add individual tests?
	 * 
	 */
	@SuppressWarnings("unlikely-arg-type")
	void calculateAndNotifyWinners() {
		final Player beingCalled = players.getLastPlayerToAction(true);

		// 1 calculate winning hand - here the winnings are distributed to each pot winner
		gamePot = sevenCardEvaluator.calculatePotsAndWinners(players, getCardsOnTable());
		
		final List<Winner> potWinners = gamePot.getWinners();
		players.setWinners( new ArrayList<PlayerInfo>(potWinners) );
		
		// 2 Flip the cards of the person being called
		if ( beingCalled != null ) {
			sendRevealCards(beingCalled, beingCalled.getPlayerHandle() + " was called", false);
			potWinners.remove(beingCalled); // So not revealed again if also won
		}
		
		// 3 All the winners show their cards - a show-down!
		potWinners.stream()
			.map( w -> players.getPlayer(w))
			.forEach( p -> sendRevealCards(p, p.getPlayerHandle() + " proves they won", true));
		
		if (settings.getFormat()==GameFormat.TOURNAMENT) {
			rankTourneyLoosers();
			if (rankTourneyWinners()) {
				// complete the game
				gameState = TexasGameState.COMPLETE;
				settings.setCompleted();
				msgUtils.sendBroadcastToTable(settings.getGameId(), new CompleteGameMessage(getPlayerStats()), 2000);
			}
		}
	}

	/** Rank the top spots of the tournament
	 * 
	 * @return
	 */
	boolean rankTourneyWinners() {
		int reqWinners = settings.getTournamentSplit().size();
		final List<Player> pLeft = players.stream()
				.filter(p -> p.getCurrentStack().getStack() > 0)
				.sorted(Comparator.comparing(p->p.getCurrentStack().getStack(), Comparator.naturalOrder()))
				.collect(Collectors.toList());

		if (pLeft.size() == reqWinners) {
			final int prizeFund = getTournamentPrizeFund();
			// We have the requisite number of winners left
			for (Player p : pLeft) {
				p.getCurrentStack().getGameStats().setRank(reqWinners);
				p.getCurrentStack().setStack(0);
				float winVal = (settings.getTournamentSplit().get(reqWinners-1) / 100f) * prizeFund;
				p.getCurrentStack().setStack(winVal);
				p.cashOut();
				reqWinners--;
			}
			return true;
		}
		return false;
	}
	
	/** Rank the players that have gone bust
	 * 
	 */
	void rankTourneyLoosers() {
		// All un-ranked players that are at zero, sorted by largest bet first
		final List<Player> zeroStack = players.getUnRankedLoosers();
		if (zeroStack.size() > 0) {
			final List<Player> ranked = players.getRankedPlayers();
			int nextRank = players.size();
			if (ranked.size() > 0) {
				nextRank = ranked.get(ranked.size()-1).getCurrentStack().getGameStats().getRank() - 1;
			}
			for (Player p : zeroStack) {
				p.getCurrentStack().getGameStats().setRank(nextRank);
				p.cashOut();
				nextRank--;
			}
		}
	}
	
	/** Check if we have sufficient players in the game that can bet.<p>
	 * <li>If only one player left, they win automatically.
	 * <li>If only one person left that can bet, complete the game round to decide the winner. 
	 * 
	 * @return Either a AUTOMPLETE result, or the game is finished early with a NO_DEAL result
	 */
	DealResult autoCompleteIfNoBets() {
		
		if (autoCompletingGame) {
			return DealResult.NO_DEAL;
		}
		
		
		// If the only player left standing, they've won outright
		// Don't exclude zero stacks as they could be all-in
		final List<Player> playersInHand = players.getPlayersInHand(false);
		if (playersInHand.size() < 2) {
			// If only one player in the game, the loser must have folded
			finishGameEarly(playersInHand.get(0));
			return DealResult.AUTOCOMPLETING;
		}

		// Check if any more bets are possible
		// and if not, complete the round automatically
		final boolean completeGame = !autoCompletingGame
				&& players.getPlayersInHand(true).size() < 2
				&& tryCollectBetsAndReset();
				
		if (completeGame) {

			autoCompletingGame = true;
			
			int safety = 0;
			while (gameState != TexasGameState.COMPLETE && safety < 6) {
				deal();
				safety++;
			}
			// Ensure player and dealer states are reset for the new game
			players.forEach(p->p.getState().resetForNewDeal());
			players.getDealer().getState().setActionOn(true);
			
			autoCompletingGame = false;
			return DealResult.AUTOCOMPLETING;
		}
		
		return DealResult.NO_DEAL;
	}

	/** Perform a fold or reveal action for the specified player
	 * 
	 * @param player
	 * @param action
	 * @return True if the action should be moved to the next player and a deal attempted, otherwise false to not proceed
	 */
	boolean foldOrRevealPlayer(final Player player, final PlayerAction action) {
		
		boolean success = false;
		
		if (	!action.getAction().isFold() || 
				player.getState().getBlindsDue()!=Blinds.NONE || 
				player.getState().getLastAction().isFold()) {
			action.setSuccessful(success);
			return success;
		}
		
		final boolean isComplete = gameState == TexasGameState.COMPLETE;
		
		// If not the end of the game and action on the player, do it...
		if (!isComplete && player.getState().isActionOnMe()) {
			
			success = addPlayerBet(action, player);
			action.setSuccessful(success);
			
			if (success) {
				player.getState().setLastAction(action.getAction());
				if (action.getAction()==PlayerActionType.REVEAL) {
					sendRevealCards(player);
				} else {
					logger.debug("Player {} {} in game {}", player.getPlayerId(), action.getAction().getFriendlyMessage(), getSettings().getGameId());
				}
			}
			
			return success;
			
		} else if (!isComplete) {
			
			action.setSuccessful(success);
			return success;
			
		}
		
		// If we got here, it can be any player folding or revealing cards
		if (action.getAction()==PlayerActionType.REVEAL) {
			sendRevealCards(player);
		}
		
		action.setSuccessful(true);		
		player.getState().setLastAction(action.getAction());
		
		return false;
	}
	
	/** Send a ShowCardsMessage message to the table
	 * 
	 * @param player
	 * @param message
	 * @param isWinner
	 */
	private void sendRevealCards(final Player player, final String message, final boolean isWinner) {
		
		final ShowCardsMessage reveal = new ShowCardsMessage(player.getSessionId())
				.setCards(player.getCards().size()==0 ? player.getLastCards() : player.getCards())
				.setHandle(player.getPlayerHandle())
				.setSeat(player.getSeatingPos())
				.setWinner(isWinner);
		reveal.setMessage(message);
		msgUtils.sendBroadcastToTable(settings.getGameId(), reveal);
		
	}
	/** Send a reveal cards message to the table
	 * 
	 * @param player
	 */
	private void sendRevealCards(final Player player) {
		sendRevealCards(player, null, false);
	}
	
	private GameUpdateMessage errorAction(final PlayerAction action, final String message) {
		action.setSuccessful(false).setMessage(message);
		try {
			lock.unlock();
		} catch (Exception e) {/*ignore*/}
		return null;
	}
	/**
	 * We have received a message to perform the action. Uses the sessionId to get the user
	 * 
	 * @param action The action message to operate on. Always uses the sessionId from the action to find the relevant player
	 * @return a new GameUpdateMessage
	 */
	@Override
	public GameUpdateMessage doGameUpdateAction(final PlayerAction action) {
		lastActivityTime = System.currentTimeMillis();
		
		final TexasGameState prevState = getGameState();
		boolean moveActionAndDeal = false;
		
		final Player player = players.getPlayerBySessionId(action.getSessionId());
		if (player == null) {
			action.setSuccessful(false).setMessage("Player not found with session id: " + action.getSessionId());
			return null;
		}

		final PlayerState state = player.getState();

		// Pre-validate the actions

		// Check it's this player's turn
		if (!state.isActionOnMe() && action.getAction().onTurnOnly() ) {
			// If the game is complete and its a fold, don't return
			if ( (gameState == TexasGameState.COMPLETE && action.getAction().isFold())==false) {
				return errorAction(action, "Action not on player " + player.getPlayerHandle());
			}

		} else if (state.getBlindsDue() != Blinds.NONE 
				&& action.getAction() != PlayerActionType.POST_BLIND
				&& !action.getAction().isFold()) {
			// Check if player is a blind and not posting a blind, reject it
			return errorAction(action, "You must post the blind first");

		}
		
		// Acquire a lock so another user's action doesn't interfere
		if (!lock.tryLock()) {
			return errorAction(action, "Unable to obtain lock for action");
		}
		
		boolean bigBlindPosted = false;
		// Attempt remaining actions...
		switch (action.getAction()) {
		case POST_BLIND:
			if (state.getBlindsDue() == Blinds.SMALL) {
				moveActionAndDeal = player.getCurrentStack().addToTable(TexasGameState.POST_DEAL, settings.getAnte());
				state.setBlindsDue(Blinds.NONE);
			} else if (state.getBlindsDue() == Blinds.BIG) {
				moveActionAndDeal = player.getCurrentStack().addToTable(TexasGameState.POST_DEAL, settings.getBigBlind());
				state.setBlindsDue(Blinds.NONE);
				bigBlindPosted = true;
			}
			break;
		case CHECK: // no bet
			final boolean bigBlindChecking = (state.wasBigBlind() && state.getBlindsDue() != Blinds.BIG);
			moveActionAndDeal = PokerMathUtils.floatEqualsZero(getRequiredBet()) || bigBlindChecking;
			if (moveActionAndDeal) {
				// Add 0 to table so the 'commit-per-round' has a value, so when checking pots equal, 
				// it won't generate a random number
				player.getCurrentStack().addToTable(gameState, 0f);
			}
			break;
		default:
			// All other actions involve collecting bets
			if (action.getAction().isFold()) {
				moveActionAndDeal = foldOrRevealPlayer(player, action);
				if (!action.isSuccessful()) {
					return errorAction(action, "Action already prcocessed");
				}
			} else {
				if (gameState !=TexasGameState.COMPLETE) {
					moveActionAndDeal = addPlayerBet(action, player);
				}
			}
			break;
		}

		final GameUpdateMessage gum = new GameUpdateMessage(this);
		
		// If the action was carried out, move the action on,
		// configure the bets and then perform a deal
		if (moveActionAndDeal) {

			if (action.getAction().isValueBet() || bigBlindPosted) {
				setNewBets(player.getCurrentStack().getOnTable());
			}
			
			// Move action to next player
			state.setLastAction(action.getAction());
			players.moveActionToNextPlayer(player);
			
			// Do the deal (if possible)
			deal();
			// Do the sound after dealing so we can detect the change in state
			gum.setActionSound(getActionSound(prevState, action));
		}

		action.setSuccessful(true);
		
		lock.unlock();
		return gum;
	}
	
	/** Set the game bet values based on a new bet being placed
	 * 
	 * @param totalPlayerBet The last player's on table value
	 */
	void setNewBets(final float totalPlayerBet) {
		float prevRequired = getRequiredBet();
		// Ensure the bet stays at the highest amount placed so far
		// As the player maybe going all-in without enough money
		float newRequired = Math.max(prevRequired, totalPlayerBet);
		setLastRaise( Math.max(getLastRaise(), newRequired - prevRequired) );
		
		setRequiredBet(newRequired);
		
		if (settings.isEnforceMinimumRaise()) {
			// Standard rule is the required bet + the previous raise
			setMinRaise(newRequired + getLastRaise());
		} else {
			// Otherwise just the require bet + big blind
			setMinRaise(getRequiredBet() + settings.getBigBlind());
		}
	}
	
	/** Complete a game early regardless of the current state.
	 * Note this should only be used if there is ONLY 1 person left in the hand
	 * 
	 * @param winner If not null, this player will take the current pot
	 */
	private void finishGameEarly(final Player winner) {
	
		currentPot += players.collectBets(gameState);
		setRequiredBet(0);
		
		final SidePot sidepot = new SidePot(currentPot);
		
		if (winner != null) {
			winner.getCurrentStack().transferWinAmount(currentPot);
			players.setWinners(Collections.singletonList(winner));
			sidepot.addCompetingWinner(winner);
			currentPot = 0;
			// Inform all users
			final StatusMessage msg = new StatusMessage(winner.getPlayerHandle() + " takes the pot!");
			msgUtils.sendBroadcastToTable(settings.getGameId(), msg, 1000);
		}
		
		/* Store the history by calling the call-back. Have to manually build
		 * the gamePot as haven't gone through the hand evaluator */
		this.gamePot = new GamePots();
		this.gamePot.addSidePot(sidepot);
		roundCompleted(null);
		
		// Send an 'end-of-game' update, before players get
		// reset to ensure the UI is updated
		msgUtils.sendBroadcastToTable(settings.getGameId(), new GameUpdateMessage(this) );
		
		// Reset players for next game so they can be included (or excluded if sitting out)
		// from the movement of the dealer position which happens next
		players.forEach( p-> p.resetForNewRound(settings.getBuyInAmount()));
		
		// move the dealer on so they can start the new game
		// This is done here because the result of this method doesn't go through
		// the rest of the 'normal' deal process
		moveDealerPosition();
		
		players.autoExcludeZeroStacks();
		
		// Set-up next game
		gameState = TexasGameState.COMPLETE;
	}

	/** Calculate the actual bet value, dependent on the action being performed,
	 * the players stack and what other's have available
	 * 
	 * @param action The current player's action
	 * @param player The current player
	 * @return True if successful
	 */
	boolean addPlayerBet(final PlayerAction action, final Player player) {
		
		if (player.isStillInHand()==false) {
			return false;
		}

		final PlayerStack currentStack = player.getCurrentStack();
		float maxBet = getPlayers().getMaxBetPossible(player);

		float addToTable = 0f;
		
		// This section determines the total that the player wants to bet,
		// keeping in mind that a 'Bet' is additional to what is already on the table
		switch (action.getAction()) {
		case CALL: // match the last bet
			addToTable = getRequiredBet() - currentStack.getOnTable();
			break;
		case BET: // An absolute value
			boolean allIn = action.getBetValue() == currentStack.getRoundedTotalStack();
			if (action.getBetValue() >= getRequiredBet() || allIn ) {
				addToTable = action.getBetValue() - currentStack.getOnTable();
			} else {
				return false;
			}
			break;
		case RAISE: // An additional value (not really used now)
			addToTable = (getRequiredBet() + action.getBetValue()) - currentStack.getOnTable();
			break;
		case ALL_IN: // everything they have
			addToTable = currentStack.getStack();
			break;
		case FOLD:
		case REVEAL:
			currentPot += currentStack.collectBets();
			action.setBetValue(0f);
			return true;
		default:
			return false;
		}
		
		// If trying to bet more than anyone has, bet the maximum allowed, less what we've already put in
		if (maxBet > 0 && maxBet < (addToTable+currentStack.getOnTable())) {
			addToTable = maxBet - currentStack.getOnTable();
		}
		
		final boolean added = currentStack.addToTable(gameState, addToTable) || (getRequiredBet()==0 && addToTable == 0);
		
		if (added) {
			// Check if the player is actually all-in now?
			if (player.getCurrentStack().getStack() == 0) {
				action.setAction(PlayerActionType.ALL_IN);
				action.setBetValue(currentStack.getOnTable() - action.getBetValue());
				player.getState().setAllIn(true);
			} else {
				player.getState().setLastAction(action.getAction());
			}
			
			// Set the actual value they bet
			action.setBetValue(currentStack.getOnTable());
			
			// Voluntary bets and VPIP calculation
			if (action.getAction().isValueBet()) {
				if ((gameState==TexasGameState.PRE_DEAL || gameState == TexasGameState.POST_DEAL)) {
					currentStack.getGameStats().addVoluntaryBet(getRound());
				} else if (gameState==TexasGameState.FLOP && player.getState().wasBigBlind()) {
					currentStack.getGameStats().addVoluntaryBet(getRound());
				}
			}
		} 
		
		return added;
	}


	/** Are all bets in this round the same, excluding those that
	 * have gone all-in.<p>
	 * We need to ensure that players that have gone all-in (because they cannot match the
	 * current bet) do not stop the betting round completing. But if other players can match 
	 * the bet then the pot hasn't equalised.
	 * 
	 * @return
	 */
	boolean hasPotEqualized() {
		if (autoCompletingGame) {
			return true;
		}
		final List<Player> inHand = getPlayers().getPlayersInHand(false);
		final float lastBet = getRequiredBet();
		
		/* Go through the players, getting the amount committed this round,
		 * filtering out those that have already matched
		 * the most recent bet and those that cannot bet any more (zero stack) */
		for (Player p : inHand) {
			final PlayerStack pStack = p.getCurrentStack();
			final float bet = pStack.getCommitedPerRound().getOrDefault(gameState, genRandomBet());
			if (PokerMathUtils.floatEquals(bet, lastBet)) {
				continue;// they've bet the required amount
			}
			if (PokerMathUtils.floatEqualsZero(pStack.getStack())) {
				continue;// They're all-in so can't bet anymore
			}
			// Nether of the above are true, therefore the pot hasn't equalised
			return false;
		}
		
		return true;
	}
	

	
	private float genRandomBet() {
		double rnd = Math.random();
		while (rnd == 0) {
			rnd = Math.random();
		}
		return (float)(rnd * 100f);
	}

	/** Check whether the bets in the round have equalised via {@link #hasPotEqualized()}.
	 * <p>If it has, and we're not auto-completing the round, collect player bets to the pot
	 * and reset all player states ready for the next.</p>
	 * This function is called on every player action to asses whether to move on to the next
	 * round.
	 * 
	 * @return True if bets are collected and the game can move to next round
	 */
	boolean tryCollectBetsAndReset() {
		
		if (!hasPotEqualized()) {
			return false;
		}
		
		if (autoCompletingGame) {
			return true;
		}

		currentPot += players.collectBets(gameState);
		resetBets(settings.getBigBlind());
		
		// Reset players for next set of cards
		players.forEach(p->p.getState().resetForNewDeal());
		
		return true;
	}

	@Override
	public Player pausePlayer(final String playerId, final String sessionId) {
		
		final Player player = players.getPlayerById(playerId);
		final PlayerActionMessage pa = new PlayerActionMessage(sessionId);
		GameUpdateMessage gum = null;
		
		if (player==null) {
			return null;
		}
		
		// Do this first so the action moves to correct player
		if (!player.getState().isSittingOut()) {
			player.getState().toggleSittingOut(getGameState(), true);
		}
		
		// If it's this players turn then either auto-fold them or check
		if (player.getState().isActionOnMe()) {
			if (getRequiredBet() > 0 && gameState!=TexasGameState.COMPLETE) {
				pa.setAction(PlayerActionType.FOLD);
				pa.setBetValue(getRequiredBet());
				gum = doGameUpdateAction(pa);
				logger.info("Player inactivity, game {}: Player {} automatically FOLDED", settings.getGameId(), playerId);
			} else if (PokerMathUtils.floatEqualsZero(getRequiredBet())  && gameState!=TexasGameState.COMPLETE) {
				pa.setAction(PlayerActionType.CHECK);
				pa.setBetValue(0);
				gum = doGameUpdateAction(pa);
				logger.info("Player inactivity, game {}: Player {} automatically CHECKED", settings.getGameId(), playerId);
			} else if (gameState==TexasGameState.COMPLETE && player.getState().isDealer()) {
				moveDealerPosition();
			}
		}
		
		if (gum!=null) {
			// If we've done a player action, the action would have moved
			// to the next player
			msgUtils.sendBroadcastToTable(getSettings().getGameId(), gum);
		}
		return player;
	}
	
	@Override
	protected void roundCompleted(RoundHistory rh) {
		super.roundCompleted(rh);

		setMinRaise(settings.getBigBlind());
		setLastRaise(0);
		
		gameState = TexasGameState.COMPLETE;
	}

	@Override
	protected String getActionSound(TexasGameState prevState, PlayerAction playerAction) {
		String soundClip = "";
		// Add a sound to go with the action performed
		if (playerAction.isSuccessful()) {
			if (playerAction.getAction().isValueBet()) {
				soundClip = "add_chips";
			} else if (playerAction.getAction()==PlayerActionType.CHECK) {
				soundClip = "check";
			} else if (playerAction.getAction()==PlayerActionType.POST_BLIND) {
				soundClip = "1chip";
			}
			TexasGameState newState = getGameState();
			if (prevState!=newState) {
				if (newState==TexasGameState.FLOP) {
					soundClip = "turn_3_cards";
				} else if (newState==TexasGameState.TURN || newState == TexasGameState.RIVER) {
					soundClip = "turn_1_card";
				}
			}
		}
		return soundClip;
	}

	@Override
	public void completeGame() {
		logger.info("Completing game: {}", settings.getGameId());
		
		if (blindIncreaseTimer!=null) {
			blindIncreaseTimer.cancel();
		}
		if (inActionTimer!=null) {
			inActionTimer.cancel();
		}
		
		/* Removing all the players correctly also cashes them out
		 * and calls related callbacks */
		new HashSet<>(getPlayers()).forEach( p -> {
			removePlayer(p.getPlayerId());
		});
		
		settings.setCompleted();
	}
	
	/** Auto-fold players that have spent too long with the action on them
	 * 
	 */
	boolean doInactionProcess() {
		final Player actionOn = players.getActionOn();
		
		if (getRound() == 0 || actionOn == null || gameState==TexasGameState.COMPLETE) {
			return false;
		}

		final PlayerState aos = actionOn.getState();

		if (aos.getNextAutoInaction() > 0 && aos.getNextAutoInaction() <= System.currentTimeMillis() && aos.getLastAction().isFold()==false) {
			
			// Determine the most appropriate action...
			PlayerActionType action;
			if (aos.getBlindsDue() != Blinds.NONE) {
				action = PlayerActionType.POST_BLIND;
			} else {
				if (aos.wasBigBlind() || PokerMathUtils.floatEqualsZero( getRequiredBet() )) {
					action = PlayerActionType.CHECK;
				} else {
					action = PlayerActionType.FOLD;
				}
			}
			
			logger.debug("Player {} automated action {} due to in-action", actionOn.getPlayerHandle(), action);

			final PlayerActionMessage pam = new PlayerActionMessage(actionOn.getSessionId(), action);
			pam.setGameState(getGameState())
				.setGameId(getSettings().getGameId())
			    .setRound(getRound())
			    .setMessage("Automated action: " + action.getFriendlyMessage());

			// Inform the player client of the action performed (really for completeness)
			msgUtils.sendPrivateMessage(pam.getSessionId(), pam);
			final GameUpdateMessage gum = doGameUpdateAction(pam);
			
			if (gum != null) {
				/* Re-apply the automated action as the player states could have
				 * changed due to the game update action. This keeps the UI in-synch */
				aos.setLastAction(action);
				msgUtils.sendBroadcastToTable(getSettings().getGameId(), gum, 500L);
			}
			
			// TODO Really should store the automated action here as well
			return true;
		}
		
		return false;
	}
	/** The automatic timer for increasing the ante in a Tournament
	 * 
	 */
	class IncreaseAnteTimer extends TimerTask {
		private AbstractCardGame<?> theGame;
		IncreaseAnteTimer(AbstractCardGame<?> theGame) {
			this.theGame = theGame;
		}
		@Override
		public void run() {
 			if (settings.increaseAnte()) {
				blindIncreaseAt = System.currentTimeMillis() + settings.getBlindIncreaseInterval();
				final float ante = Math.round(settings.getAnte() * 100) / 100f;
				final GameUpdateMessage gum = new GameUpdateMessage(theGame);
				gum.setMessage("The blinds have doubled to " + ante + " / " + (ante * 2));
				msgUtils.sendBroadcastToTable(settings.getGameId(), gum);
				anteAutoUpBeforeDeal = getGameState().getOrder() <= TexasGameState.POST_DEAL.getOrder();
				logger.info("Automatically increasing Ante for game {} to {}", settings.getGameId(), ante);
			} else {
				this.cancel();
				logger.debug("Maxiumum ante reached for game {}", settings.getGameId());
			}
		}
	}
}