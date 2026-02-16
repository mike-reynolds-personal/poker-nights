package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class AbstractHandEvalTest {
	
	/** Assert which hands wins, returning 0, 1 or 2
	 * This method takes care of merging card lists and duplicate detection as per PokerHandEvaluator.
	 * 
	 * @param he Evaluator
	 * @param table Table cards
	 * @param p1 P1 cards
	 * @param p2 p2 cards
	 * @param rank Which rank is expected for both
	 * @return 0 = Equal/ Spit, 1 = player 1, 2 = player 2
	 */
	int assertAvsB(RankEvaluator he, List<Card> table, List<Card> p1, List<Card> p2, PokerHand rank) {
		
		final List<Card> mp1 = merge(table, p1);
		final List<Card> mp2 = merge(table, p2);
		final HandRank pre1 = EvaluatorUtils.getHandRank(mp1);
		final HandRank pre2 = EvaluatorUtils.getHandRank(mp2);
		HandRank hr1 = he.evaluate(pre1);
		HandRank hr2 = he.evaluate(pre2);

		assertEquals(rank, hr1.getRankName());
		assertEquals(rank, hr2.getRankName());
		System.out.println("1 vs 2: " + rd(hr1.getRankValue()) + " vs " + rd(hr2.getRankValue()));

		// two pairs on table, B has higher kicker therefore wins
		if (hr1.getRankValue() > hr2.getRankValue()) {
			return 1;
		} else if (hr2.getRankValue() > hr1.getRankValue()) {
			return 2;
		}
		return 0;
	}
	
	private double rd(double val) {
		return Math.round(val * 10000) / 10000d;
	}
	/** Make a set of cards and sort them
	 * 
	 * @param codes
	 * @return
	 */
	public static List<Card> mk(final String... codes) {
		List<Card> cs = Card.makeCards(codes);
		cs.sort((x, y) -> {
			return x.getFace() - y.getFace();
		});
		return cs;
	}

	/** Create a new List with both sets of cards, preserving the originals
	 * 
	 * @param table
	 * @param player
	 * @return
	 */
	public static List<Card> merge(List<Card> table, List<Card> player) {
		List<Card> ret = new ArrayList<>(table);
		ret.addAll(player);
		return ret;
	}
}
