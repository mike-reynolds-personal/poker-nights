package com.langleydata.homepoker.api;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface AccountStats {

	/** The player's VPIP rating - 
	 * Number of pre-flop rounds where they have voluntarily added money,
	 * divided by the number of hands they've played
	 * 
	 * @return A percentage
	 */
	float getVpip();

	/**
	 * @return the balance
	 */
	float getBalance();

	/**
	 * @return the rebuys
	 */
	int getRebuys();

	/**
	 * @return the handsPlayed
	 */
	int getHandsPlayed();

	/**
	 * @return the won
	 */
	int getWon();

	/**
	 * @return the lost
	 */
	int getLost();

	/** The total amount of time the player played for
	 * 
	 * @return
	 */
	long getTimePlayed();
	
	/** The number of voluntary bets
	 * 
	 * @return
	 */
	@JsonIgnore
	int getVolBets();
	
	/** The games this user has played in
	 * 
	 * @return
	 */
	List<String> getGamesPlayed();

	/** The timestamp of the last game this account played in
	 * 
	 * @return The timestamp, or -1 if they have never played a game
	 */
	long getLastGamePlayedTime();
	
	/** Get all of the games organised by this player keyed by GameId
	 * 
	 * @return Map of <GameId, ScheduledTime>
	 */
	Map<String, Long> getOrganisedGames();
	
	/** Apply a set of new game stats to the account stats
	 * 
	 * @param newStats
	 * @param gameId The current gameId
	 */
	void updateStats(AccountStats newStats, String gameId);
}