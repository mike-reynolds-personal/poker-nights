package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.game.players.SystemPlayer;

/** A general chat message from a user to the Lobby broadcast channel
 * 
 * @author reynolds_mj
 *
 */
public class ChatMessage extends PokerMessage {
	private String playerHandle;
	private String receiptSound;
	
	/** Visible for de-serialisation */
	public ChatMessage() {
		this(null, null);
	}
	
	/** Create a chat message from the system user
	 * 
	 * @param message The system message to send
	 */
	public ChatMessage(final String message) {
		super(MessageTypes.CHAT, SystemPlayer.ID);
		setPicture(SystemPlayer.PICTURE);
		setMessage(message);
	}
	/**
	 * 
	 * @param originator Who sent the message
	 * @param message The message
	 */
	public ChatMessage(final String originator, final String message) {
		super(MessageTypes.CHAT, originator);
		this.message = message;
	}

	/**
	 * @return the playerHandle
	 */
	public String getPlayerHandle() {
		return playerHandle;
	}

	/**
	 * @param playerHandle the playerHandle to set
	 */
	public void setPlayerHandle(String playerHandle) {
		this.playerHandle = playerHandle;
	}
	
	public String getReceiptSound() {
		return receiptSound;
	}
	
	public void setReceiptSound(String receiptSound) {
		this.receiptSound = receiptSound;
	}
	
}
