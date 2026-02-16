package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.game.texasHoldem.HandRank;

/** A one-way message storing a single calculated HandRank
 * 
 */
public class HandRankingMessage extends PokerMessage {
	private final HandRank handRank;
	
	/** A new message with an error only
	 * 
	 * @param errorMessage
	 */
	public HandRankingMessage(final String errorMessage) {
		this(null, null);
		setMessage(errorMessage);
	}
	/**
	 * 
	 * @param sessionId
	 * @param hr
	 */
	public HandRankingMessage(final String sessionId, final HandRank hr) {
		super(MessageTypes.HAND_RANKING, sessionId);
		this.handRank = hr;
	}
	
	public HandRank getHandRank() {
		return handRank;
	}

}
