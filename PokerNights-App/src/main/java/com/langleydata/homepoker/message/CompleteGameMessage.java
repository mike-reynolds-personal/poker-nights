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
public class CompleteGameMessage extends PokerMessage {
	private final Map<String, GameEndStats> stats = new HashMap<>();
	
	public CompleteGameMessage(final String error) {
		super(MessageTypes.COMPLETE_GAME, null);
		this.message = error;
	}
	
	public CompleteGameMessage(Map<String, GameStats> stats) {
		super(MessageTypes.COMPLETE_GAME, null);
		
		stats.entrySet().forEach(e-> {
			this.stats.put(e.getKey(), new GameEndStats(e.getValue()));
		});
	}

	public Map<String, GameEndStats> getStats() {
		return this.stats;
	}
	
	/** Class to include the transfer amount
	 * 
	 */
	public class GameEndStats extends GameStats {
		private float transfer = 0;
		
		public GameEndStats(GameStats gs) {
			this.transfer = gs.getInitialWallet() + gs.getBalance();
			updateStats(gs, null);
			gamesPlayedIn = null;
			organised = null;
			this.setRank(gs.getRank());
		}
		
		public float getTransfer() {
			return this.transfer;
		}

	}
}
