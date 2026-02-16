package com.langleydata.homepoker.api;

/** The types of card game that can be played.
 * Note that this is linked to the types of 'GameServer' that are implemented
 * and running. Change names will adversely effect the logic */
public enum GameType {
	TEXAS_HOLDEM(2, 2, 9, "Texas Hold'em", "texas"),
	TEXAS_HOLDEM_SNG(2, 3, 9, "Texas Hold'em SnG", "texas_sng"),
	OMAHA(2, 2, 9, "Omaha", "omaha");
	
	private final int startCards;
	private final int maxPlayers;
	private final int minPlayers;
	private final String friendly;
	private final String servicePath;
	
	/**
	 * 
	 * @param startCards
	 * @param minPlayers
	 * @param maxPlayers
	 * @param friendly
	 */
	private GameType(int startCards, int minPlayers, int maxPlayers, String friendly, String servicePath) {
		this.startCards = startCards;
		this.maxPlayers = maxPlayers;
		this.minPlayers = minPlayers;
		this.friendly = friendly;
		this.servicePath = servicePath;
	}
	
	public int getStartCards() {
		return this.startCards;
	}
	
	public int getMaxPlayers() {
		return this.maxPlayers;
	}
	/**
	 * @return the minPlayers
	 */
	public int getMinPlayers() {
		return minPlayers;
	}

	public String getFriendly() {
		return friendly;
	}
	/** Get a type from a path variable
	 * 
	 * @param path
	 * @return
	 */
	public static GameType fromPathPart(String path) {
		for (GameType gt : GameType.values()) {
			if ( gt.servicePath.equals(path)) {
				return gt;
			}
		}
		return null;
	}
}