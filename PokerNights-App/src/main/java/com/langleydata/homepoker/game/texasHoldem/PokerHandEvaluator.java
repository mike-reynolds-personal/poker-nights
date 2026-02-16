package com.langleydata.homepoker.game.texasHoldem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePot;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;
import com.langleydata.homepoker.game.texasHoldem.ranking.EvaluatorUtils;
import com.langleydata.homepoker.game.texasHoldem.ranking.Flush;
import com.langleydata.homepoker.game.texasHoldem.ranking.FourOfAKind;
import com.langleydata.homepoker.game.texasHoldem.ranking.FullHouse;
import com.langleydata.homepoker.game.texasHoldem.ranking.HighCard;
import com.langleydata.homepoker.game.texasHoldem.ranking.OnePair;
import com.langleydata.homepoker.game.texasHoldem.ranking.RankEvaluator;
import com.langleydata.homepoker.game.texasHoldem.ranking.RoyalFlush;
import com.langleydata.homepoker.game.texasHoldem.ranking.Straight;
import com.langleydata.homepoker.game.texasHoldem.ranking.StraightFlush;
import com.langleydata.homepoker.game.texasHoldem.ranking.ThreeOfAKind;
import com.langleydata.homepoker.game.texasHoldem.ranking.TwoPairs;

/**
 * Evaluator for Texas Hold'em 7 card hands. Would also handle 5 card stud hands.
 * Reference: https://github.com/danielpaz6/Poker-Hand-Evaluator
 * This class not only calculates multiple players hand rankings, but also
 * handles side-pot/ split-pot calculations
 * 
 * @author Mike Reynolds
 */
@Component
public class PokerHandEvaluator {

	private static final List<RankEvaluator> rankNamers = new ArrayList<>();
	final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	
	// Load all of the hand evaluators
	static {
		rankNamers.add(new RoyalFlush());
		rankNamers.add(new StraightFlush());
		rankNamers.add(new FourOfAKind());
		rankNamers.add(new FullHouse());
		rankNamers.add(new Flush());
		rankNamers.add(new Straight());
		rankNamers.add(new ThreeOfAKind());
		rankNamers.add(new TwoPairs());
		rankNamers.add(new OnePair());
		rankNamers.add(new HighCard());
		
		// Ensure they're applied in the correct order (best to worse)
		rankNamers.sort((x, y) -> {
			return y.getRank() - x.getRank();
		});
	}

	/** Get the rank of a players hand
	 * 
	 * @param playerCards
	 * @param cardsOnTable
	 * @return
	 */
	public HandRank getPlayerHandRank(List<Card> playerCards, List<Card> cardsOnTable) {

		if (playerCards.stream().anyMatch(c -> c == null) || cardsOnTable.stream().anyMatch(c -> c == null)) {
			return new HandRank(0, null, null);
		}

		final List<Card> allCards = new ArrayList<>(cardsOnTable);
		allCards.addAll(playerCards);

		final HandRank calculated = EvaluatorUtils.getHandRank(allCards);
		HandRank rankedHand = null;
		for (int i=0; i < rankNamers.size(); i++) {
			if (( rankedHand = rankNamers.get(i).evaluate(calculated)) !=null) {
				return rankedHand;
			}
		}
		
		return null;
	}
	
	/** Calculates the main and side pot winners based on the players hand-rank
	 * 
	 * @param playersInHand All players that have bet in this hand
	 * @param cardsOnTable The 5 community cards that are out
	 * @return A list of the main and side pots
	 */
	public GamePots calculatePotsAndWinners(final Players playersInHand, final List<Card> cardsOnTable) {
		
		if (playersInHand == null || playersInHand.size()==0) {
			return new GamePots();
		}
		
		/* Update all players that have not folded with their hand's rank.
		 * Folded player's can't win, so they have no rank*/
		playersInHand.getPlayersInHand(false).stream()
			.filter(p -> p.getCurrentStack().getTotalBetInRound() > 0)
			.forEach(p -> p.setRankedHand( getPlayerHandRank(p.getCards(), cardsOnTable) ));

		
		/* Calculate all the split pots based on what the users have bet and in which round
		 * (including those that have folded) to create a list of who is a potential winner (contestedBy)*/ 
		final Map<TexasGameState, GamePot> potsPerRound = new HashMap<>();
		for (TexasGameState gs : TexasGameState.values()) {
			playersInHand.forEach(p -> {
				final GamePot pot = potsPerRound.getOrDefault(gs, new GamePot(gs));
				pot.addContestedBy(p);
				potsPerRound.putIfAbsent(gs, pot);
			});
		}
		
		/* For each pot, calculate all split pots and then
		 * get the winning players and divide the pot value between them,
		 * accumulating on their stack (winnings)  */
		final GamePots finalPots = new GamePots();
		for (GamePot item : potsPerRound.values()) {
			if (item.getContestedBy().size()==0) {
				continue;
			}
			final List<SidePot> sidePots = calculateSidePots(item);
			finalPots.addSidePots(sidePots);
			
			// Transfer the winnings for this pot (round)
			for (SidePot pw : sidePots) {
				pw.getPotWinners().forEach(p -> {
					p.getCurrentStack().transferWinAmount( pw.getWinPerPerson() );
				});

			}
		}
		
		return finalPots;
		
	}
	
	/**
	 * Calculate side pots (where players have bet different amounts). Given bets of
	 * = a=1 (w1), b=10 (w16), c=10 (w0) and d=5 (w6) = total: 26 Divides to three pots
	 * of: A = 4, B = 12, C = 10
	 *
	 * @param gamePot 
	 * 
	 * @return A list of side pots, sorted in order of their value 
	 */
	public List<SidePot> calculateSidePots(final GamePot gamePot) {
		
		final TexasGameState state = gamePot.getState();
		final List<Player> inRound = gamePot.getContestedBy();
		final List<SidePot> newSidePots = new ArrayList<>();
		
		// Sort players by the value they bet in this round, excluding zero values
		List<Player> byValue = inRound.stream()
			.sorted(Comparator.comparing(new CompCommit(state), Comparator.naturalOrder()))
			.filter( p-> p.getCurrentStack().getCommitedPerRound().getOrDefault(state, 0f) > 0f)
			.collect(Collectors.toList());
		
		float prevCalc = 0;
		float totalShared = 0;
		// For each player, ordered by bet value, calculate each side pot...
		for (int p=0; p < byValue.size(); p++) {
			SidePot sp = new SidePot();
			
			// Calculate the pot value by taking the lowest bet
			// value and multiplying it by the number of players with
			// higher bet values still in the hand.
			float pVal = getCommit(byValue.get(p), state) - prevCalc;
			float potVal =  pVal * ( gamePot.getPlayersContributing() - p );
			
			if (potVal > 0) {
				// If we're going to share out more than is in the pot, 
				// it's the last pot, so match the total.
				if (potVal + totalShared > gamePot.getPotTotal()) {
					potVal = gamePot.getPotTotal() - totalShared;
				}
				sp.setPotTotal(potVal);
						
				// Get the highest ranked player of all those that
				// we're still evaluating, including the current player
				// and add them first.
				final List<Player> remaining = byValue.subList(p, byValue.size());
				Player maxRank = remaining.stream()
					.sorted((x, y)-> {
						return y.getRankedHand().getRankValue() - x.getRankedHand().getRankValue();
					})
					.findFirst()
					.orElse(byValue.get(p));
				sp.addCompetingWinner(maxRank); // Has to be first for evaluating max rank
				
				// Add the remaining players in case there is a draw
				remaining.forEach(sp::addCompetingWinner);
				newSidePots.add(sp);
			}
			prevCalc += pVal;
			totalShared += potVal;
			
			if (eq(totalShared, gamePot.getPotTotal())) {
				break;
			}
		}

		return newSidePots;

	}
	
	private class CompCommit implements Function<Player, Float> {
		TexasGameState round;
		CompCommit(TexasGameState round) {
			this.round = round;
		}
		@Override
		public Float apply(Player p) {
			return p.getCurrentStack().getCommitedPerRound().getOrDefault(round, 0f) * 100;
		}
		
	}
	/** Test two doubles for equality
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean eq(double a, double b) {
		return Math.round(a * 1000) == Math.round(b * 1000);
	}
	/** Get what a player committed (bet) in a round
	 * 
	 * @param p
	 * @param state
	 * @return
	 */
	private float getCommit(Player p, TexasGameState state) {
		return p.getCurrentStack().getCommitedPerRound().get(state);
	}
}