package com.langleydata.homepoker.game.texasHoldem;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum TexasGameState {
	/** The game round has started, but the cards have not been dealt (pre-blind posting) */
	PRE_DEAL(0),
	/** The round has started and cards have been dealt to the players, waiting on bets */
	POST_DEAL(1),
	/** The flop has been dealt, waiting on bets */
	FLOP(2),
	/** The turn card has been shown, waiting on bets */
	TURN(3),
	/** The river (final card) has been dealt, waiting on final betting round */
	RIVER(4),
	/** The final beting round has finished and the winner determined. Waiting on a new round to start */
	COMPLETE(5);
	
	private final int order;
	
	private TexasGameState(int order) {
		this.order = order;
	}
	
	@JsonIgnore
	public int getOrder() {
		return this.order;
	}
}
