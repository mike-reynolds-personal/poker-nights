package com.langleydata.homepoker.game.texasHoldem.ranking;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/**
 * Check for a Royal flush, highest hand possible 7462 Has to be the same as a
 * Straight-Flush, but finishing on an Ace
 * 
 * @author reynolds_mj
 *
 */
public class RoyalFlush implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {

		if (preCalculated.getRankValue() == 7462) {
			preCalculated.setRankName(PokerHand.ROYAL_FLUSH);
			return preCalculated;
		}

		return null;
	}

	@Override
	public int getRank() {
		return 9;
	}
}
