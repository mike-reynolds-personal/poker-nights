package com.langleydata.homepoker.persistence.es;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;


/** An Elastic search backed repository for querying and storing game settings
 * 
 * @author reynolds_mj
 * @param <T>
 *
 */
@Repository
@Profile(value = {"prod", "test"})
public interface SettingsRepository extends ElasticsearchRepository<TexasHoldemSettings, String> {

	/** Remove game settings
	 * 
	 */
	long deleteByGameId(final String gameId);
	
	/** Get all games that have not been played
	 * 
	 * @return
	 */
	@Query("{\"term\":{\"wasPlayed\":false}}")
	List<GameSettings> findNotPlayed();
}
