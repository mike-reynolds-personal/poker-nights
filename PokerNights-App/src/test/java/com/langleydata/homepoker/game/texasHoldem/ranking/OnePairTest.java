package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class OnePairTest extends AbstractHandEvalTest {

	private OnePair op = new OnePair();
	// All tests validated independently
	
	@Test
	public void testPairAgainstPairWithHighKicker() {
		List<Card> table = mk("QS", "9C", "8S", "KH", "4D");

		List<Card> p1 = mk("TD", "KD");
		List<Card> p2 = mk("AS", "4S");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Kings vs 4s
		assertEquals(1, win);
	}
	
	@Test
	public void testPairOnTableWinLowKickerInHands() {
		List<Card> table = mk("QS", "9C", "8S", "KH", "6S");

		List<Card> p1 = mk("TH", "6D");
		List<Card> p2 = mk("TD", "8C");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Pair 8s vs 6s
		assertEquals(2, win);
	}

	@Test
	public void testHighestPairUsingTableCardsWins() {
		List<Card> table = mk("4C", "5C", "TC", "KD", "7H");

		List<Card> p1 = mk("TH", "AD");
		List<Card> p2 = mk("TD", "9D");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// T-Ace vs T-K (on table)
		assertEquals(1, win);
	}
	
	@Test
	public void testPairHighKickerDoesntWin() {
		List<Card> table = mk("QH", "KH", "JS", "7C", "4D");

		List<Card> p1 = mk("QS", "AD");
		List<Card> p2 = mk("KC", "3H");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Qs-Ace vs Kings
		assertEquals(2, win);
	}
	
	@Test
	public void testPairOnTableWithHighestKickerOnTableSplit() {
		List<Card> table = mk("QH", "QD", "JS", "7C", "4D");

		List<Card> p1 = mk("TH", "2D");
		List<Card> p2 = mk("TD", "5H");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Pair and kicker on table = split
		assertEquals(0, win);
	}
	
	@Test
	public void testPairOnTableWithHighestKickerInHand() {
		List<Card> table = mk("QH", "QD", "6S", "7C", "4D");

		List<Card> p1 = mk("TH", "2D");
		List<Card> p2 = mk("9D", "5H");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Qs-T vs Qs-9
		assertEquals(1, win);
	}
	
	@Test
	public void testPairOnTableWithHighestKickerInHand2() {
		List<Card> table = mk("AS","3D","9S","9H","4D");

		List<Card> p1 = mk("TH", "JS");
		List<Card> p2 = mk("6S", "KH");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// 9s-K vs 9s-J
		assertEquals(2, win);
	}
	
	@Test
	public void testPairAndKickerOnTableButSecondHighestInHand() {
		List<Card> table = mk("KC", "KH", "AC", "3H", "8D");

		List<Card> p1 = mk("4C", "7C");
		List<Card> p2 = mk("6H", "TC");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Kings and Ace high on table, T kicker in hand
		assertEquals(2, win);
	}
	
	@Test
	public void testPairAndAllTopKickersOnTableShouldSplit() {
		List<Card> table = mk("3C", "5D", "7S", "9D", "TC");

		List<Card> p1 = mk("3S", "6S");
		List<Card> p2 = mk("3H", "4H");

		final int win = assertAvsB(op, table, p1, p2, PokerHand.PAIR);
		// Same pair and all higher kickers on table, therefore split
		assertEquals(0, win);
	}
}
