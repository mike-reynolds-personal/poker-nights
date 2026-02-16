package com.langleydata.homepoker.api;

public enum AccountLevel {
	/** The lowest free level. Restricted to one virtual-money game per month that they play in*/
	TIER_1(1, true),
	/** Restricted to multiple virtual-money games per month that they play in*/
	TIER_2(5, true),
	/** Restricted to multiple games (real or virtual) that they don't have to play in*/
	TIER_3(10, false),
	/** Associate that can distribute? */
	TIER_4(Integer.MAX_VALUE, false);
	
	private final int gamesPerMonth;
	private final boolean mustHost;
	
	/**
	 * 
	 * @param gamesPerMonth
	 * @param mustHost
	 */
	private AccountLevel(int gamesPerMonth, boolean mustHost) {
		this.gamesPerMonth = gamesPerMonth;
		this.mustHost = mustHost;
	}

	/**
	 * @return the gamesPerMonth
	 */
	public int getGamesPerMonth() {
		return gamesPerMonth;
	}

	/**
	 * @return the mustHost
	 */
	public boolean isMustHost() {
		return mustHost;
	}
	
}
