package com.langleydata.homepoker.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum PlayerActionType {
	NONE("none"),
	DEAL("Deal"),
	TRANSFER_FUNDS("Transfer Stack", true, false, false),
	POST_BLIND("Posted a blind"),
	CALL("Called"),
	CHECK("Checked"),
	BET("Bet"),
	RAISE("Raised"),
	ALL_IN("Went all-in"),
	FOLD("Folded", true, true, true),
	REVEAL("Revealed their cards", true, true, true),
	RE_BUY("Bought back in", true, false, false),
	SIT_OUT("User sitting out", false, false, false),
	CASH_OUT("Cashed out", true, false, false),
	EVICT_PLAYER("Eject player", true, false, false),
	@Deprecated
	BIG_REVEAL("deprecated");
	
	final String friendlyMessage;
	final boolean sendUpdate;
	final boolean isFold;
	final boolean onTurnOnly;
	
	/** Defaults to sendUpdate=true, isFold=false, onTurn=true
	 * 
	 * @param friendly The description to use in the client
	 */
	private PlayerActionType(final String friendly) {
		this(friendly, true, false, true);
	}
	
	/**
	 * 
	 * @param friendly The description to use in the client
	 * @param sendUpdate Should a GameUpdateMessage be sent to the table?
	 * @param isFold Is this a fold action?
	 * @param onTurnOnly Is this action only permissible on the users go>?
	 */
	private PlayerActionType(final String friendly, final boolean sendUpdate, final boolean isFold, final boolean onTurnOnly) {
		this.friendlyMessage = friendly;
		this.sendUpdate = sendUpdate;
		this.isFold = isFold;
		this.onTurnOnly = onTurnOnly;
	}
	
	public String getFriendlyMessage() {
		return this.friendlyMessage;
	}
	
	/** Is this state a bet with a value? (Call, Bet, Raise or all-in */
	public boolean isValueBet() {
		switch (this) {
		case CALL:
		case BET:
		case RAISE:
		case ALL_IN:
			return true;
		default:
			return false;
		}
	}
	
	@JsonIgnore
	public boolean onTurnOnly() {
		return onTurnOnly;
	}
	/** Is this a fold action?
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isFold() {
		return isFold;
	}
	/** Should a game update be returned to the client?
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean sendUpdate() {
		return this.sendUpdate;
	}
}
