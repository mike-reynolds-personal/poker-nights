package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.List;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/**
 * Checks for a Full House, rank:<p>
 * A full house requires all five cards (a set of 3 and a set of 2).
 * A kicker is not required as it uses all 5 available cards, just the rank 
 * governed by the card value of the '3' set.
 * Two players can have a full house, therefore the higher trip wins, but you
 * can't have two sets of duplicates with the same card value (i.e. 3 x 6 and 3 x 6),
 * unless the trips are on the table,
 * but you can have 2 x 4 and 2 x 5
 */
public class FullHouse implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {

		// If we have less than two sets of duplicates, won't be a full hose
		final List<int[]> duplicates = preCalculated.calcDuplicates();
		if (duplicates.size() < 2 || duplicates.get(0)[0] != 3) {
			return null;
		}
		preCalculated.setRankName(PokerHand.FULL_HOUSE);
		return preCalculated;
	}

	@Override
	public int getRank() {
		return 6;
	}
}
