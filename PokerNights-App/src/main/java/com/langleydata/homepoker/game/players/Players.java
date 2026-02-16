package com.langleydata.homepoker.game.players;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterables;
import com.langleydata.homepoker.game.PokerMathUtils;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.player.PlayerInfo;

/** A set of players bound to a single game
 * 
 * @author Mike Reynolds
 *
 */
public class Players extends HashSet<Player> {
	private static final long serialVersionUID = 3849301378294958247L;
	private static final Lock lock = new ReentrantLock();

	
	@Override
	public boolean add(Player newPlayer) {
		
		// Sit the player at the next available position
		if (newPlayer.getSeatingPos() == -1) {
			newPlayer.setSeatingPos(getNextAvailableSeat());
		} else {
			// Does someone already have this seat? (Shouldn't really happen)
			if (this.stream()
				.anyMatch(p -> p.getSeatingPos() == newPlayer.getSeatingPos()) ) {
				newPlayer.setSeatingPos(getNextAvailableSeat());
			}
		}
		
		return super.add(newPlayer);
	}
	
	/** Get the next seating position available
	 * 
	 * @return
	 */
	private int getNextAvailableSeat() {
		List<Player> bySeat = getInSeatOrder();
		
		// Ensure position 0 is filled
		if (bySeat.size()==0 || bySeat.get(0).getSeatingPos() !=0) {
			return 0;
		}
		
		for (int i=0; i < bySeat.size() -1; i++) {
			if ( (bySeat.get(i).getSeatingPos() + 1) != bySeat.get(i + 1).getSeatingPos()) {
				return bySeat.get(i).getSeatingPos() + 1;
			}
		}
		return bySeat.size();
	}
	
	/**
	 * Collect all the bets, setting them to zero. If a player
	 * has 'overpaid' on a bet, it is refunded to their stack.<p>
	 * This happens when a user has gone all in and another calls the bet, 
	 * but can't actually match it.
	 * 
	 * @return The total amount collected
	 */
	public float collectBets(final TexasGameState gameState) {
		if (!lock.tryLock()) {
			return 0f;
		}
		
		try {
			final List<Double> allBets = this
				.stream()
				.mapToDouble(p -> p.getCommit(gameState))
				.sorted()
				.boxed()
				.collect(Collectors.toList());
			
			if (allBets.size()==0) {
				return 0f;
			}
			
			final double highest = allBets.get(allBets.size()-1);
			double nextHighest = highest;
			if (allBets.size() > 1) {
				nextHighest = allBets.get(allBets.size()-2);
			}
			
			// If the two highest bets are not equal, we have to refund the highest
			// the difference so there's equality. Any subsequent lower values results in a split pot
			if (PokerMathUtils.floatEquals(highest, nextHighest)==false) {
				
				final Player pHigh = this.stream()
					.filter(p -> PokerMathUtils.floatEquals(p.getCommit(gameState), highest))
					.findFirst()
					.orElse(null);
				
				if (pHigh != null) {
					pHigh.getCurrentStack().reverseBet((float) (highest-nextHighest), gameState);
				}
			}
			
			
			return (float) stream()
					.mapToDouble(p -> p.getCurrentStack().collectBets())
					.sum();
		} catch (Exception e) {
			
		} finally {
			lock.unlock();
		}
		return 0f;
	}
	
	/** Update all player's win/loose stats. Any players sitting out are
	 * ignored
	 * 
	 * @param winners All round winners
	 */
	public void setWinners(List<PlayerInfo> winners) {
		this.stream()
			.filter(p -> p.getState().isSittingOut()==false)
			.forEach(p -> p.getCurrentStack().getGameStats().updateStats(winners.contains(p)) );
	}

	/** Get the maximum bet possible, based on what other players
	 * have in their stack and on the table
	 * 
	 * @param player The current player (to exclude from the check)
	 * @return The maximum permissible bet 
	 */
	public float getMaxBetPossible(final PlayerInfo toExclude) {

		return (float) getPlayersInHand(false)
						.stream()
						.filter(p -> !p.equals(toExclude))
						.mapToDouble(p -> p.getCurrentStack().getRoundedTotalStack())
						.filter(v -> v > 0)
						.max()
						.orElse(0);
	}
	/** Get the maximum on the table from any player at the current time
	 * 
	 * @return
	 */
	public float getMaxBetInRound() {
		return (float) getPlayersInHand()
						.stream()
						.mapToDouble(p -> p.getCurrentStack().getOnTable())
						.max()
						.orElse(0);
			
	}
	/** For all players that have zero left to play with, auto-sit them out of the next round.
	 * These players will have to buy back in to continue;
	 */
	public void autoExcludeZeroStacks() {
		this.forEach(p->{
			if (PokerMathUtils.floatEqualsZero(p.getCurrentStack().getStack()) && !p.getState().isSittingOut()) {
				p.getState().toggleSittingOut(TexasGameState.COMPLETE, true);
			}
		});
	}
	
	/**
	 * Get the players in the hand (not folded, not sitting out) but they could have zero cash available).
	 *
	 * 
	 * @return A list of players sorted by seating position
	 */
	@JsonIgnore
	public List<Player> getPlayersInHand() {
		return getPlayersInHand(false);
	}
	
	/**
	 * Get the players in the hand (not folded or sitting out) sorted by their
	 * seating position
	 * @param excludeZeroStacks Should the list of players also exclude those with no cash available?
	 * If false, then still only returns players that are eligible to player (not folded and not sitting out)
	 *
	 * @return A list of players sorted by seating position
	 */
	@JsonIgnore
	public List<Player> getPlayersInHand(final boolean excludeZeroStacks) {
		if (excludeZeroStacks) {
			return this.stream()
					.filter(Player::isStillInHand)
					.sorted(Comparator.comparing(Player::getSeatingPos, Comparator.naturalOrder()))
					.collect(Collectors.toList());
		} else {
			return this.stream()
					.filter(p -> !p.getState().isSittingOut() && !p.getState().isFolded())
					.sorted(Comparator.comparing(Player::getSeatingPos, Comparator.naturalOrder()))
					.collect(Collectors.toList());
		}
	}
	/** Get a player based on the PlayerInfo equals method (id and handle)
	 * 
	 * @param pi
	 * @return
	 */
	public Player getPlayer(final PlayerInfo pi) {
		return this.stream()
				.filter(p -> p.equals(pi))
				.findFirst()
				.orElse(null);
	}
	/**
	 * Get a current player by their persistent player Id
	 * 
	 * @param playerId The player's id
	 * @return The player, or null
	 */
	public Player getPlayerById(final String playerId) {
		if (StringUtils.isBlank(playerId)) {
			return null;
		}
		
		return this.stream()
			.filter(p -> p.getPlayerId().equals(playerId))
			.findFirst()
			.orElse(null);
	}
	
	/** Get a user by their transient session id
	 * 
	 * @param sessionId
	 * @return
	 */
	public Player getPlayerBySessionId(final String sessionId) {
		return this.stream()
				.filter(p -> p.getSessionId().equals(sessionId))
				.findFirst()
				.orElse(null);
	}
	
	/** Get the current game host
	 * 
	 * @return The host or null
	 */
	@JsonIgnore
	public Player getHost() {
		return this.stream()
				.filter( p -> p.getState().isHost() )
				.findFirst()
				.orElse(null);
	}
	/**
	 * Get the current dealer, or the player at the first seating position
	 * 
	 * @return The current dealer or the person in first position, or null if there are no players 
	 */
	@JsonIgnore
	public Player getDealer() {
		final List<Player> ps = this.stream()
				.sorted(Comparator.comparing(Player::getSeatingPos, Comparator.naturalOrder()))
				.collect(Collectors.toList());
		
		// Quick exit for simple cases
		if (ps.size()==0) {
			return null;
		} else if (ps.size()==1) {
			return ps.get(0);
		}
		
		// Get any player with the dealer flag...
		Player dealer = ps.stream()
				.filter(p -> p.getState().isDealer())
				.findFirst()
				.orElse(null);
		
		if (dealer!=null) {
			return dealer;
		} else {
			// only case left is multiple players, but none flagged as dealer yet
			// so get the first in the sorted list
			return ps.get(0);
		}
	}

	/**
	 * Reset all players actionOn state, and put it on the first player to the left
	 * of the dealer.
	 * 
	 * @return True if successful
	 */
	public boolean resetActionPositionForDeal() {
		this.forEach(p -> p.getState().setActionOn(false));
		Player first = getPlayerRelativeTo(getDealer(), 1, true);
		if (first != null) {
			first.getState().setActionOn(true);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Move the playing action from the currentPlayer on to the next based on their
	 * seating order
	 * 
	 * @param currentPlayer The action the current player is on
	 * @return True if it moved
	 */
	public boolean moveActionToNextPlayer(final Player currentPlayer) {
		
		final Player next = getPlayerRelativeTo(currentPlayer, 1, true);
		if (next==null) {
			return false;
		}
		
		currentPlayer.getState().setActionOn(false);
		next.getState().setActionOn(true);
		return true;
	}

	/** Get all players with no rank and a zero stack, sorted by their commitment in the
	 * latest round
	 * 
	 * @return Players with rank = 0
	 */
	@JsonIgnore
	public List<Player> getUnRankedLoosers() {
		return this.stream()
			.filter(p -> PokerMathUtils.floatEqualsZero(p.getCurrentStack().getStack()))
			.filter(p -> p.getCurrentStack().getGameStats().getRank() == 0)
			.sorted(Comparator.comparing(p->p.getCurrentStack().getTotalBetInRound(), Comparator.naturalOrder()))
			.collect(Collectors.toList());
	}
	
	/** Get ranked players in order of their rank, 1 to n
	 * 
	 * @return Players with rank > 0
	 */
	public List<Player> getRankedPlayers() {
		return this.stream()
				.filter(p -> p.getCurrentStack().getGameStats().getRank() > 0)
				.sorted(Comparator.comparing(p->p.getCurrentStack().getTotalBetInRound(), Comparator.naturalOrder()))
				.collect(Collectors.toList());
	}

	/** Get the last player that performed an action
	 * 
	 * @param inHandOnly Only count players still in the round?
	 * @return The player, or Null
	 */
	@JsonIgnore
	public Player getLastPlayerToAction(final boolean inHandOnly) {
		
		final Comparator<Player> comp = Comparator.comparing(Player::getLastActionTime, Comparator.reverseOrder());
		
		if (inHandOnly) {
			return this.stream()
					.filter(p -> !p.getState().isSittingOut() && !p.getState().isFolded())
					.sorted(comp)
					.findFirst()
					.orElse(null);
		} else {
			return this.stream()
					.sorted(comp)
					.findFirst()
					.orElse(null);
		}
	}
	
	/**
	 * Get which player the action is on, or null if not on anyone
	 * 
	 * @return
	 */
	@JsonIgnore
	public Player getActionOn() {
		return this.stream().filter(p -> p.getState().isActionOnMe()).findFirst().orElse(null);
	}
	
	List<Player> getInSeatOrder() {
		return this.stream()
				.sorted(Comparator.comparing(Player::getSeatingPos, Comparator.naturalOrder()))
				.collect(Collectors.toList());
	}
	
	/** Set a new randomised dealer
	 * 
	 * @param onlyInHand Restrict to only those players in the hand?
	 * @return The new dealer
	 */
	public Player setRandomDealer(final boolean onlyInHand) {
		Player currDealer = getDealer();
		
		int dealerSeat = new Random().nextInt(this.size());
		Player randDealer = getPlayerRelativeTo(currDealer, dealerSeat, onlyInHand);
		randDealer.getState().setActionOn(true);
		randDealer.getState().setDealer(true);
		
		currDealer.getState().setActionOn(false);
		currDealer.getState().setDealer(false);
		
		return randDealer;
	}
	/** Get a player relative to another player, based on seating position
	 * 
	 * @param start The position to start at
	 * @param relative positive or negative position
	 * @param inHandOnly Only include players in the current hand?
	 * @return
	 */
	public Player getPlayerRelativeTo(Player start, int relative, boolean inHandOnly) {
		if (start==null) {
			throw new IllegalArgumentException("Cannot have a null starting position");
		}
		if (relative==0) {
			return start;
		}
		List<Player> players = getInSeatOrder();
		if (relative < 0) {
			players.sort(Comparator.comparing(Player::getSeatingPos, Comparator.reverseOrder()));
		}
		List<Player> toCycle = new ArrayList<>();
		
		if (inHandOnly) {
			for (Player p : players) {
				if (p.isStillInHand() || start.equals(p)) {
					toCycle.add(p);
				} else {
					toCycle.add(null);
				}
			
			}
		} else {
			toCycle.addAll(players);
		}

		int cnt = 0, safety = 0;
		Player player = null;
		boolean reachedStart = false;
		Iterator<Player> cycle = Iterables.cycle(toCycle).iterator();
		
		/* Cycle around the list of sorted players in the game, starting the 
		 * counter when we reached the 'start' player. The list of potential players
		 * is already sorted according to the direction of rotation (-/+) therefore 
		 * we only need to increment per position */
		while (cycle.hasNext() && cnt < Math.abs(relative) + 1) {
			player = cycle.next();
			if (reachedStart || (player!=null && player.equals(start))) {
				if (player!=null) {
					cnt++;
				}
				reachedStart = true;
			}
			safety++;
			if (safety >= 30) {
				throw new IllegalArgumentException("Potential infinite loop cycling players");
			}
		}
		return player;
	}

	/** Clone all current players
	 * 
	 */
	@Override
	public List<Player> clone() {
		return 	this.stream()
				.map(Player::clone)
				.collect(Collectors.toList());
	}
}
