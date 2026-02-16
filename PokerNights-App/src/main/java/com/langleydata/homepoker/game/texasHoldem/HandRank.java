package com.langleydata.homepoker.game.texasHoldem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.deck.Card;

public class HandRank {
	@Transient
	private transient final int rankValue;
	private PokerHand rankName;
	@Transient
	final transient private List<Card> rankCards;
	/** Used to persist the ranked cards */
	private List<String> rankCodes;
	@Transient
	private transient List<int[]> duplicates;
	
	@PersistenceConstructor
	private HandRank() {
		this.rankName = null;
		this.rankValue = 0;
		rankCards = new ArrayList<Card>();
	}
	
	protected HandRank(final HandRank hr) {
		rankValue = hr.rankValue;
		rankName = hr.rankName;
		rankCards = hr.rankCards;
		setRankCodes();
	}
	/**
	 * 
	 * @param rankValue
	 * @param rankName
	 * @param rankCards
	 */
	public HandRank(final int rankValue, final PokerHand rankName, final List<Card> rankCards) {
		this.rankName = rankName;
		this.rankValue = rankValue;
		this.rankCards = rankCards;
		setRankCodes();
	}

	/**
	 * @return the rankName
	 */
	public PokerHand getRankName() {
		return rankName;
	}

	/** Collate the number of duplicate card numbers along with their face value.<p>
	 *  The returned list is sorted descending according to the number of cards in the 'set',
	 *  for example, [3,2] then [2,5] (3 x TWO then 2 x FIVE)
	 * 
	 * @return [Value: Count], for example: [4, 3] : There are 4 cards of the face value '3'.<br>
	 * If the cards are empty then an empty list is returned
	 */
	public List<int[]> calcDuplicates() {
		if (rankCards == null) {
			return Collections.emptyList();
		}
		if (duplicates == null) { 
			duplicates = rankCards.stream()
				  .collect(Collectors.groupingBy(Card::getFace, Collectors.counting()))
				  .entrySet().stream()
				  	.filter(e -> e.getValue() > 1)
					.map(e -> new int[] { e.getValue().intValue(), e.getKey()})
					.sorted((x, y) -> y[0] - x[0])
					.collect(Collectors.toList());
		}
		return duplicates;
	}
	/**
	 * 
	 * @param rankName
	 */
	public void setRankName(PokerHand rankName) {
		this.rankName = rankName;
	}
	
	/**
	 * @return the rankValue
	 */
	public int getRankValue() {
		return rankValue;
	}

	/**
	 * @return the cards
	 */
	@JsonIgnore
	public List<Card> getCards() {
		return rankCards;
	}

	/** Get a list of card codes that make up the rank
	 * 
	 * @return
	 */
	@JsonIgnore
	private void setRankCodes() {
		if (rankCards!=null) {
			rankCodes = rankCards.stream()
				.map(Card::getCode)
				.collect(Collectors.toList());
		} else {
			rankCodes = Collections.emptyList();
		}
		
	}
	/** Perform a deep copy of this handRank
	 * 
	 */
	@Override
	public HandRank clone() {
		return  new HandRank(this.rankValue, this.rankName, new ArrayList<>(this.rankCards));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(rankValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		HandRank other = (HandRank) obj;
		if (Double.doubleToLongBits(rankValue) != Double.doubleToLongBits(other.rankValue)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HandRank\nrankValue=");
		builder.append(rankValue);
		builder.append(",\nrankName=");
		builder.append(rankName);
		builder.append("\n");
		return builder.toString();
	}

}