package com.langleydata.homepoker.game.texasHoldem.ranking;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Check for highest card [0, 100].
 * Simply get the card with the highest face value out of a set of 12 
 * Cards (2 to Ace)<p>
 * Note that this always return a result
 * 
 *
 */
public class HighCard implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {
		
		preCalculated.setRankName(PokerHand.HIGH_CARD);
		return preCalculated;
	}

	@Override
	public int getRank() {
		return 0;
	}

}
