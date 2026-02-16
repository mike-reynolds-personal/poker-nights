package com.langleydata.homepoker.game.texasHoldem.ranking;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.deck.CardNumber;
import com.langleydata.homepoker.deck.CardSuit;


public class EvaluatorUtilsTest {

	@Test
	public void testRoyalFlushIs7462FromFiveCards() {
		
        for (CardSuit suit : CardSuit.values()) {
        	List<Card> cards = new ArrayList<Card>();
        	cards.add(new Card(CardNumber.ACE, suit));
        	cards.add(new Card(CardNumber.KING, suit));
        	cards.add(new Card(CardNumber.QUEEN, suit));
        	cards.add(new Card(CardNumber.JACK, suit));
        	cards.add(new Card(CardNumber.TEN, suit));
        	
        	assertEquals(7462, EvaluatorUtils.getHandRank(cards).getRankValue());
        }
	}
	
	@Test
	public void testRoyalFlushIs7462FromSevenCards() {
		
        for (CardSuit suit : CardSuit.values()) {
        	List<Card> cards = new ArrayList<Card>();
        	cards.add(new Card(CardNumber.ACE, suit));
        	cards.add(new Card(CardNumber.KING, suit));
        	cards.add(new Card(CardNumber.QUEEN, suit));
        	cards.add(new Card(CardNumber.JACK, suit));
        	cards.add(new Card(CardNumber.TEN, suit));
        	
        	cards.add(new Card(CardNumber.SIX, CardSuit.Diamonds));
        	cards.add(new Card(CardNumber.THREE, CardSuit.Clubs));
        	assertEquals(7462, EvaluatorUtils.getHandRank(cards).getRankValue());
        }
	}
	
	@Test
	public void testPairTwosFromFiveCards() {
		
        	List<Card> cards = new ArrayList<Card>();
        	cards.add(new Card(CardNumber.TWO, CardSuit.Hearts));
        	cards.add(new Card(CardNumber.TWO, CardSuit.Diamonds));
        	cards.add(new Card(CardNumber.THREE, CardSuit.Clubs));
        	cards.add(new Card(CardNumber.FOUR, CardSuit.Spades));
        	cards.add(new Card(CardNumber.FIVE, CardSuit.Spades));
        	
        	assertEquals(1278, EvaluatorUtils.getHandRank(cards).getRankValue());
	}
	
	@Test
	public void testSevenHighIs1FromFiveCards() {
		
        	List<Card> cards = new ArrayList<Card>();
        	cards.add(new Card(CardNumber.TWO, CardSuit.Hearts));
        	cards.add(new Card(CardNumber.THREE, CardSuit.Diamonds));
        	cards.add(new Card(CardNumber.FOUR, CardSuit.Clubs));
        	cards.add(new Card(CardNumber.FIVE, CardSuit.Spades));
        	cards.add(new Card(CardNumber.SEVEN, CardSuit.Hearts));
        	
        	assertEquals(1, EvaluatorUtils.getHandRank(cards).getRankValue());
	}
	
	@Test
	public void testEightHighIs13FromSixCards() {
		
    	List<Card> cards = new ArrayList<Card>();
    	cards.add(new Card(CardNumber.TWO, CardSuit.Hearts));
    	cards.add(new Card(CardNumber.THREE, CardSuit.Diamonds));
    	cards.add(new Card(CardNumber.FOUR, CardSuit.Clubs));
    	cards.add(new Card(CardNumber.FIVE, CardSuit.Spades));

    	cards.add(new Card(CardNumber.SEVEN, CardSuit.Hearts));
    	cards.add(new Card(CardNumber.EIGHT, CardSuit.Hearts));
    	
    	assertEquals(13, EvaluatorUtils.getHandRank(cards).getRankValue());
	}
	
	
	@Test
	public void testSequencesFromOneToSeven() {
		//sequence-count, highest card in run
		
		int[] seq0 = EvaluatorUtils.getSequenceCounts( Card.makeCards("3S", "3D", "6D", "6D", "8D", "8S", "TC"));
		
		Assert.assertEquals(1, seq0[0]);
		Assert.assertEquals(-1, seq0[1]);
		
		int[] seq1 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "4S", "4D", "5D", "8C", "8D", "QH"));
		
		Assert.assertEquals(2, seq1[0]);
		Assert.assertEquals(5, seq1[1]);
		
		int[] seq2 = EvaluatorUtils.getSequenceCounts( Card.makeCards("AS", "3D", "4D", "5D", "8D", "8S", "9C"));
		
		Assert.assertEquals(3, seq2[0]);
		Assert.assertEquals(5, seq2[1]);
		
		int[] seq3 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "5D", "8D", "8S", "9C"));
		
		Assert.assertEquals(4, seq3[0]);
		Assert.assertEquals(5, seq3[1]);
		
		int[] seq4 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "5D", "6D", "8S", "9C"));
		
		Assert.assertEquals(5, seq4[0]);
		Assert.assertEquals(6, seq4[1]);
		
		int[] seq5 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "5D", "6D", "7S", "9C"));
		
		Assert.assertEquals(6, seq5[0]);
		Assert.assertEquals(7, seq5[1]);
		
		int[] seq6 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "5D", "6D", "7S", "8C"));
		
		Assert.assertEquals(7, seq6[0]);
		Assert.assertEquals(8, seq6[1]);
	}

	@Test
	public void testRunsGetHighestCardInStraight() {
		// Don't care about anything below a run of 5, and a 7 is maximum
		
		int[] seq0 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "5D", "6D", "8S", "TC"));
		
		Assert.assertEquals(5, seq0[0]);
		Assert.assertEquals(6, seq0[1]);
		
		int[] seq1 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3S", "4D", "5D", "6C", "7D", "QH"));
		
		Assert.assertEquals(6, seq1[0]);
		Assert.assertEquals(7, seq1[1]);
		
		int[] seq2 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "5D", "6D", "7S", "8C"));
		
		Assert.assertEquals(7, seq2[0]);
		Assert.assertEquals(8, seq2[1]);
		
	}
	
	@Test
	public void testBrokenRuns() {
		int[] seq0 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "8D", "9D", "TS", "JC"));
		
		Assert.assertEquals(4, seq0[0]);
		Assert.assertEquals(11, seq0[1]);
		// 7, 8, 9, T, J, Q
		int[] seq1 = EvaluatorUtils.getSequenceCounts( Card.makeCards("7S", "8S", "TC", "JD", "QH", "9D", "5D"));
		
		Assert.assertEquals(6, seq1[0]);
		Assert.assertEquals(12, seq1[1]);
		
		int[] seq2 = EvaluatorUtils.getSequenceCounts( Card.makeCards("8C", "3C", "7D", "7S", "2S", "3D", "3S"));
		
		Assert.assertEquals(2, seq2[0]);
		Assert.assertEquals(8, seq2[1]);
	}
	
	@Test
	public void testNoRunsAtAll() {
		int[] seq0 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "4D", "6D", "8D", "TD", "TS", "QC"));
		
		Assert.assertEquals(1, seq0[0]);
		Assert.assertEquals(-1, seq0[1]);
		
		int[] seq1 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "2D", "6D", "6D", "6D", "6S"));
		
		Assert.assertEquals(1, seq1[0]);
		Assert.assertEquals(-1, seq1[1]);
	}
	
	@Test
	public void testLowAceStraight() {
		int[] seq0 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "AD", "9D", "TS", "JC"));
		
		Assert.assertEquals(4, seq0[0]);
		Assert.assertEquals(4, seq0[1]);
		
		int[] seq1 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4D", "AD", "QD", "KS", "TC"));
		
		Assert.assertEquals(4, seq1[0]);
		Assert.assertEquals(4, seq1[1]);
		
		// Ace could be low, but T, J, Q, K, A is higher as there are more in sequence
		int[] seq3 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4C", "TD", "JD", "QD", "KS", "AC"));
		
		Assert.assertEquals(5, seq3[0]);
		Assert.assertEquals(14, seq3[1]);
		
		// Ace could be low, but J, Q, K, A is higher, even though both runs are 4
		int[] seq4 = EvaluatorUtils.getSequenceCounts( Card.makeCards("2S", "3D", "4C", "8D", "JD", "QD", "KS", "AC"));
		
		Assert.assertEquals(4, seq4[0]);
		Assert.assertEquals(14, seq4[1]);
	}
}
