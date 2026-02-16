package com.langleydata.homepoker.game.players;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.langleydata.homepoker.game.GameStats;
import com.langleydata.homepoker.game.PokerMathUtils;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;

/** The set of chips that a Player has both on the table and in front of them
 * 
 * @author reynolds_mj
 *
 */
public class PlayerStack {
	public enum RebuyState {
		SUCCESS,
		NON_ZERO_STACK,
		NO_FUNDS
	}
	private transient GameStats stats = new GameStats();
	/** What they currently have in the current game */
	private float onTable = 0;
	/** The chips the user can play with */
	private float stack = 0;

	/** Money lodged that can be used for buy-in's */
	private float currentWallet = 0;
	/** For each round of the game, what was the user's total bet */
	private Map<TexasGameState, Float> commitedPerRound = new HashMap<>();
	/** Waiting on fund allocation */
	private transient boolean isWOFA = false, isHostControlled = false;

	/** Initialise a new PlayerStats object and transfer the deposit value to the wallet
	 * 
	 * @param The amount to deposit to the players wallet
	 * @param hostControlledWallet Does the host controlled the wallet
	 */
	public void initialise(final float deposit, final boolean hostControlledWallet) {
		if (!hostControlledWallet) {
			this.stats.setInitialWallet(deposit);
			this.currentWallet = deposit;
		}
		this.isHostControlled = hostControlledWallet;
		this.isWOFA = hostControlledWallet;
		
		// Ensure these values are not set by the incoming message
		this.stack = 0;
		this.onTable = 0;
	}

	/** Add money in to a single round of the current game - 
	 * i.e. place money on the table
	 * 
	 * @param value The value to add in to the game
	 * @return True if we did, false if you don't have enough!
	 */
	public boolean addToTable(TexasGameState round, float value) {
		
		/* Previously we were checking if the passed
		 * value was 0 and failing it, but this has issues when a 
		 * player sits out mid-round. The previous check was when a user
		 * was going all-in and it wasn't being added as bet. Stopping a 0
		 * bet stopped the action moving to the next player */
		final float rStack = getStack();
		if (PokerMathUtils.floatEqualsZero(rStack)) {
			return false;
		}
		
		final float rValue = PokerMathUtils.rd(value);
		
		Float cThisRound = commitedPerRound.getOrDefault(round, 0f);
		if (rStack >= rValue) {
			cThisRound +=value;
			commitedPerRound.put(round, cThisRound);
			stack -= value;
		} else if (rStack < rValue && rStack > 0) {
			// Commit whole stack (all-in)
			cThisRound += rStack;
			commitedPerRound.put(round, cThisRound);
			stack = 0;
		}
		onTable = cThisRound;
		return true;
	}
	
	/** Return the amount that is on the table and set the table 
	 * amount to 0
	 * 
	 * @return The amount that was on the table
	 */
	public double collectBets() {
		double ret = onTable;
		onTable = 0f;
		stats.calcBalance(currentWallet + stack);
		if ((currentWallet + stack) == 0 && isHostControlled) {
			isWOFA = true;
		}
		return ret;
	}

	/** Get the current stats
	 * 
	 * @return
	 */
	@JsonIgnore
	public GameStats getGameStats() {
		return this.stats;
	}

	/** Transfer the provided amount from the onTable amount
	 * back to the player's stack and adjust their commitment value
	 * 
	 * @param toRefund The amount to transfer
	 * @return The new onTable amount
	 */
	double reverseBet(final float toRefund, final TexasGameState gameState) {
		stack +=toRefund;
		if (PokerMathUtils.rd(onTable) >= PokerMathUtils.rd(toRefund)) {
			onTable-=toRefund;
			commitedPerRound.put(gameState, onTable);
		}
		return onTable;
	}

	/** Get the total bet throughout the last round, regardless of which 
	 * betting round it was
	 * 
	 * @return
	 */
	@JsonIgnore
	public float getTotalBetInRound() {
		return (float) commitedPerRound.values().stream()
			.mapToDouble(Float::floatValue)
			.sum();
	}
	
	/**
	 * @return the isWOFA
	 */
	public boolean isWOFA() {
		return isWOFA;
	}

	/**
	 * @return the commitedPerRound
	 */
	@JsonIgnore
	public Map<TexasGameState, Float> getCommitedPerRound() {
		return commitedPerRound;
	}

	/** How much is the current bet on table
	 * 
	 * @return the onTable
	 */
	public float getOnTable() {
		return PokerMathUtils.rd(onTable);
	}
	
	/** The total amount available to the player on the table
	 * and in their stack, rounded to 2 decimal places
	 * 
	 * @return
	 */
	@JsonIgnore
	public float getRoundedTotalStack() {
		return PokerMathUtils.rd(onTable + stack);
	}

	/**
	 * @return the stack
	 */
	@JsonProperty
	public float getStack() {
		return PokerMathUtils.rd(stack);
	}

	/**
	 * @param stack the stack to set
	 */
	@VisibleForTesting
	@JsonIgnore
	public void setStack(float stack) {
		this.stack = stack;
	}
	
	/** What the user has lodged to play with
	 * Not visible to other players
	 * @return
	 */
	@JsonIgnore
	public float getWallet() {
		return PokerMathUtils.rd(currentWallet);
	}

	/** Transfer money directly to players stack. This gets called for
	 * each pot and/ or round that has money in it.
	 * 
	 * @param winAmount The amount to transfer
	 * @return The new stack value
	 */
	public float transferWinAmount(float winAmount) {
		if (winAmount > 0) {
			stack += winAmount;
			getGameStats().calcBalance(stack + currentWallet);
		}
		
		return stack;
	}

	/** Reduce a player's stack by the provided amount, if they have enough
	 * to cover it.
	 * 
	 * @param reduceBy The amount to reduce by
	 * @return The new stack value
	 */
	public float reduceStack(float reduceBy) {
		if (stack > reduceBy) {
			stack -= PokerMathUtils.rd(reduceBy);
			getGameStats().calcBalance(stack + currentWallet);
		}
		
		return stack;
	}
	
	/** Assign a wallet value to the player's stack, when a host-controlled-wallet game
	 * 
	 * @param value The value being assigned for the initial wallet
	 * @param buyInAmount The game defined buy-in
	 * @return True if isWOFA and the buy-in works, otherwise false
	 */
	@JsonIgnore
	public boolean assignWallet(final float value, final float buyInAmount) {
		if (isWOFA) {
			this.stats.setInitialWallet(value);
			this.currentWallet = value;
			if (reBuy(buyInAmount, 0f) == RebuyState.SUCCESS) {
				isWOFA = false;
				stats.setRebuys(0);
				return true;
			}
		}
		return false;
	}
	
	/** Re-buy in to the game for the buyInAmount.
	 * Players wallet deducted by the same amount. The player must have 
	 * less or equal to the minPermissible amount in their stack
	 * 
	 * @param buyInAmount The required buy-in
	 * @param minPermissible What is the minimum permissible amount in the player's stack?
	 * 
	 * @return NON_ZERO_STACK, NO_FUNDS or SUCCESS
	 */
	public RebuyState reBuy(float buyInAmount, float minPermissible) {
		
		if (getRoundedTotalStack() > PokerMathUtils.rd(minPermissible)) {
			return RebuyState.NON_ZERO_STACK;
		}
		
		// Alter the amounts
		if (PokerMathUtils.rd(currentWallet) >= buyInAmount) {
			if (buyInAmount > 0) {
				stats.addRebuy(currentWallet);
			}
			stack += buyInAmount;
			currentWallet -= buyInAmount;
			stats.calcBalance(stack + currentWallet);
			return RebuyState.SUCCESS;
		}
		return RebuyState.NO_FUNDS;
	}
	
	/** 'Cash-out' the player, transferring all money from 
	 * their stack back to their wallet
	 * 
	 * @return False if the player has more than 0 on the table (in-round)
	 */
	boolean cashOut() {
		if (onTable > 0) {
			return false;
		}
		currentWallet += stack;
		stats.calcBalance(currentWallet);
		stack = 0;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerStack [onTable=");
		builder.append(onTable);
		builder.append(", stack=");
		builder.append(stack);
		builder.append(", wallet=");
		builder.append(currentWallet);
		builder.append("]");
		return builder.toString();
	}
	
}
