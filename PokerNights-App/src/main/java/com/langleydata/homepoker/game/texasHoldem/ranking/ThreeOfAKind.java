package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.List;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Check for three of a kind 
 * Three of a kind allows for two floating cards that are not a part of the
 * winning 3, therefore the rank has to include a kicker taken from the players
 * cards, excluding any value used to make the 3-of-kind.
 * Has also got to account for the top 4 cards all being the same.
 */
public class ThreeOfAKind implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {
		final List<int[]> duplicates = preCalculated.calcDuplicates();
		if (EvaluatorUtils.duplicateCount(duplicates) != 3) {
			return null;
		}
		
		preCalculated.setRankName(PokerHand.THREE_KIND);
		return preCalculated;

	}
	
	@Override
	public int getRank() {
		return 3;
	}
}
