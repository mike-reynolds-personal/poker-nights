package com.langleydata.homepoker.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.Players;

/** Represents a single standard deck of 52 playing cards with for suits. 
 * <p>Note that the deck is pre-populated with a <b>sequential</b> deck of cards,
 * and should therefore {@link #shuffle()} should be called when starting a game.
 * 
 * @author Mike Reynolds
 *
 */
public class Deck {
	
	public static final int CARDS_IN_DECK = 52;
	private final List<Card> currentDeck = getSequentialDeck();
	private final Random seedGenerator = new Random();
	private int lastCard = 0, numShuffles = 0;
	private long lastSeed = -1L;
	
	/**
	 * Randomly shuffle the <i>current</i> deck
	 * 
	 */
	@JsonIgnore
	public void shuffle() {
		lastSeed = seedGenerator.nextLong();
		lastCard = 0; // When dealing, restart from the 'front' of the deck

		// See comments for the method - Fisher-Yates unbiased reverse shuffle
		Collections.shuffle(currentDeck, new Random(lastSeed));
		numShuffles++;
	}

	/** Get the last seed value used in the random shuffle.
	 * 
	 * @return The long value of the last shuffle
	 */
	public long getLastSeed() {
		return lastSeed;
	}
	
	/** Set a seed to use in the next shuffle.<p>
	 * Note: To replay a hand, the seed has to be set before
	 * the first shuffle, and then this deck is set on the AbstractCardGame,
	 * before the startNextRound method is called
	 * 
	 * @param seed
	 */
	void setSeed(final long seed) {
		this.lastSeed = seed;
	}
	/** Get the number of shuffles that have occurred
	 * 
	 * @return
	 */
	public int getNumShuffles() {
		return numShuffles;
	}
	
	/** Deal the provided number of cards to the players, in the order the players are seated 
	 * 
	 * @param players The players to deal to, but only those still in the hand
	 * @param toDeal The number of cards to deal
	 * @param excludeZeroStacks If false, players with a zero stack will be dealt cards
	 */
	public void dealCardsToPlayers(Players players, final int toDeal, final boolean excludeZeroStacks) {

		final List<Player> toDealTo = players.getPlayersInHand(excludeZeroStacks);
		
		for (int i = 0; i < toDeal; i++) {
			toDealTo.forEach( p -> p.addCard( getNextCard() ) );
		}
	}

	/** Draw the next card from the current deck. Restarts at the beginning of the deck
	 * if the next card is greater than #52
	 * 
	 * @return The next sequentially ordered card.
	 */
	@JsonIgnore
	public Card getNextCard() {
		if (lastCard == 52) {
			lastCard = 0;
		}
		Card c = currentDeck.get(lastCard);
		lastCard++;
		return c;
	}


	/** Get the deck of cards in their current order
	 * 
	 * @return
	 */
	List<Card> getCurrentDeck() {
		return this.currentDeck;
	}

	/** Get the cards in order, with suits Clubs, Diamonds, Hearts and Spades
	 * 
	 * @return A sorted deck of 52 cards
	 */
	static List<Card> getSequentialDeck() {
		List<Card> sortedDeck = new ArrayList<>();

		final IntStream suits = IntStream.of(
				CardSuit.Clubs.getValue(), 
				CardSuit.Diamonds.getValue(),
				CardSuit.Hearts.getValue(), 
				CardSuit.Spades.getValue());

		suits.forEach(f -> {
			for (int v = 2; v < 15; v++) {
				Card card = new Card(CardNumber.fromIndex(v), CardSuit.fromIndex(f));
				sortedDeck.add(card);
			}
		});

		return sortedDeck;
	}
}
