package com.langleydata.homepoker.api;

/** A generic player action message
 * 
 * @author Dell
 *
 */
public interface PlayerAction extends GameMessage {

	/**
	 * @return the actionId
	 */
	String getActionId();
	
	/**
	 * @return the playerHandle
	 */
	String getPlayerHandle();

	/**
	 * @param playerHandle the playerHandle to set
	 */
	PlayerAction setPlayerHandle(String playerHandle);

	/**
	 * @return the betValue rounded to 2 decimal places
	 */
	float getBetValue();

	/**
	 * @param betValue the betValue to set
	 */
	PlayerAction setBetValue(float betValue);

	/**
	 * @return the action
	 */
	PlayerActionType getAction();

	/**
	 * @param action the action to set
	 */
	PlayerAction setAction(PlayerActionType action);

	/** Was the action successfully processed - i.e resulted in a game state change?
	 * 
	 * @return
	 */
	boolean isSuccessful();

	/** Set whether the action was successfully processed - resulted in a game state
	 * change
	 * 
	 * @param successful
	 */
	PlayerAction setSuccessful(boolean successful);

	/**
	 * @return the playerId
	 */
	String getPlayerId();

	/**
	 * @param playerId the playerId to set
	 */
	PlayerAction setPlayerId(String playerId);

	/**
	 * @return the gameId
	 */
	String getGameId();

	/**
	 * @param gameId the gameId to set
	 */
	PlayerAction setGameId(String gameId);

	/**
	 * @return the timestamp
	 */
	long getTimestamp();

	/**
	 * @return the round
	 */
	int getRound();

	/**
	 * @param round the round to set
	 */
	PlayerAction setRound(int round);

}