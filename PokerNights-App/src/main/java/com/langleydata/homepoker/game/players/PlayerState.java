package com.langleydata.homepoker.game.players;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.game.texasHoldem.Blinds;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;

/**
 * The state of the current player's play
 * 
 * @author reynolds_mj
 *
 */
public class PlayerState {
	private enum SIT_NEXT_ROUND {
		SIT_IN, SIT_OUT, NONE
	}
	public static final long ACTION_MARGIN = 2000;
	@Transient
	private long lastActionTime = System.currentTimeMillis();
	private long joinTime = System.currentTimeMillis(), cashOutTime = -1;
	
	private Blinds blindsDue = Blinds.NONE;
	private PlayerActionType lastAction = PlayerActionType.NONE;
	private SIT_NEXT_ROUND sitNextRound = SIT_NEXT_ROUND.NONE;
	
	private boolean wasBigBlind = false, folded = false, sittingOut = false;
	private boolean isHost = false, isDealer = false, isAllIn = false;
	private boolean cashedOut = false, actionOnMe = false;

	private int inactionTimeout = Integer.MAX_VALUE;
	private long nextAutoInaction = -1L;
	
	/** Initialisation steps for the player's state
	 * 
	 * @param gameState The current game state
	 * @param gameSettings The current game's settings
	 */
	public void initialise(final TexasGameState gameState, final GameSettings gameSettings) {
		
		if (gameSettings.isHostControlledWallet()) {
			sitNextRound = SIT_NEXT_ROUND.SIT_OUT;
			sittingOut = true;
		} else if (gameState!=TexasGameState.COMPLETE) {
			sitNextRound = SIT_NEXT_ROUND.SIT_IN;
			sittingOut = true;
		}
		if (gameSettings.getActionTimeout() > 0) {
			this.inactionTimeout = gameSettings.getActionTimeout();
		}
	}
	/**
	 * @return the isDealer
	 */
	public boolean isDealer() {
		return isDealer;
	}

	/**
	 * @param isDealer the isDealer to set
	 */
	public void setDealer(boolean isDealer) {
		this.isDealer = isDealer;
	}

	/**
	 * @return the actionOn
	 */
	public boolean isActionOnMe() {
		return actionOnMe;
	}

	/** Determines which player the action is waiting on
	 * 
	 * @param actionOn the actionOn to set
	 */
	public void setActionOn(boolean actionOn) {
		this.actionOnMe = actionOn;
		if (actionOn) {
			nextAutoInaction = System.currentTimeMillis() + (inactionTimeout * 1000) + ACTION_MARGIN;
		} else {
			nextAutoInaction = -1;
		}
	}

	/** Get the time that this player will be auto-folded for inactions
	 * 
	 * @return The time in milliseconds or -1 if they don't have a time set
	 */
	public long getNextAutoInaction() {
		return this.nextAutoInaction;
	}
	
	/**
	 * @return the lastAction
	 */
	public PlayerActionType getLastAction() {
		return lastAction;
	}

	/**
	 * @param lastAction the lastAction to set
	 */
	public void setLastAction(PlayerActionType lastAction) {
		this.lastAction = lastAction;
		this.lastActionTime = System.currentTimeMillis();
		if (lastAction.isFold()) {
			this.folded = true;
		}
	}
	
	public boolean isHost() {
		return isHost;
	}
	
	/**
	 * @return the lastActionTime
	 */
	long getLastActionTime() {
		return lastActionTime;
	}
	/** Set this player as the game host
	 * 
	 * @param isHost
	 */
	public void setHost(boolean isHost) {
		this.isHost = isHost;
	}
	/**
	 * @return the isAllIn
	 */
	public boolean isAllIn() {
		return isAllIn;
	}

	/**
	 * @param isAllIn the isAllIn to set
	 */
	public void setAllIn(boolean isAllIn) {
		this.isAllIn = isAllIn;
	}

	/**
	 * @return the blindsDue
	 */
	public Blinds getBlindsDue() {
		return blindsDue;
	}

	/**
	 * @param blindsDue the blindsDue to set
	 */
	public void setBlindsDue(Blinds blindsDue) {
		if (this.blindsDue == Blinds.BIG && blindsDue==Blinds.NONE) {
			wasBigBlind = true;
		}
		this.blindsDue = blindsDue;
	}

	/**
	 * @return the wasBigBlind
	 */
	public boolean wasBigBlind() {
		return wasBigBlind;
	}

	/**
	 * @return the folded
	 */
	public boolean isFolded() {
		return folded;
	}
	
	/** The time the player joined the game
	 * 
	 * @return
	 */
	public long getJoinTime() {
		return this.joinTime;
	}
	@JsonIgnore
	public boolean isCashedOut() {
		return cashedOut;
	}
	
	@JsonIgnore
	void setCashedOut(boolean cashedOut) {
		this.cashedOut = cashedOut;
		cashOutTime = System.currentTimeMillis();
	}
	
	@JsonIgnore 
	long getTimePlayed() {
		return ((this.cashOutTime == -1 ? System.currentTimeMillis() : cashOutTime) - this.joinTime) / 1000;
	}
	/**
	 * Reset the state ready for next deal
	 * 
	 */
	public void resetForNewDeal() {
		wasBigBlind = false;
		blindsDue = Blinds.NONE;
		lastAction = PlayerActionType.NONE;
		actionOnMe = false;
	}

	/** Reset state information and whether the player 
	 * is sitting in or out of the next game round (fresh hand).
	 * 
	 * @param stack The value of the players stack (on table). If 0, then the player
	 * is set to 'sitting-out'
	 */
	public void resetForNewRound(final float stack) {
		resetForNewDeal();

		folded = false;
		isAllIn = false;
		
		// Before being called a re-buy is performed, therefore if the
		// stack is still zero then sit the player out.
		if (stack == 0) {
			sittingOut = true;
			sitNextRound = SIT_NEXT_ROUND.NONE;
			return;
		}
		
		if (sitNextRound != SIT_NEXT_ROUND.NONE) {
			// If sitting out, and now want in
			if (sitNextRound == SIT_NEXT_ROUND.SIT_IN) {
				sittingOut = false;
			}
			// if not sitting out, but want to
			if (sitNextRound == SIT_NEXT_ROUND.SIT_OUT && !sittingOut) {
				sittingOut = true;
			}
			sitNextRound = SIT_NEXT_ROUND.NONE;
		}
		
	}
	
	/** Is the player going to sit out when reset is called?
	 * 
	 * @return true if sitNextRound == SIT_OUT
	 */
	public boolean isSONR() {
		return sitNextRound == SIT_NEXT_ROUND.SIT_OUT;
	}
	/**
	 * @return the sittingOut
	 */
	public boolean isSittingOut() {
		return sittingOut;
	}

	/** Toggle the player state of sitting in or out in the next round.
	 * If the game is complete, this is applied immediately, otherwise the player
	 * stays in the current round.<p>
	 * This is a bit complicated as it has to cater for users joining before
	 * the initial game has started, or if they 'arrive late'
	 * 
	 * @param gameState The game's current state
	 * @param sitOut True to sit the player out, false to sit back in
	 * @return True if the sitNextRound state has been changed
	 */
	@JsonIgnore
	public boolean toggleSittingOut(final TexasGameState gameState, final boolean sitOut) {
		final SIT_NEXT_ROUND prevState = sitNextRound;
		final boolean immediate = gameState == TexasGameState.COMPLETE;
		
		// If the same action as before, reject it
		if (sitOut == sittingOut && prevState == SIT_NEXT_ROUND.NONE) {
			return false;
		}
		
		if (sitOut) {
			sitNextRound = immediate ? SIT_NEXT_ROUND.NONE : SIT_NEXT_ROUND.SIT_OUT;
			if (!sittingOut) {
				// Player wants to sit out, but they aren't at the moment...
				sittingOut = immediate;
			}
		} else {
			if (sittingOut) {
				sitNextRound = immediate ? SIT_NEXT_ROUND.NONE : SIT_NEXT_ROUND.SIT_IN;
				// Player currently sat out, but want to sit back in...
				if (immediate) {
					sittingOut = false;
				}
			} else {
				sitNextRound = SIT_NEXT_ROUND.NONE;
				sittingOut = false;
			}
		}
		return sitNextRound != prevState || immediate;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerState [isDealer=");
		builder.append(isDealer);
		builder.append(", actionOn=");
		builder.append(actionOnMe);
		builder.append(", blindsDue=");
		builder.append(blindsDue);
		builder.append(", folded=");
		builder.append(folded);
		builder.append(", sittingOut=");
		builder.append(sittingOut);
		builder.append("]");
		return builder.toString();
	}


}
