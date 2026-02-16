package com.langleydata.homepoker.persistence;

import java.util.List;

import com.langleydata.homepoker.game.RoundHistory;

public interface RoundHistoryProvider {

	/** Get the history of all rounds for a game
	 * 
	 * @param gameId
	 * @return
	 */
	List<RoundHistory> getGameRounds(final String gameId);
	
	/** Get history for a specific round within a specific game
	 * 
	 * @param gameId
	 * @param round
	 * @return
	 */
	RoundHistory getGameRound(final String gameId, final int round);
	
	/** Add details of a round
	 * 
	 * @param round
	 * @return
	 */
	boolean addGameRound(final RoundHistory round);
	
	/** Delete the history for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	long deleteGameRounds(final String gameId);
	
	/** Get a list of all the game's that have history
	 * 
	 * @return
	 */
	List<String> getGameIds();
}
