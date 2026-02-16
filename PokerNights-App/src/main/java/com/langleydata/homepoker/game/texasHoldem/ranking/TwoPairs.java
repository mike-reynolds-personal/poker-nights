package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.List;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Check for two pairs of cards.
 * <p>This can happen on the table (out of 5 community)
 * therefore we need to get the single highest card in the player's hand
 * to determine a tie (2 + 2 + 1). If a players match each card in their
 * hand to 1 each on the table, both of these need to be excluded as 
 * kicker's
 *
 */
public class TwoPairs implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {

		final List<int[]> duplicates = preCalculated.calcDuplicates();
		if (EvaluatorUtils.duplicateCount(duplicates) != 2 || EvaluatorUtils.duplicateCount(duplicates, 1) !=2) {
			return null;
		}
		
		preCalculated.setRankName(PokerHand.TWO_PAIR);
		return preCalculated;
	}

	@Override
	public int getRank() {
		return 2;
	}
	
}
