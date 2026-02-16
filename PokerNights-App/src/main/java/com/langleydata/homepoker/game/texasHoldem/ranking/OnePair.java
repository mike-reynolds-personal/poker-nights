package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.List;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Check for a pair
 */
public class OnePair implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {
		
		// If the highest count isn't two, then its either not a pair (trips) or
		// no pair at all
		final List<int[]> duplicates = preCalculated.calcDuplicates();
		if (EvaluatorUtils.duplicateCount(duplicates) != 2 || duplicates.size() > 1) {
			return null;
		}

		preCalculated.setRankName(PokerHand.PAIR);
		return preCalculated;

	}
	
	@Override
	public int getRank() {
		return 1;
	}
}
