package com.langleydata.homepoker.game.texasHoldem.pots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.Winner;

/** A class for holding a side pot (split based on player's betting)
 * Note there will be a side-pot per round of play in a game
 * 
 * @author Mike
 *
 */
public class SidePot {

	private String name;
	private float potTotal;
	private Set<Player> potWinners = new HashSet<>();
	private HandRank maxRank;
	
	/**
	 * 
	 */
	public SidePot() {
		// Default constructor
	}
	
	/** Construct a side pot, setting the pot total
	 * 
	 * @param potTotal
	 */
	public SidePot(final float potTotal) {
		this.potTotal = potTotal;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/** Add a player competing for this pot
	 * Note that players must be added in order of hand rank: high to low
	 * as they're filtered based on their hand rank
	 * 
	 * @param p The player
	 */
	public void addCompetingWinner(Player p) {
		if (maxRank == null || p.getRankedHand().getRankValue() >= maxRank.getRankValue()) {
			potWinners.add(p);
			maxRank = p.getRankedHand();
		}
	}

	/** Add all players as winners, without filtering on rank
	 * 
	 * @param players
	 */
	public void addAllWinners(List<Player> players) {
		potWinners.addAll(players);
	}
	/** Ignore ranking and just set a list of winners
	 * 
	 * @param winners
	 */
	void setWinners(List<Player> winners) {
		this.potWinners.addAll(winners);
	}
	
	/** Set the value of this pot
	 * 
	 * @param potTotal
	 */
	public void setPotTotal(float potTotal) {
		this.potTotal = potTotal;
	}

	/** Get how much each winner receives
	 * 
	 * @return
	 */
	@JsonIgnore
	public float getWinPerPerson() {
		return this.potTotal / potWinners.size();
	}
	
	/** The total value of this side-pot
	 * 
	 * @return
	 */
	public float getPotTotal() {
		return potTotal;
	}

	/** Get a copy of the players who have won this pot sorted by name - Could be shared
	 * 
	 * @return All players that share this pot
	 */
	@JsonIgnore
	public List<Player> getPotWinners() {
		if (potWinners.size() > 1) {
			return potWinners.stream()
					.sorted(Comparator.comparing(Player::getPlayerHandle, Comparator.naturalOrder()))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<>(potWinners);
		}
	}

	/** Get the winning hand rank of this pot
	 * 
	 * @return
	 */
	public HandRank getMaxRank() {
		return maxRank;
	}

	/** Get the winners as winners
	 * 
	 * @return
	 */
	public List<Winner> getWinners() {
		return potWinners.stream()
				.map(Winner::new)
				.collect(Collectors.toList());
	}
	
	void setMaxRank(HandRank maxRank) {
		this.maxRank = maxRank;
	}

	@Override
	public String toString() {
		return "SidePot [name=" + name + ", potTotal=" + potTotal + ", potWinners=" + potWinners + ", maxRank="
				+ maxRank + "]";
	}



}
