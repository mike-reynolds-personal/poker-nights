package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class ThreeOfAKindTest extends AbstractHandEvalTest {

	ThreeOfAKind tok = new ThreeOfAKind();

	@Test
	public void testHighestOfTwoTripsWins() {
		List<Card> table = mk("3H", "6H", "TH", "KS", "AC");

		List<Card> p1 = mk("AH", "AD");
		List<Card> p2 = mk("KD", "KC");

		final int win = assertAvsB(tok, table, p1, p2, PokerHand.THREE_KIND);
		// Ace's vs Kings
		assertEquals(1, win);
	}
	
	@Test
	public void testHighestKickerInHandBreakTie() {
		List<Card> table = mk("4H", "4S", "9S", "7H", "5C");

		List<Card> p1 = mk("4D", "QC");
		List<Card> p2 = mk("4C", "JD");

		final int win = assertAvsB(tok, table, p1, p2, PokerHand.THREE_KIND);
		// 4s - 8 vs 4s-J
		assertEquals(1, win);
	}
	
	@Test
	public void firstKickerOnTable() {
		List<Card> table = mk("5H", "9H", "5S", "5C", "7D");

		List<Card> p1 = mk("3C", "8D");
		List<Card> p2 = mk("3H", "2C");

		final int win = assertAvsB(tok, table, p1, p2, PokerHand.THREE_KIND);

		assertEquals(1, win);
	}
	
	@Test
	public void testHighestKickerOnTableEqualsSplit() {
		List<Card> table = mk("4H", "4S", "9S", "KH", "5C");

		List<Card> p1 = mk("4D", "QC");
		List<Card> p2 = mk("4C", "JD");

		final int win = assertAvsB(tok, table, p1, p2, PokerHand.THREE_KIND);
		// 4s - 8 vs 4s-J
		assertEquals(1, win);
	}
}
