package com.langleydata.homepoker.game.texasHoldem.ranking;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/**
 * Check for a straight (run of 5), including ace high or low
 * 
 */
public class Straight implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {

		final int[] seq = EvaluatorUtils.getSequenceCounts(preCalculated.getCards());
		final int seqCountMax = seq[0];
		
		if (seqCountMax < 5 ) {
			return null;
		}
		
		preCalculated.setRankName(PokerHand.STRAIGHT);
		return preCalculated;

	}

	@Override
	public int getRank() {
		return 4;
	}
}
