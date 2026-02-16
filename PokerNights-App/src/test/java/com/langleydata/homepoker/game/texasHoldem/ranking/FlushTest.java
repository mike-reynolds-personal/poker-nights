package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class FlushTest extends AbstractHandEvalTest {

	Flush st = new Flush();

	@Test
	public void testHighestOfTwoFlushWins() {
		List<Card> table = mk("3H", "6H", "TH", "KD", "3C");

		List<Card> p1 = mk("AH", "JH");
		List<Card> p2 = mk("KH", "4H");

		final int win = assertAvsB(st, table, p1, p2, PokerHand.FLUSH);
		// 2-6 vs 3-7
		assertEquals(1, win);
	}

}
