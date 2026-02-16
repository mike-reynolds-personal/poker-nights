package com.langleydata.homepoker.game.texasHoldem.ranking;

import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** 3rd highest hand, four of a kind
 * Out of 7 cards it's not possible to have two four-kind within the duplicates,
 * therefore don't need to check that. <p>
 * Two players can have four-kind, but the winning hand is decided on the card's face
 * value (as two the same aren't possible), therefore a kicker is not relevant
 */
public class FourOfAKind implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {
		if (EvaluatorUtils.duplicateCount(preCalculated.calcDuplicates()) != 4) {
			return null;
		}
		
		preCalculated.setRankName(PokerHand.FOUR_KIND);
		return preCalculated;
	}

	@Override
	public int getRank() {
		return 7;
	}
}
