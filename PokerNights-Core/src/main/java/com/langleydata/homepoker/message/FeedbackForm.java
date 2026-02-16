package com.langleydata.homepoker.message;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.annotations.Document;

import com.langleydata.homepoker.api.Feedback;
import com.langleydata.homepoker.persistence.ElasticSettings;

/** A client to server messsage containing player or user feedback
 * 
 */
@Document(createIndex = true, indexName = ElasticSettings.FEEDBACK_IDX, refreshInterval = "10s")
public class FeedbackForm implements Feedback {

	private final String messageId = UUID.randomUUID().toString();
	private long created = System.currentTimeMillis();
	private String subject;
	private String body;
	private String playerId;
	private String playerEmail;
	private String gameId;
	private int gameRound;
	private String screen;
	private boolean hasScreenshot = false;
	
	/**
	 * @return the messageId
	 */
	@Override
	public String getMessageId() {
		return messageId;
	}
	/**
	 * @return the created
	 */
	@Override
	public long getCreated() {
		return created;
	}
	/**
	 * @return the subject
	 */
	@Override
	public String getSubject() {
		return subject;
	}
	/**
	 * @param subject the subject to set
	 */
	@Override
	public void setSubject(String subject) {
		this.subject = subject;
	}
	/**
	 * @return the body
	 */
	@Override
	public String getBody() {
		return body;
	}
	/**
	 * @param body the body to set
	 */
	@Override
	public void setBody(String body) {
		this.body = body;
	}
	/**
	 * @return the playerId
	 */
	@Override
	public String getPlayerId() {
		return playerId;
	}
	/**
	 * @param playerId the playerId to set
	 */
	@Override
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	/**
	 * @return the playerEmail
	 */
	@Override
	public String getPlayerEmail() {
		return playerEmail;
	}
	/**
	 * @param playerEmail the playerEmail to set
	 */
	@Override
	public void setPlayerEmail(String playerEmail) {
		this.playerEmail = playerEmail;
	}
	/**
	 * @return the gameId
	 */
	@Override
	public String getGameId() {
		return gameId;
	}
	/**
	 * @param gameId the gameId to set
	 */
	@Override
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}
	/**
	 * @return the gameRound
	 */
	@Override
	public int getGameRound() {
		return gameRound;
	}
	/**
	 * @param gameRound the gameRound to set
	 */
	@Override
	public void setGameRound(int gameRound) {
		this.gameRound = gameRound;
	}
	/**
	 * @return the screen
	 */
	@Override
	public String getScreen() {
		return screen;
	}
	/**
	 * @param screen the screen to set
	 */
	@Override
	public void setScreen(String screen) {
		if (StringUtils.isNotBlank(screen)) {
			hasScreenshot = true;
		}
		this.screen = screen;
	}

	@Override
	public boolean hasScreenshot() {
		return hasScreenshot;
	}

}
