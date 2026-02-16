package com.langleydata.homepoker.api;

public class ActiveGame {
	private final String gameId;
	private String gameFormat;
	private int players;
	
	public ActiveGame(String gameId) {
		this.gameId = gameId;
	}

	/**
	 * @return the gameFormat
	 */
	public String getGameFormat() {
		return gameFormat;
	}

	/**
	 * @param gameFormat the gameFormat to set
	 */
	public void setGameFormat(String gameFormat) {
		this.gameFormat = gameFormat;
	}

	/**
	 * @return the players
	 */
	public int getPlayers() {
		return players;
	}

	/**
	 * @param players the players to set
	 */
	public void setPlayers(int players) {
		this.players = players;
	}

	/**
	 * @return the gameId
	 */
	public String getGameId() {
		return gameId;
	}
	
}
