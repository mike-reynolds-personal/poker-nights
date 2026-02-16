package com.langleydata.homepoker.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;

/** A two way message used for ejecting a player from the game, or for the 
 * recipient to reject the ejection!
 * 
 */
public class EvictPlayerMessage extends PokerMessage {

	private String sentByplayerHandle;
	private String gameId;
	private String toEvictId;
	private boolean isReject = false;
	
	/**
	 * 
	 */
	public EvictPlayerMessage() {
		super(MessageTypes.EVICT_PLAYER, null);
	}

	/**
	 * 
	 * @param error
	 */
	public EvictPlayerMessage(final String error) {
		this();
		setMessage(error);
	}

	/**
	 * @return the toEvictId
	 */
	public String getToEvictId() {
		return toEvictId;
	}

	/**
	 * @param toEvictId the toEvictId to set
	 */
	public void setToEvictId(String toEvictId) {
		this.toEvictId = toEvictId;
	}

	/**
	 * @return the isReject
	 */
	public boolean isReject() {
		return isReject;
	}

	/**
	 * @param isReject the isReject to set
	 */
	public void setIsReject(boolean isReject) {
		this.isReject = isReject;
	}
	
	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getPlayerHandle() {
		return sentByplayerHandle;
	}

	public void setPlayerHandle(String sentByplayerHandle) {
		this.sentByplayerHandle = sentByplayerHandle;
	}

	/** Return a new PlayerAction for history storage
	 * 
	 * @param hostId
	 * @param round
	 * @param gameState
	 * @return
	 */
	@JsonIgnore
	public PlayerActionMessage toPlayerAction(final String hostId, final int round, final TexasGameState gameState) {
		final PlayerActionMessage evictAction = new PlayerActionMessage(hostId);
		evictAction.setAction(PlayerActionType.EVICT_PLAYER);
		evictAction.setPlayerId(hostId);
		evictAction.setPlayerHandle(sentByplayerHandle);
		evictAction.setGameId(gameId);
		evictAction.setRound(round);
		evictAction.setGameState(gameState);
		return evictAction;
	}
	
}
