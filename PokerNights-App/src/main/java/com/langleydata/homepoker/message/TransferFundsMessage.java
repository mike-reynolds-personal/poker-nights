package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.PlayerActionType;

/** A one-way message from client to server to initiate a transfer all or part
 * of one player's stack value to another. Only initiated by the Game Host
 * 
 * @author Mike Reynolds
 *
 */
public class TransferFundsMessage extends PlayerActionMessage {

	private String fromId;
	private String toId;

	// Serialisation constructor
	TransferFundsMessage() {

	}
	
	/** Create a new TransferFundsMessage from an existing one.
	 * Used during the sending of an error response
	 * 
	 * @param toClone
	 */
	public TransferFundsMessage(TransferFundsMessage toClone) {
		this(toClone.sessionId);
		this.fromId = toClone.fromId;
		this.toId = toClone.toId;
		this.setBetValue(toClone.getBetValue());
	}
	/**
	 * 
	 * @param sessionId The sessionId of the host
	 */
	public TransferFundsMessage(final String sessionId) {
		super(sessionId);
		setAction(PlayerActionType.TRANSFER_FUNDS);
	}


	/**
	 * @return the fromId
	 */
	public String getFromId() {
		return fromId;
	}


	/**
	 * @param fromId the fromId to set
	 */
	public TransferFundsMessage setFromId(String fromId) {
		this.fromId = fromId;
		return this;
	}


	/**
	 * @return the toId
	 */
	public String getToId() {
		return toId;
	}


	/**
	 * @param toId the toId to set
	 */
	public TransferFundsMessage setToId(String toId) {
		this.toId = toId;
		return this;
	}

	/** For consistency
	 * 
	 * @param toTransfer
	 */
	public TransferFundsMessage setAmount(float toTransfer) {
		super.setBetValue(toTransfer);
		return this;
	}
	
	/** For consistency
	 * 
	 * @return
	 */
	public float getAmount() {
		return super.getBetValue();
	}
}
