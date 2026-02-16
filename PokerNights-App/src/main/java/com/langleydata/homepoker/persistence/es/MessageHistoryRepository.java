package com.langleydata.homepoker.persistence.es;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.message.PlayerActionMessage;

/** This Elasticsearch repository (elastic index) holds the history of all player actions
 * This can be extended with further explicit queries<p>
 * TODO This really shouldn't need a concrete class of PokerActionMessage, but the interface,
 * but don't know why it isn't using it correctly
 * @author reynolds_mj
 *
 */
@Repository
@Profile(value = {"prod", "test"})
public interface MessageHistoryRepository extends ElasticsearchRepository<PlayerActionMessage, String> {

	/** Get all the player actions for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	List<PlayerAction> findByGameId(final String gameId);
	
	/** Get all the player actions for a round within a game 
	 * 
	 * @param gameId
	 * @param round
	 * @return
	 */
	List<PlayerAction> findByGameIdAndRound(final String gameId, final Integer round);
	
	/** Get all actions by a player within a game
	 * 
	 * @param gameId
	 * @param playerId
	 * @return
	 */
	List<PlayerAction> findByGameIdAndPlayerId(final String gameId, final String playerId);
	
	/** Delete all game history
	 * 
	 * @param gameId
	 */
	long deleteBygameId(final String gameId);
}
