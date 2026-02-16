package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.MessageTypes;

/** A Broadcast message to tell all players someone has left the game
 * 
 * @author reynolds_mj
 *
 */
public class LeaverMessage extends PokerMessage {
	private final String playerHandle;
	private final int seatingPos;
	
	/** Construct a new leaver message
	 * 
	 * @param sessionId Who's leaving
	 * @param playerHandle Their name
	 */
	public LeaverMessage(final String sessionId, final String playerHandle, final int seatingPos) {
		super(MessageTypes.LEAVER, sessionId);
		this.playerHandle = playerHandle;
		this.seatingPos = seatingPos;
	}

	/**
	 * @return the playerHandle
	 */
	public String getPlayerHandle() {
		return playerHandle;
	}

	public int getSeatingPos() {
		return seatingPos;
	}
	
}
