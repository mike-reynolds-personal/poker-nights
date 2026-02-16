package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.players.Player;

/** A one-way message to the person that has just joined the game.
 * Includes the current game's state
 * 
 * @author reynolds_mj
 *
 */
public class PrivateJoinerMessage extends PokerMessage {
	private AbstractCardGame<?> currentGame;
	private Player player;
	private boolean successful = false;
	private boolean isReconnect = false;
	
	/**
	 * 
	 * @param joinerId Who joined?
	 */
	public PrivateJoinerMessage(final String joinerId) {
		super(MessageTypes.JOINER_PRIVATE, joinerId);
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @param player the player to set
	 */
	public PrivateJoinerMessage setPlayer(Player player) {
		this.player = player;
		return this;
	}

	/**
	 * @return the currentGame
	 */
	public AbstractCardGame<?> getCurrentGame() {
		return currentGame;
	}

	/**
	 * @param currentGame the currentGame to set
	 */
	public PrivateJoinerMessage setCurrentGame(final AbstractCardGame<?> currentGame) {
		this.currentGame = currentGame;
		return this;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public PrivateJoinerMessage setSuccessful(boolean successful) {
		this.successful = successful;
		return this;
	}

	public boolean isReconnect() {
		return isReconnect;
	}

	public PrivateJoinerMessage setReconnect(boolean isReconnect) {
		this.isReconnect = isReconnect;
		return this;
	}
	
}
