package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

public class HandRankingTest extends AbstractHandEvalTest {

	
	@Test
	public void testHandValuesAreBetterThanEachOther() {
		
		// Royal Flush
		final List<Card> rf = mk("TS", "JS", "QS", "KS", "AS");
		final HandRank hrRF = eval(new RoyalFlush(), rf);
		
		// Straight Flush
		final List<Card> sf = mk("TS", "JS", "QS", "KS", "9S");
		final HandRank hrSF = eval(new StraightFlush(), sf);
		
		// Four of a Kind
		final List<Card> fc = mk("KS", "KD", "KC", "KH", "AD");
		final HandRank hrFC = eval(new FourOfAKind(), fc);
		
		// Full House
		final List<Card> fh = mk("KS", "KD", "KC", "TD", "TS");
		final HandRank hrFH = eval(new FullHouse(), fh);
		
		// Straight
		final List<Card> st = mk("8C", "9C", "TS", "JD", "QH");
		final HandRank hrST = eval(new Straight(), st);
		
		// Three of a Kind
		final List<Card> tc = mk("TS", "TC", "TD", "5S", "6C");
		final HandRank hrTC = eval(new ThreeOfAKind(), tc);
		
		// Two Pair
		final List<Card> tp = mk("TS", "TC", "KD", "KC", "6C");
		final HandRank hrTP = eval(new TwoPairs(), tp);
		
		// One Pair
		final List<Card> op = mk("AS", "AC", "KD", "TC", "6C");
		final HandRank hrOP = eval(new OnePair(), op);
		
		// High Card
		final List<Card> hc = mk("AS", "TC", "KD", "JC", "6C");
		final HandRank hrHC = eval(new HighCard(), hc);
		
		// Check names
		assertEquals(PokerHand.ROYAL_FLUSH, hrRF.getRankName());
		assertEquals(PokerHand.STRAIGHT_FLUSH, hrSF.getRankName());
		assertEquals(PokerHand.FOUR_KIND, hrFC.getRankName());
		assertEquals(PokerHand.FULL_HOUSE, hrFH.getRankName());
		assertEquals(PokerHand.STRAIGHT, hrST.getRankName());
		assertEquals(PokerHand.THREE_KIND, hrTC.getRankName());
		assertEquals(PokerHand.TWO_PAIR, hrTP.getRankName());
		assertEquals(PokerHand.PAIR, hrOP.getRankName());
		assertEquals(PokerHand.HIGH_CARD, hrHC.getRankName());
		
		// Check rank values
		assertTrue(hrRF.getRankValue() > hrSF.getRankValue());
		assertTrue(hrSF.getRankValue() > hrFC.getRankValue());
		assertTrue(hrFC.getRankValue() > hrFH.getRankValue());
		assertTrue(hrFH.getRankValue() > hrST.getRankValue());
		assertTrue(hrST.getRankValue() > hrTC.getRankValue());
		assertTrue(hrTC.getRankValue() > hrTP.getRankValue());
		assertTrue(hrTP.getRankValue() > hrOP.getRankValue());
		assertTrue(hrOP.getRankValue() > hrHC.getRankValue());
	}
	
	/**
	 * 
	 */
	private HandRank eval(RankEvaluator he, List<Card> cards) {
		return he.evaluate(EvaluatorUtils.getHandRank(cards));
	}
}
