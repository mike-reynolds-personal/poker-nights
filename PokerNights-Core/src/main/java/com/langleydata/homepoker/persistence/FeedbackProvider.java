package com.langleydata.homepoker.persistence;

import java.util.List;

import com.langleydata.homepoker.api.Feedback;

public interface FeedbackProvider {

	/** The count of feedback submitted
	 * 
	 * @return
	 */
	long getFeedbackCount();
	/** Retrieve a FeedbackForm for a specific Game
	 * 
	 * @param gameId
	 * @return
	 */
	List<Feedback> getFeedback(final String gameId);
	
	/** Get all FeedbackForm currently stored
	 * 
	 * @param page The page numnber, starting at 1
	 * @param size The page size
	 * @return
	 */
	List<Feedback> getFeedback(final int page, final int size);
	
	/** Delete a feedback message
	 * 
	 * @param id
	 * @return
	 */
	boolean deleteFeedback(final String id);
	/** Persist the FeedbackForm submitted by a user
	 * 
	 * @param settings
	 * @return
	 */
	boolean storeFeedback(Feedback feedback);
}
