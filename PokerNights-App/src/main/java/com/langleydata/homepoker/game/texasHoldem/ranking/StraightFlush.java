package com.langleydata.homepoker.game.texasHoldem.ranking;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Check for a straight flush
 * A combination of straight and flush, therefore both their
 * sets of rules apply. As the hand has to include all 5 cards 
 * a kicker is not relevant and the rank of the hand is governed
 * by the highest card in the straight. 
 * 
 * @author reynolds_mj
 *
 */
public class StraightFlush implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {
		
		// Evaluate as a straight first
		final HandRank sr = new Straight().evaluate(preCalculated);
		
		if (sr == null) {
			return null;
		}
		
		// Okay, got a straight, is it a flush?
		final HandRank f = new Flush().evaluate(preCalculated);
		if (f==null) {
			return null;
		}

		preCalculated.setRankName(PokerHand.STRAIGHT_FLUSH);
		return preCalculated;
		
	}

	@Override
	public int getRank() {
		return 8;
	}
}
