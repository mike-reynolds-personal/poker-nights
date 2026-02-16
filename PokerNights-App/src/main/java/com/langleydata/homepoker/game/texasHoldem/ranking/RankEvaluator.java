package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.List;

import com.langleydata.homepoker.game.texasHoldem.HandRank;


/** All evaluators are [now] simply used to determine the correct name of the
 * handRank based on the duplicates/ series of cards within the passed handrank
 * 
 * @author reynolds_mj
 *
 */
public interface RankEvaluator {

	/** Evaluate a poker hand to determine its rank
	 * 
	 * @param preCalculated The pre-calculated handrank
	 * 
	 * @return the calculated hand-rank
	 * @see {@link EvaluatorUtils#getHandRank(List)}
	 */
	HandRank evaluate(final HandRank preCalculated);
	
	int getRank();
}
