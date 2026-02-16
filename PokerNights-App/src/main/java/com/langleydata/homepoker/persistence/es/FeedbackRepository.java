package com.langleydata.homepoker.persistence.es;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.langleydata.homepoker.api.Feedback;

@Repository
@Profile(value = {"prod", "test"})
public interface FeedbackRepository extends ElasticsearchRepository<Feedback, String> {

	/** Get all the feedback for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	List<Feedback> findByGameId(final String gameId);
}
