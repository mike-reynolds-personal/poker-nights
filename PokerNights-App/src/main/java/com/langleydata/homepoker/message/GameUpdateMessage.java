package com.langleydata.homepoker.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.CardGame.GameUpdateType;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.players.SystemPlayer;

/** A one-way (server->Player) broadcast message with the game update
 * 
 * @author reynolds_mj
 *
 */
public class GameUpdateMessage extends PokerMessage {

	final AbstractCardGame<?> currentGame;
	private boolean newGame = false;
	private String actionSound;
	private boolean sendUpdate = true;
	
	
	/** Construct a new update with the game
	 * 
	 * @param currentGame The game detail
	 * @param newGame Is this a new game?
	 */
	public GameUpdateMessage(final AbstractCardGame<?> currentGame, boolean newGame) {
		super(MessageTypes.GAME_UPDATE, SystemPlayer.ID);
		this.currentGame = currentGame;
		this.newGame = newGame;
	}
	
	/** Construct a new update with the game
	 * 
	 * @param currentGame The game detail
	 */
	public GameUpdateMessage(final AbstractCardGame<?> currentGame) {
		super(MessageTypes.GAME_UPDATE, SystemPlayer.ID);
		this.currentGame = currentGame;
	}

	/** Construct a new message with an error
	 * 
	 * @param errorMessage The error message
	 */
	public GameUpdateMessage(final String errorMessage) {
		super(MessageTypes.GAME_UPDATE, SystemPlayer.ID);
		this.currentGame = null;
		this.message = errorMessage;
	}
	/**
	 * 
	 * @param updateType
	 */
	public GameUpdateMessage(final GameUpdateType updateType) {
		super(MessageTypes.GAME_UPDATE, SystemPlayer.ID);
		this.currentGame = null;
		this.message = updateType.name();
	}
	/**
	 * @return the currentGame
	 */
	public AbstractCardGame<?> getCurrentGame() {
		return currentGame;
	}

	/**
	 * @return the newGame
	 */
	public boolean isNewGame() {
		return newGame;
	}

	public String getActionSound() {
		return actionSound;
	}

	public void setActionSound(String actionSound) {
		this.actionSound = actionSound;
	}

	/**
	 * @return the sendUpdate
	 */
	@JsonIgnore
	public boolean sendUpdate() {
		return sendUpdate;
	}

	/**
	 * @param sendUpdate the sendUpdate to set
	 */
	@JsonIgnore
	public GameUpdateMessage setSendUpdate(boolean sendUpdate) {
		this.sendUpdate = sendUpdate;
		return this;
	}
	
	
}
