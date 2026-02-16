package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class StraightTest extends AbstractHandEvalTest {

	Straight st = new Straight();

	@Test
	public void testHighestStraightWins() {
		List<Card> table = mk("3C", "4C", "5C", "KD", "3H");

		List<Card> p1 = mk("2H", "6D");
		List<Card> p2 = mk("6S", "7C");

		final int win = assertAvsB(st, table, p1, p2, PokerHand.STRAIGHT);
		// 2-6 vs 3-7
		assertEquals(2, win);
	}
	
	@Test
	public void testHighestStraightWinsWithLowAce() {
		List<Card> table = mk("3C", "4C", "5C", "KD", "3H");

		List<Card> p1 = mk("2H", "AD");
		List<Card> p2 = mk("6S", "7C");

		final int win = assertAvsB(st, table, p1, p2, PokerHand.STRAIGHT);
		// A-5 vs 3-7
		assertEquals(2, win);
	}
	
	@Test
	public void testHighestAceStraightWins() {
		List<Card> table = mk("8C", "JS", "QC", "4D", "AH");

		List<Card> p1 = mk("TH", "KD");
		List<Card> p2 = mk("9S", "TC");

		final int win = assertAvsB(st, table, p1, p2, PokerHand.STRAIGHT);
		// T-A vs 8-Q
		assertEquals(1, win);
	}
	
	@Test
	public void testRankCardsReturned() {
		List<Card> all = mk("6C", "7S", "8C", "9D", "TH", "JH", "QD");
		final HandRank pre1 = EvaluatorUtils.getHandRank(all);
		final HandRank hr = st.evaluate(pre1);
		assertEquals(5, hr.getCards().size());
		assertEquals(8, hr.getCards().get(0).getFace());
		assertEquals(PokerHand.STRAIGHT, hr.getRankName());
	}
	
	@Test
	public void testRankCardsReturnedLowAce() {
		List<Card> all = mk("6C", "7S", "5C", "AD", "2H", "3S", "4D");
		final HandRank pre1 = EvaluatorUtils.getHandRank(all);
		final HandRank hr = st.evaluate(pre1);
		assertEquals(5, hr.getCards().size());
		assertEquals(3, hr.getCards().get(0).getFace());
		assertEquals(PokerHand.STRAIGHT, hr.getRankName());
	}
	
	@Test
	public void testRanksIgnoreDuplicates() {
		List<Card> all = mk("6C", "7S", "7C", "8D", "9S", "TH", "TS");
		final HandRank pre1 = EvaluatorUtils.getHandRank(all);
		final HandRank hr = st.evaluate(pre1);
		assertEquals(5, hr.getCards().size());
		assertEquals(6, hr.getCards().get(0).getFace());
		assertEquals(10, hr.getCards().get(4).getFace());
		assertEquals(PokerHand.STRAIGHT, hr.getRankName());
	}
	
	@Test
	public void testRankCardsReturnedLowAce2() {
		List<Card> all = mk("9C", "7S", "5C", "AD", "2H", "3S", "4D");
		final HandRank pre1 = EvaluatorUtils.getHandRank(all);
		final HandRank hr = st.evaluate(pre1);
		assertEquals(5, hr.getCards().size());
		assertEquals(PokerHand.STRAIGHT, hr.getRankName());
		assertEquals(2, hr.getCards().get(0).getFace());
		assertEquals(14, hr.getCards().get(4).getFace());
	}
}
