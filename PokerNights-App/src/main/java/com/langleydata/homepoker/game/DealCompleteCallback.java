package com.langleydata.homepoker.game;

import java.util.List;

import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.deck.Card;

/** Primarily used for testing so the cards can be changed once the deal has happened
 * 
 */
public interface DealCompleteCallback {

	/** The deal has completed and the result is provided
	 * 
	 * @param result
	 */
	void dealCompleted(List<Card> tableCards, DealResult result);
}
