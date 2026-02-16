package com.langleydata.homepoker.game;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.langleydata.homepoker.player.PlayerStats;

/** Statistics for a Player throughout a single game
 * 
 * @author Mike Reynolds
 *
 */
public class GameStats extends PlayerStats {

	@Transient
	private int lastRound = 0;
	/** What the player bought in with */
	private float initialWallet = 0;
	private int rank = 0;
	
	/** Update the stats
	 * 
	 * @param won If true, the win count is incremented, regardless of the lost flag
	 * @param lost If true, the lost count is incremented, only if won is false
	 */
	@VisibleForTesting
	public void updateStats(boolean won) {
		if (won) {
			this.won++;
		} else {
			this.lost++;
		}
		handsPlayed++;
	}

	/** Increment the number of times the player has voluntarily bet 
	 * per round.
	 * 
	 * @param roundNumber The unique round number
	 */
	public void addVoluntaryBet(final int roundNumber) {
		if (lastRound == roundNumber) {
			return;
		}
		lastRound = roundNumber;
		volBets++;
	}

	/** Set the value in the player's wallet.
	 * If their wallet is not -1 then this is ignored (only buy-in once)
	 *  
	 * @param boughtIn What to deposit in the wallet
	 */
	public void setInitialWallet(float boughtIn) {
		if (initialWallet == 0) {
			this.initialWallet = boughtIn;
		}
	}
	
	@JsonIgnore
	public float getInitialWallet() {
		return this.initialWallet;
	}
	/** Calculate player's balance vs their stack
	 * 
	 * @param cashInGame Current stack amount
	 */
	public void calcBalance(final float cashInGame) {
		balance = cashInGame - initialWallet;
	}
	
	/** Overwrite the current set of stats with another set.
	 * Excludes the balance and wallet values.
	 * 
	 * @param prev Another set of stats
	 */
	public void applyStats(final GameStats prev) {
		this.handsPlayed = prev.handsPlayed;
		this.won = prev.won;
		this.lost = prev.lost;
		this.lastRound = prev.lastRound;
		this.volBets = prev.volBets;
		this.timePlayed = prev.timePlayed;
		this.rank = prev.rank;
	}
	
	
	/**
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * @param rank the rank to set
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}

	/** Set the amount of time played this game
	 * 
	 * @param timePlayed
	 */
	public void setTimePlayed(long timePlayed) {
		this.timePlayed = timePlayed;
	}
	/** Set the number of rebuys. Only used for Host-controlled-wallet
	 * 
	 * @param rebuyNum
	 */
	public void setRebuys(int rebuyNum) {
		rebuys = rebuyNum;
	}
	/**
	 * 
	 * @param totalStack
	 */
	public void addRebuy(final float totalStack) {
		rebuys++;
		calcBalance(totalStack);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerStats [totalWallet=");
		builder.append(initialWallet);
		builder.append(", rank=");
		builder.append(rank);
		builder.append(", balance=");
		builder.append(balance);
		builder.append(", handsPlayed=");
		builder.append(handsPlayed);
		builder.append(", won=");
		builder.append(won);
		builder.append(", lost=");
		builder.append(lost);
		builder.append(", rebuys=");
		builder.append(rebuys);
		builder.append(", lastRound=");
		builder.append(lastRound);
		builder.append(", volBets=");
		builder.append(volBets);
		builder.append("]");
		return builder.toString();
	}

}
