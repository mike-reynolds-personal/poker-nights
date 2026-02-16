package com.langleydata.homepoker.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A GameServer defines a single instance of an application that can serve card games
 *  of a specific type.
 * 
 * @author Mike Reynolds
 *
 */
public class GameServer {

	private final GameType gameType;
	private String uri;
	private List<ActiveGame> activeGames = new ArrayList<>();
	private String serverName;

	/**
	 * 
	 * @param uri
	 * @param gameType
	 */
	public GameServer(String uri, GameType gameType) {
		this.uri = uri;
		this.gameType = gameType;
	}
	/**
	 * @return the gameType
	 */
	public GameType getGameType() {
		return gameType;
	}
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * 
	 * @param newUri
	 */
	public void setUri(String newUri) {
		this.uri = newUri;
	}
	
	/**
	 * @return the serverName
	 */
	public String getServerName() {
		return serverName;
	}
	/**
	 * @param serverName the serverName to set
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	/**
	 * @return the activeGames
	 */
	public List<ActiveGame> getActiveGames() {
		return activeGames == null ? Collections.emptyList() : activeGames;
	}
	
	/** Add a game to the list of active games
	 * 
	 * @param gameId
	 */
	public void addActiveGame(final ActiveGame gameId) {
		if (this.activeGames==null) {
			activeGames = Collections.singletonList(gameId);
		} else {
			this.activeGames.add(gameId);
		}
	}
	
	/**
	 * 
	 * @param activeGames If Null, the activeGame count will be set to Integer.MAX_VALUE
	 */
	public void setActiveGames(List<ActiveGame> activeGames) {
		this.activeGames = activeGames;
	}
	
	/** Return the number of active games
	 * 
	 * @return
	 */
	public int getGameCount() {
		return activeGames==null ? Integer.MAX_VALUE : activeGames.size();
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gameType == null) ? 0 : gameType.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameServer other = (GameServer) obj;
		if (gameType != other.gameType)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameServer [gameType=");
		builder.append(gameType);
		builder.append(", uri=");
		builder.append(uri);
		builder.append("]");
		return builder.toString();
	}
	
}
