package com.langleydata.homepoker.persistence.es;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.langleydata.homepoker.game.RoundHistory;

/** The Elasticsearch repository (elastic index) that holds the history of all game rounds
 * This can be extended with further explicit queries
 * 
 * @author reynolds_mj
 *
 */
@Repository
@Profile(value = {"prod", "test"})
public interface RoundHistoryRepository extends ElasticsearchRepository<RoundHistory, String> {

	/** Get a list of all rounds for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	List<RoundHistory> findByGameId(final String gameId);
	
	/** Get a single round within a specific game
	 * 
	 * @param gameId
	 * @param round
	 * @return
	 */
	@Query("{\"bool\":{\"must\":[{\"match\":{\"gameId\":\"?0\"}},{\"term\":{\"roundNum\":?1}}]}}")
	List<RoundHistory> findByGameIdAndRound(final String gameId, final Integer round);
	
	/** Delete all game history
	 * 
	 * @param gameId
	 */
	long deleteBygameId(final String gameId);
}
