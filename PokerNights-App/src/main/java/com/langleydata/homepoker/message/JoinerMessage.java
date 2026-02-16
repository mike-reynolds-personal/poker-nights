package com.langleydata.homepoker.message;

import java.util.Set;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.game.players.Player;

/**
 * A Broadcast message to tell all players that someone has entered the game
 * 
 * @author reynolds_mj
 *
 */
public class JoinerMessage extends PokerMessage {
	private Set<Player> currentPlayers;
	private final Player player;
	/**
	 * 
	 * @param joiner The joining user's sessionId
	 * @param playerHandle The player's nick-name
	 * @param picture The player's profile picture
	 */
	public JoinerMessage(final Player player) {
		super(MessageTypes.JOINER, player.getSessionId());
		this.player = player;
	}

	public void setCurrentPlayers(Set<Player> currentPlayers) {
		this.currentPlayers = currentPlayers;
	}

	/**
	 * @return the currentPlayers
	 */
	public Set<Player> getCurrentPlayers() {
		return currentPlayers;
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}
	
}
