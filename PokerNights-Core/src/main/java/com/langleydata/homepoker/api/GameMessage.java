package com.langleydata.homepoker.api;

public interface GameMessage {

	/** This message's type
	 * 
	 * @return The type
	 */
	MessageTypes getMessageType();

	/** Get any associated message
	 * 
	 * @return The message or a blank String.
	 */
	String getMessage();

	/**
	 * @param message the message to set
	 */
	void setMessage(String message);

	/** Get the sessionId for this message
	 * 
	 * @return the relevant sessionId
	 */
	String getSessionId();

	/**
	 * @return the picture
	 */
	String getPicture();

	/**
	 * @param picture the picture to set
	 */
	void setPicture(String picture);

}