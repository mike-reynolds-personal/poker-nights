package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.deck.CardSuit;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Check for a standard flush
 * A flush doesn't care about duplicates or any kicker, as it has to consist
 * of 5 or more cards
 * 
 */
public class Flush implements RankEvaluator {

	@Override
	public HandRank evaluate(final HandRank preCalculated) {

		// Count up all the cards of the same suit
		final List<Entry<CardSuit, Long>> cSuit = preCalculated.getCards().stream()
				  .collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()))
				  .entrySet().stream()
				  	.filter(e -> e.getValue() >= 5)// 5 or more cards
					.collect(Collectors.toList());
		
		if (cSuit.size() < 1) {
			return null;
		}
		
		preCalculated.setRankName(PokerHand.FLUSH);
		return preCalculated;
	}

	@Override
	public int getRank() {
		return 5;
	}
}
