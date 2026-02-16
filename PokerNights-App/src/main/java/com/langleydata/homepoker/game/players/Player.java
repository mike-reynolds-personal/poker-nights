package com.langleydata.homepoker.game.players;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.players.PlayerStack.RebuyState;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.player.PlayerInfo;

/** A class representing a single player and that player's state within the game
 * Note that this class is persisted to the DB
 * @author reynolds_mj
 *
 */
public class Player extends PlayerInfo {
	@Transient
	private final transient Logger logger = LoggerFactory.getLogger(Player.class);
	
	@Transient
	private transient List<Card> cards = new ArrayList<>();
	@Transient
	private transient List<Card> lastCards = new ArrayList<>();
	private PlayerStack currentStack = new PlayerStack();
	private int seatingPos = 0;
	@Transient
	private transient PlayerState state = new PlayerState();
	@Transient
	private transient HandRank rankedHand;
	@Field("cards")
	private String[] pastCards;
	@Transient
	private transient String sessionId;


	/**
	 * 
	 * @param playerId
	 * @param playerHandle
	 */
	public Player(final String playerId, final String playerHandle) {
		super(playerId, playerHandle);
	}


	/** Add a card to this player's cards
	 * 
	 * @param card
	 */
	public void addCard(final Card card) {
		this.cards.add(card);
	}

	/**
	 * @return the lastActionTime
	 */
	public long getLastActionTime() {
		return this.state.getLastActionTime();
	}

	/** Get the current sessionId.<p>
	 * The session id is tied to the browser session and is transient
	 * 
	 * @return
	 */
	public String getSessionId() {
		return this.sessionId;
	}
	
	/** Set a new sessionId on the player.<p>
	 * The session id is tied to the browser session and is transient
	 * 
	 * @param sessionId
	 */
	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}
	
	/**
	 * @return the rankedHand
	 */
	@JsonIgnore
	public HandRank getRankedHand() {
		return rankedHand;
	}

	/**
	 * @param rankedHand the rankedHand to set
	 */
	@JsonIgnore
	public void setRankedHand(HandRank rankedHand) {
		this.rankedHand = rankedHand;
	}

	/**
	 * @return the seatingPos
	 */
	public int getSeatingPos() {
		return seatingPos;
	}

	/**
	 * @param seatingPos the seatingPos to set
	 */
	public void setSeatingPos(int seatingPos) {
		this.seatingPos = seatingPos;
	}

	/**
	 * @param cards the cards to set
	 */
	@JsonIgnore
	public void setCards(List<Card> cards) {
		this.cards = cards;
	}
	

	/** Determined by the player sitting-in, not folded and has money
	 * in their stack.
	 * 
	 * @return True if all conditions met, otherwise false
	 */
	@JsonIgnore
	public boolean isStillInHand() {
		return !state.isSittingOut() && !state.isFolded() && currentStack.getStack() > 0f;
	}
	
	/**
	 * @return the cards
	 */
	@JsonIgnore
	public List<Card> getCards() {
		return cards;
	}

	/** Only used (currently) when a game completes early and is reset, but the
	 * player wants to 'Reveal'.
	 *  
	 * @return the cards
	 */
	@JsonIgnore
	public List<Card> getLastCards() {
		return lastCards;
	}
	
	/** Only populated on clone
	 * 
	 * @return
	 */
	@JsonIgnore
	public String[] getPastCards() {
		return pastCards;
	}
	
	/** Clear the player state down ready for a new round,
	 * excluding the dealer flag.
	 * 
	 * @param rebuyAmount If the player is set to auto-rebuy and their, 
	 *  stack is 0, then this amount is used.
	 * 
	 */
	public void resetForNewRound(int rebuyAmount) {
		lastCards = new ArrayList<>(cards);
		cards.clear();
		
		state.resetForNewRound(currentStack.getStack());
		currentStack.getCommitedPerRound().clear();
		rankedHand = null;
	}
	
	/** 'Cash-out' the player<p>
	 * This will transfer all money on the table to the players wallet
	 * and flag them so they can't re-join, effectively ending their game
	 * with the exception that they are not removed from any game.
	 * 
	 * @return The final amount in the player's wallet
	 */
	public float cashOut() {
		currentStack.cashOut();
		state.setCashedOut(true);
		currentStack.getGameStats().setTimePlayed(this.state.getTimePlayed());
		return currentStack.getWallet();
	}
	/** Get the state of the players stack
	 * 
	 * @return
	 */
	public PlayerStack getCurrentStack() {
		return this.currentStack;
	}

	/**
	 * @return the state
	 */
	public PlayerState getState() {
		return state;
	}
	
	/** Perform an action, initiated by this user, on the user state.
	 * This does not alter the game state at all.
	 * 
	 * @param action
	 * @param gameState
	 * @param settings
	 * @param maxRebuy
	 */
	@SuppressWarnings("incomplete-switch")
	public void doPlayerAction(PlayerAction action, final TexasGameState gameState, final GameSettings settings, final float maxRebuy) {

		// This method only handles actions that can be done out-of-turn
		if (action.getAction().onTurnOnly()) {
			return;
		}
		
		switch (action.getAction()) {
		case RE_BUY:

			boolean wasSuccess = false;
			
			if ( gameState!=TexasGameState.COMPLETE && gameState!=TexasGameState.PRE_DEAL && !settings.isBuyInDuringGameAllowed()) {
				wasSuccess = false;
				action.setSuccessful(false).setMessage("You cannot buy-in during a round");
			} else {
				final RebuyState boughtIn = getCurrentStack().reBuy(maxRebuy, settings.getAnte() * 2f);
				final String mValue = MessageUtils.formatMoney(getCurrentStack().getWallet(), settings);
				if (boughtIn==RebuyState.SUCCESS) {
					action.setSuccessful(true).setMessage("You now have " + mValue + " left in your wallet");
					wasSuccess = true;
					getState().toggleSittingOut(gameState, false);
				} else if (boughtIn==RebuyState.NO_FUNDS) {
					action.setSuccessful(false).setMessage("You have insufficient funds to buy back in (" + mValue + ")");
				} else if (boughtIn==RebuyState.NON_ZERO_STACK) {
					action.setSuccessful(false).setMessage("You cannot buy-in whilst you have money in your stack");		
				}
			}
			logger.debug("Player {} attempted re-buy. Success={}",  getPlayerId(), wasSuccess);
			break;
			
		case CASH_OUT:
			logger.debug("Player '{}' ({}) initiated a cash out", getPlayerHandle(), getPlayerId());
			cashOut();
			action.setSuccessful(true)
				.setMessage("Cashed out (" + formatBalance(settings)  + ")");
			break;
			
		case SIT_OUT:
			final boolean changeTo = action.getBetValue()==1 ? true : false;
			final boolean success = getState().toggleSittingOut(gameState, changeTo);
			String msg = "Sit-out toggle ";
			if (success) {
				msg += changeTo ? "on" : "off";
			} else {
				msg += "change failed";
			}
			action.setSuccessful(success).setMessage(msg);
			logger.debug(msg);
			break;
			
		case FOLD:
		case REVEAL:
			getState().setLastAction(action.getAction());
			action.setSuccessful(true);
			logger.debug("Player {} revealed/ folded",  getPlayerId() );

			break;
		}
	}
	
	public String formatBalance(final GameSettings settings) {
		final float bal = currentStack.getGameStats().getBalance();
		return (bal > 0 ? "+" : "") + MessageUtils.formatMoney(bal, settings);
	}
	
	/** Get the player's current committed value for a given round
	 * 
	 * @param gameState
	 * @return
	 */
	@JsonIgnore
	public float getCommit(TexasGameState gameState) {
		return getCurrentStack().getCommitedPerRound().getOrDefault(gameState, 0f);
	}
	
	/** Perform a deep copy of the Player, their current Stack and State
	 * 
	 */
	@Override
	public Player clone() {
		final Player ret = new Player(getPlayerId(), getPlayerHandle());
		ret.pastCards = cards.stream()
				.map(Card::getCode)
				.collect(Collectors.toList())
				.toArray(new String[0]);
		
		ret.currentStack = currentStack;
		ret.seatingPos = seatingPos;
		ret.state = state;
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("playerId=");
		builder.append(getPlayerId());
		builder.append(", handle=");
		builder.append(getPlayerHandle());
		builder.append(", state=");
		builder.append(state);
		builder.append(", rankedHand=");
		builder.append(rankedHand);
		return builder.toString();
	}
}
