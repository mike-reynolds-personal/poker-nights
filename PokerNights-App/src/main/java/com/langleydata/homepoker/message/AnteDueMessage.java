package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.MessageTypes;

/** A general chat message from a user to the Lobby broadcast channel
 * 
 * @author reynolds_mj
 *
 */
public class AnteDueMessage extends PokerMessage {
	
	final Object anteType;
	
	/** Visible for de-serialisation */
	public AnteDueMessage(final String sessionId, Object anteType) {
		super(MessageTypes.ANTE, sessionId);
		this.anteType = anteType;
	}

	/**
	 * @return the anteType
	 */
	public Object getAnteType() {
		return anteType;
	}
	
	
}
