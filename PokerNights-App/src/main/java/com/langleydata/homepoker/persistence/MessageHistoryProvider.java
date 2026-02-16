package com.langleydata.homepoker.persistence;

import java.util.List;

import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.message.PlayerActionMessage;

/** An interface to provide player action history
 * 
 */
public interface MessageHistoryProvider {

	/** Get all player actions for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	List<PlayerAction> getPlayerActions(final String gameId);
	
	/** Get all player actions for a round within a game
	 * 
	 * @param gameId
	 * @param round
	 * @return
	 */
	List<PlayerAction> getPlayerActions(final String gameId, final int round);
	
	/** Get all player actions for a specific player in a game
	 * 
	 * @param gameId
	 * @param playerId
	 * @return
	 */
	List<PlayerAction> getPlayerActions(final String gameId, final String playerId);
	/** Add a player action to the repo
	 * 
	 * @param action
	 * @return
	 */
	boolean addPlayerAction(final PlayerActionMessage action);
	
	/** Delete all player actions for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	long deletePlayerActions(final String gameId);
	
}
