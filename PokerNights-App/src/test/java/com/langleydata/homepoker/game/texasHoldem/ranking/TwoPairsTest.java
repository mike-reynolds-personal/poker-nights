package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class TwoPairsTest extends AbstractHandEvalTest {

	private TwoPairs tp = new TwoPairs();
	// All tests validated independently
	
	@Test
	public void testThreePairsAvailableWithHighestPairWins() {
		List<Card> table = mk("7S", "7H", "8S", "6D", "2S");

		List<Card> p1 = mk("2H","6S");// 2s, 6s & 7s
		List<Card> p2 = mk("TD", "TC");// 7s & Ts

		final int win = assertAvsB(tp, table, p1, p2, PokerHand.TWO_PAIR);
		// Ts and 7s
		assertEquals(2, win);
	}
	
	@Test
	public void testTwoPairOnTableWithHighestKickerWins() {
		List<Card> table = mk("TS", "TD", "8S", "8H", "2D");

		List<Card> p1 = mk("3D", "5D");
		List<Card> p2 = mk("6S", "3S");

		final int win = assertAvsB(tp, table, p1, p2, PokerHand.TWO_PAIR);
		// Highest kicker in player 2s hand (6) 
		assertEquals(2, win);
	}

	@Test
	public void testTwoPairOnTopFiveCardsOnTableSplits() {
		List<Card> table = mk("TS", "TD", "8S", "8H", "AD");

		List<Card> p1 = mk("3D", "5D");
		List<Card> p2 = mk("6S", "3S");

		final int win = assertAvsB(tp, table, p1, p2, PokerHand.TWO_PAIR);
		// Top 5 cards on table, therefore split
		assertEquals(0, win);
	}
	
	@Test
	public void testThreePairsHighestStillWins() {
		List<Card> table = mk("QS", "QD", "TS", "TH", "8C");

		List<Card> p1 = mk("JC", "JD");
		List<Card> p2 = mk("KS", "7S");

		final int win = assertAvsB(tp, table, p1, p2, PokerHand.TWO_PAIR);
		// Pair in hand used instread of pair on table
		assertEquals(1, win);
	}
	
	@Test
	public void testTwoPairWithHighestPairOnTheTable() {
		List<Card> table = mk("2D", "4H", "9D", "9C", "JS");
		
		List<Card> p1 = mk("5C", "5D");
		List<Card> p2 = mk("4S", "QS");

		final int win = assertAvsB(tp, table, p1, p2, PokerHand.TWO_PAIR);
		// 9-5 vs 9-4
		assertEquals(1, win);
	}

	@Test
	public void testTwoPairWithHighestHalfOfPairInHandWins() {
		List<Card> table = mk("8S", "9C", "TS", "KH", "AD");
		// Given A T-King with Ace kicker
		List<Card> p1 = mk("TD", "KD");
		// B has Ace-8 with King kicker
		List<Card> p2 = mk("8D", "AS");

		final int win = assertAvsB(tp, table, p1, p2, PokerHand.TWO_PAIR);
		// T-K vs A-8
		assertEquals(2, win);
	}
	
}
