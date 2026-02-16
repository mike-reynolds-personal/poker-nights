package com.langleydata.homepoker.message;

import java.util.List;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.deck.Card;

/**
 * A private message that only provides the player with their cards
 * 
 * @author reynolds_mj
 *
 */
public class DealUserMessage extends PokerMessage {
	private final List<Card> hand;

	/**
	 * 
	 * @param playerId
	 */
	public DealUserMessage(final String sessionId, final List<Card> hand) {
		super(MessageTypes.RECEIVE_CARDS, sessionId);
		this.hand = hand;
	}

	/**
	 * @return the hand
	 */
	public List<Card> getHand() {
		return hand;
	}

}
