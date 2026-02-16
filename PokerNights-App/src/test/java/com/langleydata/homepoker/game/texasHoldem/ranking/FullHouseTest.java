package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class FullHouseTest extends AbstractHandEvalTest {

	FullHouse fh = new FullHouse();

	@Test
	public void testFullHouseVsFullHouse1() {
		List<Card> table = mk("TS", "TC", "KS", "KH", "AD");

		List<Card> p1 = mk("TD", "KD");
		List<Card> p2 = mk("AS", "AC");

		final int win = assertAvsB(fh, table, p1, p2, PokerHand.FULL_HOUSE);
		// Ts-Ks vs As-Ks
		assertEquals(2, win);
	}

	@Test
	public void testFullHouseThatAreEqualSplit() {
		List<Card> table = mk("TS", "TC", "KS", "KH", "AD");

		List<Card> p1 = mk("TD", "KD");
		List<Card> p2 = mk("TH", "KC");

		final int win = assertAvsB(fh, table, p1, p2, PokerHand.FULL_HOUSE);
		// Ts-Ks vs Ts-Ks
		assertEquals(0, win);
	}
	
	@Test
	public void testFullHouseThatAreEqualSplit2() {
		List<Card> table = mk("TS", "TC", "KS", "KH", "AD");

		List<Card> p1 = mk("TD", "5D");
		List<Card> p2 = mk("TH", "6C");

		final int win = assertAvsB(fh, table, p1, p2, PokerHand.FULL_HOUSE);
		// Ts-Ks vs As-Ks
		assertEquals(0, win);
	}
	
	@Test
	public void testFullHouseTripsOnTable() {
		List<Card> table = mk("KC", "KD", "KS", "3D", "JH");

		List<Card> p1 = mk("TC", "3H");
		List<Card> p2 = mk("TD", "JD");

		final int win = assertAvsB(fh, table, p1, p2, PokerHand.FULL_HOUSE);
		// Player two has the higher pair
		assertEquals(2, win);
	}
}
