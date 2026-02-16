package com.langleydata.homepoker.game.texasHoldem.ranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.CombinatoricsUtils;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.deck.CardNumber;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;

/** Various utilities for arranging and classifying a set of {@link Card}s,
 * Predominantly used for calculating a {@link HandRank} for standard poker hands.
 * 
 * @author reynolds_mj
 *
 */
public class EvaluatorUtils {

    /**
     * Evaluates the given hand and returns its value as a HandRank
     * Based on Kevin Suffecool's 5-card hand evaluator and with Paul Senzee's pre-computed hash.
     * 
     * @param allCards 5 or more cards to evaluate. This always uses 5 cards to calculate the hand, but
     * each combination is tested with the highest one being returned. For example, a 7 card list will have
     * all 21 combinations evaluated.
     * @return the value of the hand as an integer between 1 and 7462. The <b>HIGHER</b> the value the better
     */
	public static HandRank getHandRank(final List<Card> allCards) {
		
		if (allCards.stream().anyMatch(c -> c == null)) {
			return new HandRank(0, PokerHand.NONE, null);
		}
		
		/* The evaluator used only assess 5 cards, so get all combinations of the input cards
		 * and evaluate each until we have the highest rank
		 */
		int bestRank = Integer.MAX_VALUE;
		List<Card> maxCombo = null;
		
		Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(allCards.size(), 5);
	    while (iterator.hasNext()) {
	        final int[] combination = iterator.next();
	        
	        // Build the combination
	        List<Card> toEval = new ArrayList<>();
	        for (int idx : combination) {
	        	toEval.add( allCards.get(idx) );
	        }
	        
	        // Eval the cards
	        final int result = evaluate(toEval);
	        if (result < bestRank) {
	        	bestRank = result;
	        	maxCombo = toEval;
	        }
	    }
	    
		// Sort the cards for better viewing and analysis
	    maxCombo.sort((x, y) -> {
			return x.getFace() - y.getFace();
		});
	    
	    // Ivert the ranking so highest value is best
	    return new HandRank( 7463 - bestRank, PokerHand.NONE, maxCombo);
	}

    /**
     * Evaluates the given hand and returns its value as an integer.
     * Based on Kevin Suffecool's 5-card hand evaluator and with Paul Senzee's pre-computed hash.
     * 
     * @param cards a hand of cards to evaluate - Must be exactly 5 cards
     * @return the value of the hand as an integer between 1 and 7462. The <b>LOWER</b> the value the better
     */
    static int evaluate(List<Card> cards) {
        // Only 5-card hands are supported
        if (cards == null || cards.size() != 5) {
            throw new IllegalArgumentException("Exactly 5 cards are required.");
        }

        // Binary representations of each card
        final int c1 = cards.get(0).getValue();
        final int c2 = cards.get(1).getValue();
        final int c3 = cards.get(2).getValue();
        final int c4 = cards.get(3).getValue();
        final int c5 = cards.get(4).getValue();

        // No duplicate cards allowed
        if (hasDuplicates(new int[]{c1, c2, c3, c4, c5})) {
            throw new IllegalArgumentException("Illegal hand.");
        }

        // Calculate index in the flushes/unique table
        final int index = (c1 | c2 | c3 | c4 | c5) >> 16;

        // Flushes, including straight flushes
        if ((c1 & c2 & c3 & c4 & c5 & 0xF000) != 0) {
            return Tables.Flushes.TABLE[index];
        }

        // Straight and high card hands
        final int value = Tables.Unique.TABLE[index];
        if (value != 0) {
            return value;
        }

        // Remaining cards
        final int product = (c1 & 0xFF) * (c2 & 0xFF) * (c3 & 0xFF) * (c4 & 0xFF) * (c5 & 0xFF);
        return Tables.Hash.Values.TABLE[hash(product)];
    }
    
    /**
     * Checks if the given array of values has any duplicates.
     * @param values the values to check
     * @return true if the values contain duplicates, false otherwise
     */
    private static boolean hasDuplicates(int[] values) {
        Arrays.sort(values);
        for (int i = 1; i < values.length; i++) {
            if (values[i] == values[i - 1])
                return true;
        }
        return false;
    }
    /** Get card hash
     * 
     * @param key
     * @return
     */
    private static int hash(int key) {
        key += 0xE91AAA35;
        key ^= key >>> 16;
        key += key << 8;
        key ^= key >>> 4;
        return ((key + (key << 2)) >>> 19) ^ Tables.Hash.Adjust.TABLE[(key >>> 8) & 0x1FF];
    }
    
	/** Collate the number of duplicate card numbers along with their face value.<p>
	 *  The returned list is sorted descending according to the number of cards in the 'set',
	 *  for example, [3,2] then [2,5] (3 x TWO then 2 x FIVE)
	 *  
	 * @param allCards All the cards to examine
	 * @return [Value: Count], for example: [4, 3] : There are 4 cards of the face value '3'.
	 */
	public static List<int[]> collateDuplicates(List<Card> allCards) {
		
		return allCards.stream()
				  .collect(Collectors.groupingBy(Card::getFace, Collectors.counting()))
				  .entrySet().stream()
				  	.filter(e -> e.getValue() > 1)
					.map(e -> new int[] { e.getValue().intValue(), e.getKey()})
					.sorted((x, y) -> y[0] - x[0])
					.collect(Collectors.toList());
	}
	
	/** Get the number of duplicate card values in the passed array
	 * 
	 * @param duplicates The array to check
	 * @return The count of duplicates at the first position, or 0
	 */
	static int duplicateCount(final List<int[]> duplicates) {
		return duplicateCount(duplicates, 0);
	}
	
	/** Get the number of duplicates in the passed array at the given index
	 * 
	 * @param duplicates The array to check
	 * @param index which position to check
	 * 
	 * @return The count of duplicates at the index position, or 0
	 */
	static int duplicateCount(final List<int[]> duplicates, int index) {
		if (duplicates.size() > index) {
			return duplicates.get(index)[0];
		}
		return 0;
	}


	/** Extract the maximum number of sequentially ordered cards.<p>
	 * An ACE is considered high or low, but between two runs of four the one that
	 * completes with an ACE will be returned
	 * 
	 * @param cardList A list of cards
	 * 
	 * @return <code> int[2]{  sequence-count, highest card in run } </code> 
	 */
	static int[] getSequenceCounts(final List<Card> cardList) {
		int seqMaxValue = -1, seqCountMax = 1;
		
		List<Integer> run = cardList.stream()
			.mapToInt(Card::getFace)
			.distinct()
			.sorted()
			.boxed()
			.collect(Collectors.toList());

		final Map<Integer, Integer> sequences = new HashMap<>();
		final boolean lowAce = run.get(run.size()-1) == CardNumber.ACE.getValue() && run.get(0) == 2;

		// if we have a ace-two then increment the counter for the first sequence
		// as 2-3 will be evaluated first
		seqCountMax = lowAce ? 2 : 1;
		
		// Find all the runs > length 1
		for (int i = 0; i < run.size() - 1; i++) {
			// If this number + 1 (value) equals the next number in the sequence...
			if (run.get(i) + 1 == run.get(i + 1)) {
				seqCountMax++;
				seqMaxValue = run.get(i) + 1;
			} else if (seqCountMax > 1) {
				// The run is broken, so store result and reset the counter
				sequences.put(seqCountMax, seqMaxValue);
				seqCountMax = 1;
			}
		}
		// store the last sequence
		if (seqCountMax > 1) {
			sequences.put(seqCountMax, seqMaxValue);
		}

		// If there's more than 1 sequence, get the longest sequence and it's max value
		if (sequences.size() > 0) {
			int maxRun = sequences.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
			seqCountMax = maxRun;
			seqMaxValue = sequences.getOrDefault(seqCountMax, -1);
		}

		return new int[] { seqCountMax, seqMaxValue };
	}
}
