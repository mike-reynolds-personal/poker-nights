package com.langleydata.homepoker.game;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Document;

import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;

@Document(createIndex = true, indexName = "round-history", refreshInterval = "5s")
public class GameRound implements RoundHistory {
	
	private long completeTime = System.currentTimeMillis();
	private String gameId;
	private int roundNum;
	private TexasGameState gameState;
	private GamePots gamePots;
	private String[] tableCards;
	private List<Player> players;
	private long shuffleSeed;
	
	
	private GameRound() {}
	
	public static class Builder {
		GameRound gr = new GameRound();
		
		public Builder gameId(final String gameId) {
			gr.gameId = gameId;
			return this;
		}
		public Builder round(final int round) {
			gr.roundNum = round;
			return this;
		}
		public Builder pots(final GamePots pots) {
			gr.gamePots = pots;
			return this;
		}
		public Builder players(final List<Player> players) {
			gr.players = players;
			return this;
		}
		public Builder tableCards(final String... tCards) {
			gr.tableCards = tCards;
			return this;
		}
		public Builder state(final TexasGameState gState) {
			gr.gameState = gState;
			return this;
		}
		public Builder seed(final long seed) {
			gr.shuffleSeed = seed;
			return this;
		}
		public GameRound build() {
			return gr;
		}
	}

	/**
	 * @return the gameId
	 */
	@Override
	public String getGameId() {
		return gameId;
	}

	/**
	 * @return the roundNum
	 */
	@Override
	public int getRound() {
		return roundNum;
	}

	/**
	 * @param gamePots the gamePots to set
	 */
	public void setGamePots(GamePots gamePots) {
		this.gamePots = gamePots;
	}

	/**
	 * @return the gamePots
	 */
	public GamePots getGamePots() {
		return gamePots;
	}

	/**
	 * @return the tableCards
	 */
	public String[] getTableCards() {
		return tableCards;
	}

	/**
	 * @return the players
	 */
	@Override
	public List<Player> getPlayers() {
		return players;
	}

	@Override
	public long getCompleteTime() {
		return completeTime;
	}

	@Override
	public TexasGameState getGameState() {
		return gameState;
	}

	@Override
	public String getId() {
		return getGameId() + "-" + getRound();
	}
	
	/**
	 * @return the shuffleSeed
	 */
	@Override
	public long getShuffleSeed() {
		return shuffleSeed;
	}

	/**
	 * @param shuffleSeed the shuffleSeed to set
	 */
	public void setShuffleSeed(long shuffleSeed) {
		this.shuffleSeed = shuffleSeed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gameId == null) ? 0 : gameId.hashCode());
		result = prime * result + roundNum;
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
		GameRound other = (GameRound) obj;
		if (gameId == null) {
			if (other.gameId != null)
				return false;
		} else if (!gameId.equals(other.gameId))
			return false;
		if (roundNum != other.roundNum)
			return false;
		return true;
	}

	
}
