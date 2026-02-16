package com.langleydata.homepoker.game;

import com.langleydata.homepoker.game.players.Player;

/** Listen for when a player is removed from a game
 * 
 * @author Mike Reynolds
 *
 */
public interface PlayerRemovedListener {

	/** A player has been removed from an active game
	 * 
	 * @param gameId The game removed from
	 * @param player The player removed
	 */
	public void playerRemoved(final String gameId, final Player player);
}
