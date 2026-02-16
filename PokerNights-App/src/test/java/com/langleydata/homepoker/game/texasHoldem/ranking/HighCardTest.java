package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class HighCardTest extends AbstractHandEvalTest {

	HighCard hc = new HighCard();

	@Test
	public void testSimpleHighCard() {
		List<Card> cards = mk("2D", "3D", "4D", "5D", "6D", "7D", "8D");
		final HandRank pre1 = EvaluatorUtils.getHandRank(cards);
		HandRank hr = hc.evaluate(pre1);
		assertEquals(PokerHand.HIGH_CARD, hr.getRankName());
		assertTrue(hr.getCards().contains(new Card("8D")));
	}

	@Test
	public void testAceHighCard() {
		List<Card> cards = mk("2D", "3D", "4D", "AD", "6D", "7D", "8D");
		final HandRank pre1 = EvaluatorUtils.getHandRank(cards);
		HandRank hr = hc.evaluate(pre1);
		assertEquals(PokerHand.HIGH_CARD, hr.getRankName());
		assertTrue(hr.getCards().contains(new Card("AD")));
	}

	@Test
	public void testAceHighCardWithDouble() {
		List<Card> cards = mk("QS", "3D", "4D", "QD", "6D", "7D", "8D");
		final HandRank pre1 = EvaluatorUtils.getHandRank(cards);
		HandRank hr = hc.evaluate(pre1);
		assertEquals(PokerHand.HIGH_CARD, hr.getRankName());
		assertTrue(hr.getCards().contains(new Card("QD")));
	}

	@Test
	public void testKickerInHandWins() {
		List<Card> table = mk("4D", "6S", "8C", "9D", "TH");
		List<Card> p1 = mk("QC", "KH");
		List<Card> p2 = mk("5H", "KD");

		final int win = assertAvsB(hc, table, p1, p2, PokerHand.HIGH_CARD);
		// Player 1 has the highest kicker
		assertEquals(1, win);
	}

	@Test
	public void testKickerOnTableMakesSplitPot() {
		List<Card> table = mk("4D", "QS", "8C", "9D", "TH");
		List<Card> p1 = mk("5C", "KH");
		List<Card> p2 = mk("5H", "KD");

		final int win = assertAvsB(hc, table, p1, p2, PokerHand.HIGH_CARD);
		// King in hand and kicker (Q) on table = split
		assertEquals(0, win);
	}

	@Test
	public void testHighCardOnTableAndKickerInHand() {
		List<Card> table = mk("4D", "7S", "8C", "9D", "KH");
		List<Card> p1 = mk("5C", "QH");
		List<Card> p2 = mk("5H", "TD");

		final int win = assertAvsB(hc, table, p1, p2, PokerHand.HIGH_CARD);
		// Player 1 has the highest kicker (Q)
		assertEquals(1, win);
	}

	@Test
	public void testTopTwoOnTablePlayerStillWins() {
		List<Card> table = mk("4D", "7S", "8C", "QD", "KH");
		List<Card> p1 = mk("5C", "TH");
		List<Card> p2 = mk("5H", "9D");

		final int win = assertAvsB(hc, table, p1, p2, PokerHand.HIGH_CARD);
		// Player 1 has the highest kicker (T) with K and Q on table
		assertEquals(1, win);
	}
}
