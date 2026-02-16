package com.langleydata.homepoker.deck;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.game.texasHoldem.ranking.Tables;

/** A representation of a standard playing card.
 * 
 * @author Mike Reynolds
 * @author https://github.com/jmp/poker-hand-evaluator/blob/master/src/Card.java
 *
 */
public class Card {
	@Transient
	final CardNumber number;
	@Transient
	final CardSuit suit;
	@Transient
	private int value = 0;
	
    // Rank symbols
    private static final String RANKS = "23456789TJQKA";
    private static final String SUITS = "shdc";
    
	/**
	 * 
	 * @param value
	 * @param suit
	 */
	public Card(final CardNumber number, final CardSuit suit) {
		this.number = number;
		this.suit = suit;
		setValue();
	}
	
	/** Make a card from a code such as 5S, TD, QH, AD.
	 * Numbers are 2-9, T, J, Q, K or A
	 * Suits are C, S, D, H
	 * 
	 * @param code The two character code in upper or lower case
	 */
	public Card(final String code) {
		
		if (code.length() == 2) {
			this.number = CardNumber.fromFace(code.substring(0, 1));
			this.suit = CardSuit.fromFace(code.substring(1));
		} else {
			throw new IllegalArgumentException("Invalid card code length, should only be 2 characters: " + code);
		}
		setValue();
	}

	/** Set the binary value of the card for hash-table lookup
	 * 
	 */
	private void setValue() {
		int rank = number.getValue()-2;
		value = (1 << (rank + 16)) | suit.getValue() | (rank << 8) | Tables.PRIMES[rank];
	}

	/**
	 * @return the card's number
	 */
	@JsonIgnore
	public CardNumber getCardNumber() {
		return number;
	}

	/** Make a list of cards from card codes
	 * 
	 * @param playerCards mark these as a player's cards?
	 * @param codes The array of codes
	 * @return
	 */
	public static List<Card> makeCards(final String... codes) {
		List<Card> cs = new ArrayList<>();
		
		for (String code : codes) {
			cs.add( new Card(StringUtils.strip(code)) );
		}
		return cs;
	}
	/**
	 * Get the 'code' of the card, which equates to the image
	 * 
	 * @return
	 */
	public String getCode() {
		return number.getFace() + (suit==null ? "" : suit.name().substring(0, 1).toUpperCase());
	}

	/**
	 * Get the integer value of the card's value
	 * 
	 * @return
	 */
	@JsonIgnore
	public int getFace() {
		return number.getValue();
	}

    /**
     * Returns the value of the card as an integer.
     * The value is represented as the bits <code>xxxAKQJT 98765432 CDHSrrrr xxPPPPPP</code>,
     * where <code>x</code> means unused, <code>AKQJT 98765432</code> are bits turned on/off
     * depending on the rank of the card, <code>CDHS</code> are the bits corresponding to the
     * suit, and <code>PPPPPP</code> is the prime number of the card.
     * 
     * @return the value of the card.
     * 
     */
	@JsonIgnore
	public int getValue() {
		return value;
	}
	
	/**
	 * @return the suit
	 */
	@JsonIgnore
	public CardSuit getSuit() {
		return suit;
	}

	@Override
	public String toString() {
		return getCode();
	}
	
    /**
     * Returns a string representation of the card.
     * For example, the king of spades is "Ks", and the jack of hearts is "Jh".
     * 
     * @return a string representation of the card.
     */
    public String valueToString() {
        char rank = RANKS.charAt(number.getValue()-2);
        char cSuit = SUITS.charAt((int) (Math.log(suit.getValue()) / Math.log(2)) - 12);
        return new String("" + rank + cSuit).toUpperCase();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Card other = (Card) obj;
		if (value != other.value)
			return false;
		return true;
	}
    
    
}