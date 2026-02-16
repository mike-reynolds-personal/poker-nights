package com.langleydata.homepoker.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.langleydata.homepoker.api.AccountStats;

/** Account statistics that cater for in-game and all games
 * 
 */
public abstract class PlayerStats implements AccountStats {

	protected float balance = 0;
	protected int won = 0, lost = 0, rebuys = 0, handsPlayed = 0, volBets = 0;
	protected long lastPlayed = -1, timePlayed = 0;
	protected List<String> gamesPlayedIn = new ArrayList<>();
	protected Map<String, Long> organised = new HashMap<>();
	
	@Override
	public float getVpip() {
		if (handsPlayed==0) {
			return 0;
		}
		return Math.round((volBets / (float)handsPlayed) * 1000) / 10;
	}

	@Override
	public float getBalance() {
		return Math.round(balance * 100f) / 100f;
	}

	@Override
	public int getRebuys() {
		return rebuys;
	}

	@Override
	public int getHandsPlayed() {
		return handsPlayed;
	}

	@Override
	public int getWon() {
		return won;
	}

	@Override
	public int getLost() {
		return lost;
	}

	@Override
	public int getVolBets() {
		return volBets;
	}
	
	@Override
	public long getTimePlayed() {
		return timePlayed;
	}

	@Override
	public List<String> getGamesPlayed() {
		return gamesPlayedIn;
	}

	@Override
	public long getLastGamePlayedTime() {
		return lastPlayed;
	}

	@Override
	public Map<String, Long> getOrganisedGames() {
		return organised;
	}

	@Override
	public void updateStats(AccountStats newStats, String gameId) {
		this.lastPlayed = System.currentTimeMillis();
		if (gamesPlayedIn == null) {
			gamesPlayedIn = new ArrayList<>();
		}
		
		// If we haven't played this game before..
//		if (StringUtils.isNotBlank(gameId) && !gamesPlayedIn.contains(gameId)) {
			if (!gamesPlayedIn.contains(gameId)) {
				gamesPlayedIn.add(gameId);
			}
			
			// Update
			this.balance += newStats.getBalance();
			this.handsPlayed += newStats.getHandsPlayed();

			this.lost += newStats.getLost();
			this.won += newStats.getWon();
			this.rebuys += newStats.getRebuys();
			this.volBets += newStats.getVolBets();
			this.timePlayed += newStats.getTimePlayed();
			
//		} else {
//			// Player could have rejoined, so just overwrite
//			this.balance = newStats.getBalance();
//			this.handsPlayed = newStats.getHandsPlayed();
//			this.lost = newStats.getLost();
//			this.won = newStats.getWon();
//
//			this.rebuys = newStats.getRebuys();
//			this.volBets = newStats.getVolBets();
//			this.timePlayed = newStats.getTimePlayed();
//		}

	}
	
}
