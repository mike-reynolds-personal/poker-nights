package com.langleydata.homepoker.message;

import java.util.HashMap;
import java.util.Map;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.game.GameStats;

/** A server to client message that describes a set of player statistics
 * 
 * @author reynolds_mj
 *
 */
public class PlayerStatsMessage extends PokerMessage {
	private final Map<String, GameStats> stats;
	
	public PlayerStatsMessage(final String error) {
		super(MessageTypes.STATS, null);
		stats = new HashMap<>();
		this.message = error;
	}
	
	/**
	 * 
	 * @param stats
	 */
	public PlayerStatsMessage(Map<String, GameStats> stats) {
		super(MessageTypes.STATS, null);
		this.stats = stats;
		this.stats.values().forEach(s-> {
			s.getOrganisedGames().clear();
			s.getGamesPlayed().clear();
		});
	}

	public Map<String, GameStats> getStats() {
		return this.stats;
	}
}
