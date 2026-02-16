package com.langleydata.homepoker.game;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.persistence.ElasticSettings;

@Document(createIndex = true, indexName = ElasticSettings.TEXAS_ROUNDS_IDX, refreshInterval = "10s")
public interface RoundHistory {

	/** The persistent ID of the game, from when the game was first created
	 * 
	 * @return
	 */
	@Id
	public String getId();
	/**
	 * @return the gameId
	 */
	public String getGameId();

	/**
	 * @return the roundNum
	 */
	public int getRound();
	
	/**
	 * @return the players
	 */
	public List<Player> getPlayers();
	
	/** What time did the round complete
	 * 
	 * @return
	 */
	public long getCompleteTime();

	/** Get the game state when the round completed
	 * 
	 * @return
	 */
	public TexasGameState getGameState();

	/** Get the seed number used for the shuffle
	 * 
	 * @return the shuffleSeed
	 */
	public long getShuffleSeed();
	
	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object obj);
}
