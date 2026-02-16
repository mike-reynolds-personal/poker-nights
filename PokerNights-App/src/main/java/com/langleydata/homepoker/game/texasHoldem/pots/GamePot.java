package com.langleydata.homepoker.game.texasHoldem.pots;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.Winner;

/** An individual pot for a given round of the game
 * 
 * @author reynolds_mj
 *
 */
public class GamePot {

	private String name;
	private final TexasGameState state;
	private float potTotal;
	private List<Player> contestedBy = new ArrayList<>();
	private List<Winner> potWinners = new ArrayList<>();
	private HandRank winningRank = new HandRank(0, null, null);
	private int playersContributing = 0;
	/**
	 * 
	 * @param name
	 */
	@PersistenceConstructor
	public GamePot(final TexasGameState state) {
		this.state = state;
	}
	
	/**
	 * 
	 * @param state
	 * @param contestedBy
	 */
	public GamePot(final TexasGameState state, final List<Player> contestedBy) {
		this.state = state;
		contestedBy.forEach(this::addContestedBy);
	}
	/** Clone a pot, giving a new name
	 * 
	 * @param name
	 */
	public GamePot(final String name, GamePot oldPot) {
		this.name = name;
		this.state = null;
		if (oldPot!=null) {
			this.potTotal = oldPot.potTotal;
			this.contestedBy = new ArrayList<>(oldPot.contestedBy);
			this.potWinners = new ArrayList<>(oldPot.potWinners);
			if (oldPot.winningRank!=null) {
				this.winningRank = oldPot.winningRank.clone();
			}
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param toAdd the amount to add to this pot
	 */
	public void addPotValue(float toAdd) {
		this.potTotal += toAdd;
	}

	void setPotTotal(float newVal) {
		this.potTotal = newVal;
	}

	/** Get the amount each player won
	 * 
	 * @return
	 */
	public float getWinPerPerson() {
		return this.potTotal / (potWinners == null ? 1 : potWinners.size());
	}
	
	/**
	 * @return the contestedBy
	 */
	@JsonIgnore
	@Transient
	public List<Player> getContestedBy() {
		return contestedBy;
	}

	/**
	 * @return the winningRank
	 */
	public HandRank getWinningRank() {
		return winningRank;
	}

	/**
	 * @return the state
	 */
	public TexasGameState getState() {
		return state;
	}

	/**
	 * 
	 * @return
	 */
	public float getPotTotal() {
		return this.potTotal;
	}
	
	/** Add a player who's in this pot and update the winning hand to the highest hand.
	 * If the player has folded they don't contest the pot, but their bet value is included
	 * 
	 * @param contestedBy the contestedBy to set
	 */
	public void addContestedBy(Player contestedBy) {
		float cpr = contestedBy.getCurrentStack().getCommitedPerRound().getOrDefault(state, 0f);
		if (cpr > 0) {
			
			playersContributing++;
			this.potTotal += cpr;
			
			if (contestedBy.getState().isFolded()) {
				return;
			}
			
			this.contestedBy.add(contestedBy);
			if (contestedBy.getRankedHand().getRankValue() > winningRank.getRankValue()) {
				winningRank = contestedBy.getRankedHand();
			}
		}
	}

	@JsonIgnore
	public int getPlayersContributing() {
		return playersContributing;
	}
	/**
	 * @return the potWinners
	 */
	public List<Winner> getPotWinners() {
		return potWinners;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("\nName=");
		builder.append(name).append(", ");
		builder.append("pot=");
		builder.append(potTotal).append(", ");
		builder.append("Wining Rank: ").append(winningRank).append("\n");
		builder.append("Winners=");
		builder.append(potWinners != null ? potWinners.subList(0, Math.min(potWinners.size(), maxLen)) : null);
		return builder.toString();
	}

}
