package com.langleydata.homepoker.message;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.persistence.ElasticSettings;

/** A two-way message used for communicating player actions during game-play
 * 
 * @author Mike Reynolds
 */
@Document(createIndex = true, indexName = ElasticSettings.PLAYER_ACTION_IDX, refreshInterval = "10s")
public class PlayerActionMessage extends PokerMessage implements PlayerAction {

	private final String actionId = UUID.randomUUID().toString();
	private final long timestamp = System.currentTimeMillis();
	private String playerHandle;
	private PlayerActionType action;
	private float betValue = 0f;
	private boolean successful = true;
	
	/* Info for storage */
	private String playerId;
	private String gameId;
	private int round;
	private TexasGameState gameState;
	
	/** Visible for de-serialisation
	 * 
	 */
	PlayerActionMessage() {
		super(MessageTypes.PLAYER_ACTION, null);
	}
	
	/**
	 * 
	 * @param sessionId
	 */
	public PlayerActionMessage(final String sessionId) {
		super(MessageTypes.PLAYER_ACTION, sessionId);
	}
	
	/**
	 * 
	 * @param sessionId
	 * @param action
	 */
	public PlayerActionMessage(final String sessionId, final PlayerActionType action) {
		super(MessageTypes.PLAYER_ACTION, sessionId);
		this.action = action;
	}
	/**
	 * @return the actionId
	 */
	@Override
	@JsonIgnore
	@Id
	public String getActionId() {
		return actionId;
	}

	/**
	 * @return the playerHandle
	 */
	@Override
	public String getPlayerHandle() {
		return playerHandle;
	}

	/**
	 * @param playerHandle the playerHandle to set
	 */
	@Override
	public PlayerActionMessage setPlayerHandle(String playerHandle) {
		this.playerHandle = playerHandle;
		return this;
	}

	/**
	 * @return the betValue rounded to 2 decimal places
	 */
	@Override
	public float getBetValue() {
		return Math.round(betValue * 100) / 100f;
	}

	/**
	 * @param betValue the betValue to set
	 */
	@Override
	public PlayerActionMessage setBetValue(float betValue) {
		this.betValue = betValue;
		return this;
	}


	/**
	 * @return the action
	 */
	@Override
	public PlayerActionType getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	@Override
	public PlayerAction setAction(PlayerActionType action) {
		this.action = action;
		return this;
	}

	/** Was the action successfully processed - i.e resulted in a game state change?
	 * 
	 * @return
	 */
	@Override
	public boolean isSuccessful() {
		return successful;
	}

	/** Set whether the action was successfully processed - resulted in a game state
	 * change
	 * 
	 * @param successful
	 */
	@Override
	public PlayerActionMessage setSuccessful(boolean successful) {
		this.successful = successful;
		return this;
	}

	/**
	 * @return the playerId
	 */
	@Override
	public String getPlayerId() {
		return playerId;
	}

	/**
	 * @param playerId the playerId to set
	 */
	@Override
	public PlayerActionMessage setPlayerId(String playerId) {
		this.playerId = playerId;
		return this;
	}

	/**
	 * @return the gameId
	 */
	@Override
	@JsonIgnore
	public String getGameId() {
		return gameId;
	}

	/**
	 * @param gameId the gameId to set
	 */
	@Override
	public PlayerAction setGameId(String gameId) {
		this.gameId = gameId;
		return this;
	}

	/**
	 * @return the timestamp
	 */
	@Override
	@JsonIgnore
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the round
	 */
	@Override
	@JsonIgnore
	public int getRound() {
		return round;
	}

	/**
	 * @param round the round to set
	 */
	@Override
	public PlayerAction setRound(int round) {
		this.round = round;
		return this;
	}

	/**
	 * @return the gameState
	 */
	public TexasGameState getGameState() {
		return gameState;
	}

	/**
	 * @param gameState the gameState to set
	 */
	public PlayerActionMessage setGameState(TexasGameState gameState) {
		this.gameState = gameState;
		return this;
	}
	
	
}
