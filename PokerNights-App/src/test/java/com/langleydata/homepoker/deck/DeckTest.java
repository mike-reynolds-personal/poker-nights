package com.langleydata.homepoker.deck;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.Players;


public class DeckTest {

	
	@Test
	public void testGetFullDeck() {
		final List<Card> cards = new Deck().getCurrentDeck();

		assertEquals(52, cards.size());
		
		Map<Integer, Long> groupFace = cards.stream()
				  .collect(Collectors.groupingBy(Card::getFace, Collectors.counting()));
		
		assertEquals(13, CardNumber.values().length);
		for (CardNumber n : CardNumber.values()) {
			assertEquals(4L, groupFace.get(n.getValue()), 0.1);
			
		}

		Map<CardSuit, Long> groupSuit = cards.stream()
				  .collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()));
		assertEquals(13, groupSuit.get(CardSuit.Clubs), 0.1);
		assertEquals(13, groupSuit.get(CardSuit.Diamonds), 0.1);
		assertEquals(13, groupSuit.get(CardSuit.Spades), 0.1);
		assertEquals(13, groupSuit.get(CardSuit.Hearts), 0.1);
		
	}
	
	@Test
	public void testShuffleGetsFullDeck() {
		Deck deck = new Deck();
		deck.shuffle();
		
		assertEquals(52, deck.getCurrentDeck().size());
		
		Map<Integer, Long> groupFace = deck.getCurrentDeck().stream()
				  .collect(Collectors.groupingBy(Card::getFace, Collectors.counting()));
		
		assertEquals(13, CardNumber.values().length);
		for (CardNumber n : CardNumber.values()) {
			assertEquals(4L, groupFace.get(n.getValue()), 0.1);
			
		}

		Map<CardSuit, Long> groupSuit = deck.getCurrentDeck().stream()
				  .collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()));
		assertEquals(13, groupSuit.get(CardSuit.Clubs), 0.1);
		assertEquals(13, groupSuit.get(CardSuit.Diamonds), 0.1);
		assertEquals(13, groupSuit.get(CardSuit.Spades), 0.1);
		assertEquals(13, groupSuit.get(CardSuit.Hearts), 0.1);
		
	}
	
	@Test
	public void testSequentialDeckIs() {
		final List<Card> cards = new Deck().getCurrentDeck();
		
		assertEquals(52, cards.size());
		
		assertEquals("2C", cards.get(0).getCode());
		assertEquals("AC", cards.get(12).getCode());
		assertEquals("2D", cards.get(13).getCode());
		assertEquals("AD", cards.get(25).getCode());
		assertEquals("2H", cards.get(26).getCode());
		assertEquals("AH", cards.get(38).getCode());
		assertEquals("2S", cards.get(39).getCode());
		assertEquals("AS", cards.get(51).getCode());

	}
	/** It is possible that a card can be in the same position as 
	 * the before the shuffle. If this is the case, check the cards
	 * either side of it in the deck to ensure they're not the same.
	 * 
	 * It's also possible that one of the one of the cards (or both)
	 * either side of the target card are also still in the same place, 
	 * but the probability is a lot lower and we can live with that
	 * failing the test - just re-run the test.
	 */
	private void assertDecksNotEqual(final List<Card> prevDeck, final List<Card> newDeck) {
		for (int i=0; i < Deck.CARDS_IN_DECK; i++) {
			if (prevDeck.get(i).equals(newDeck.get(i))) {
				if (i > 1) {
					assertNotEquals(prevDeck.get(i - 1), newDeck.get(i - 1));
				}
				if (i < 51) {
					assertNotEquals(prevDeck.get(i + 1), newDeck.get(i + 1));
				}
			} else {
				/* Otherwise just check the card isn't at the same position
				 * as the last time */
				assertNotEquals(prevDeck.get(i), newDeck.get(i));
			}
		}
	}
	@Test
	public void testReplayHand() {
		final Deck deck = new Deck();
		final List<Card> prevDeck = deck.getCurrentDeck();
		
		// Shuffle the deck first, which stores the random seed used
		deck.shuffle();
		final long lastSeed = deck.getLastSeed();
		assertEquals(1, deck.getNumShuffles());

		// Create a new deck and set the seed the same as the
		// first before shuffling
		final Deck newDeck = new Deck();
		newDeck.setSeed(lastSeed);
		newDeck.shuffle();
		assertEquals(1, newDeck.getNumShuffles());
		
		final List<Card> nextDeck = deck.getCurrentDeck();
		
		for (int i=0; i < Deck.CARDS_IN_DECK; i++) {
			assertEquals(prevDeck.get(i), nextDeck.get(i));
		}
	}
	
	@Test
	public void testTwoNewDecksAreNotTheSame() {
		final Deck deck1 = new Deck();
		deck1.shuffle();
		assertEquals(1, deck1.getNumShuffles());
		
		final Deck deck2 = new Deck();
		deck2.shuffle();
		assertEquals(1, deck2.getNumShuffles());
		
		assertDecksNotEqual(deck1.getCurrentDeck(), deck2.getCurrentDeck());
	}
	
	@Test
	public void testShuffleSameDeck() {
		final Deck deck = new Deck();
		List<Card> prevDeck = new ArrayList<>(deck.getCurrentDeck());
		long lastSeed = deck.getLastSeed();
		
		deck.shuffle();
		
		// Check a new random seed is used
		assertNotEquals(lastSeed, deck.getLastSeed());
		lastSeed = deck.getLastSeed();
		
		List<Card> newDeck = new ArrayList<>(deck.getCurrentDeck());
		
		assertDecksNotEqual(prevDeck, newDeck);
		
		List<Card> prevDeck2 = new ArrayList<>(newDeck);
		
		deck.shuffle();
		
		// Check a new random seed is used
		assertNotEquals(lastSeed, deck.getLastSeed());
		lastSeed = deck.getLastSeed();
		
		newDeck = new ArrayList<>(deck.getCurrentDeck());
		
		assertDecksNotEqual(prevDeck2, newDeck);
		assertDecksNotEqual(prevDeck, prevDeck2);// Also check the next deck against the original

	}
	
	@Test
	public void testDealingTwoCardsToPlayers() {
		Players players = new Players();
		
		Player A = mkPlayer("A", 1, true);
		Player B = mkPlayer("B", 2, true);
		Player C = mkPlayer("C", 3, true);
		Player D = mkPlayer("D", 4, false);
		
		players.add(A);players.add(B);players.add(C);players.add(D);
		
		final Deck deck = new Deck();
		deck.dealCardsToPlayers(players, 2, true);
		
		Mockito.verify(A).addCard(new Card("2C"));
		Mockito.verify(A).addCard(new Card("5C"));
		Mockito.verify(B).addCard(new Card("3C"));
		Mockito.verify(B).addCard(new Card("6C"));
		Mockito.verify(C).addCard(new Card("4C"));
		Mockito.verify(C).addCard(new Card("7C"));
		
		Mockito.verify(D, Mockito.times(0)).addCard(Mockito.any());
	}
	
	@Test
	public void testDrawNextCardDoesInOrder() {
		final Deck deck = new Deck();

		for (int f = 0; f < 13; f++) {
			Card drawn = deck.getNextCard();
			Card compare = new Card(CardNumber.fromIndex(f + 2), CardSuit.Clubs);
			assertEquals(compare, drawn);
		}

		for (int f = 0; f < 13; f++) {
			Card drawn = deck.getNextCard();
			Card compare = new Card(CardNumber.fromIndex(f + 2), CardSuit.Diamonds);
			assertEquals(compare, drawn);
		}
		
		for (int f = 0; f < 13; f++) {
			Card drawn = deck.getNextCard();
			Card compare = new Card(CardNumber.fromIndex(f + 2), CardSuit.Hearts);
			assertEquals(compare, drawn);
		}
		
		for (int f = 0; f < 13; f++) {
			Card drawn = deck.getNextCard();
			Card compare = new Card(CardNumber.fromIndex(f + 2), CardSuit.Spades);
			assertEquals(compare, drawn);
		}
		
		// Deck should now start from beginning again
		for (int f = 0; f < 13; f++) {
			Card drawn = deck.getNextCard();
			Card compare = new Card(CardNumber.fromIndex(f + 2), CardSuit.Clubs);
			assertEquals(compare, drawn);
		}
	}
	
	private Player mkPlayer(String id, int seat, boolean inHand) {
		Player p = Mockito.mock(Player.class);
		Mockito.when(p.getPlayerId()).thenReturn(id);
		Mockito.when(p.isStillInHand()).thenReturn(inHand);
		Mockito.when(p.getSeatingPos()).thenReturn(seat);
		return p;
	}
	
}
