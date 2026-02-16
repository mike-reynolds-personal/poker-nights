package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class FourOfAKindTest extends AbstractHandEvalTest {

	FourOfAKind fc = new FourOfAKind();

	@Test
	public void testHighestFourKindWins() {
		List<Card> table = mk("TS", "TC", "KS", "KH", "AD");

		List<Card> p1 = mk("TD", "TH");
		List<Card> p2 = mk("KC", "KD");

		final int win = assertAvsB(fc, table, p1, p2, PokerHand.FOUR_KIND);
		// Ts vs Ks
		assertEquals(2, win);
	}
}
