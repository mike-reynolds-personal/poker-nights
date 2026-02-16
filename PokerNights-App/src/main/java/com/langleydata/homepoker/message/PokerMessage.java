package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.GameMessage;
import com.langleydata.homepoker.api.MessageTypes;

/** Base class for sending messages to and from a client
 * 
 */
public abstract class PokerMessage implements GameMessage {
	final String sessionId;
	final transient MessageTypes messageType;
	protected String message;
	String picture;

	/**
	 * 
	 * @param type
	 * @param sessionId Who is sending the message
	 */
	public PokerMessage(final MessageTypes type, final String sessionId) {
		this.messageType = type;
		this.sessionId = sessionId;
	}

	/** This message's type
	 * 
	 * @return The type
	 */
	@Override
	public MessageTypes getMessageType() {
		return messageType;
	}
	
	/** Get any associated message
	 * 
	 * @return The message or a blank String.
	 */
	@Override
	public String getMessage() {
		return this.message == null ? "" : message;
	}
	
	/**
	 * @param message the message to set
	 */
	@Override
	public void setMessage(final String message) {
		this.message = message;
	}

	/** Get the sessionId for this message
	 * 
	 * @return the relevant sessionId
	 */
	@Override
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @return the picture
	 */
	@Override
	public String getPicture() {
		return picture;
	}

	/**
	 * @param picture the picture to set
	 */
	@Override
	public void setPicture(String picture) {
		this.picture = picture;
	}
	
}
