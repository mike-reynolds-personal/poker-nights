package com.langleydata.homepoker.message;

import java.util.List;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.deck.Card;

/** A one-way broadcast message showing one user's cards
 * 
 * @author reynolds_mj
 *
 */
public class ShowCardsMessage extends PokerMessage {
	private List<Card> playerCards;
	private String playerHandle;
	private int seatingPos;
	private boolean winner = false;
	/**
	 * 
	 * @param sessionId
	 * @param playerCards
	 */
	public ShowCardsMessage(final String sessionId) {
		super(MessageTypes.SHOW_CARDS, sessionId);
	}

	public ShowCardsMessage setCards(final List<Card> cards) {
		this.playerCards = cards;
		return this;
	}
	
	public ShowCardsMessage setSeat(final int seat) {
		this.seatingPos = seat;
		return this;
	}
	
	public ShowCardsMessage setHandle(final String pHandle) {
		this.playerHandle = pHandle;
		return this;
	}
	/** Get the cards
	 * 
	 * @return
	 */
	public List<Card> getCards() {
		return this.playerCards;
	}

	/**
	 * @return the playerHandle
	 */
	public String getPlayerHandle() {
		return playerHandle;
	}

	public int getSeatingPos() {
		return seatingPos;
	}

	/**
	 * @return the winner
	 */
	public boolean isWinner() {
		return winner;
	}

	/**
	 * @param winner the winner to set
	 */
	public ShowCardsMessage setWinner(boolean winner) {
		this.winner = winner;
		return this;
	}
	
}
