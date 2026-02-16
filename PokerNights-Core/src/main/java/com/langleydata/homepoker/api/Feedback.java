package com.langleydata.homepoker.api;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.langleydata.homepoker.persistence.ElasticSettings;

@Document(createIndex = true, indexName = ElasticSettings.FEEDBACK_IDX, refreshInterval = "10s")
public interface Feedback {

	/**
	 * @return the messageId
	 */
	@Id
	String getMessageId();

	/**
	 * @return the created
	 */
	long getCreated();

	/**
	 * @return the subject
	 */
	String getSubject();

	/**
	 * @param subject the subject to set
	 */
	void setSubject(String subject);

	/**
	 * @return the body
	 */
	String getBody();

	/**
	 * @param body the body to set
	 */
	void setBody(String body);

	/**
	 * @return the playerId
	 */
	String getPlayerId();

	/**
	 * @param playerId the playerId to set
	 */
	void setPlayerId(String playerId);

	/**
	 * @return the playerEmail
	 */
	String getPlayerEmail();

	/**
	 * @param playerEmail the playerEmail to set
	 */
	void setPlayerEmail(String playerEmail);

	/**
	 * @return the gameId
	 */
	String getGameId();

	/**
	 * @param gameId the gameId to set
	 */
	void setGameId(String gameId);

	/**
	 * @return the gameRound
	 */
	int getGameRound();

	/**
	 * @param gameRound the gameRound to set
	 */
	void setGameRound(int gameRound);

	/**
	 * @return the screen
	 */
	String getScreen();

	/**
	 * @param screen the screen to set
	 */
	void setScreen(String screen);

	/** Does the message have a screenshot?
	 * 
	 * @return
	 */
	boolean hasScreenshot();
}