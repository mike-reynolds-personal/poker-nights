package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.MessageTypes;

/**
 * A Broadcast message to tell all players that someone has entered the game
 * 
 * @author reynolds_mj
 *
 */
public class SubscribeMessage extends PokerMessage {
	private final String playerId;
	private final String playerHandle;
	/**
	 * 
	 * @param joiner
	 */
	public SubscribeMessage(final String sessionId, final String playerId, final String playerHandle) {
		super(MessageTypes.SUBSCRIBE, sessionId);
		this.playerId = playerId;
		this.playerHandle = playerHandle;
	}

	/** The authorised user's name
	 * 
	 * @return
	 */
	public String getPlayerHandle() {
		return playerHandle;
	}

	/** The authorised user's playerId
	 * 
	 * @return
	 */
	public String getPlayerId() {
		return playerId;
	}
	
}
