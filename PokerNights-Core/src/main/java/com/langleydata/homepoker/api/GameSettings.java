package com.langleydata.homepoker.api;

import java.util.List;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.MoneyType;
import com.langleydata.homepoker.api.CardGame.ShuffleOption;
import com.langleydata.homepoker.exception.GameSettingsValidationException;

@TypeAlias("game-settings")
public interface GameSettings {

	
	/**
	 * @return the gameId
	 */
	@Id
	String getGameId();
	
	/** The server/ container that is serving this game
	 * 
	 * @return
	 */
	@JsonIgnore
	String getAssignedServer();
	
	/** Are the settings valid?
	 * 
	 * @throws GameSettingsValidationException if not
	 */
	void validate() throws GameSettingsValidationException;
	
	/** Assign this game to a server
	 * 
	 * @param uri
	 */
	void setAssignedServer(String uri);
	/** The shuffle option - generally 'always'
	 * 
	 * @return
	 */
	ShuffleOption getShuffleOption();

	/** Is the Host controlling the wallets?
	 *  
	 * @return true if they are
	 */
	boolean isHostControlledWallet();
	
	/** Get the inaction time before a player is auto-folded
	 * 
	 * @return The time in seconds.
	 */
	int getActionTimeout();
	
	/** Set the amount of inactivity from a player before being auto-folded
	 * 
	 * @param timeInSecs
	 */
	void setActionTimeout(int timeInSecs);
	/** The name of the server we're running on
	 * 
	 * @return
	 */
	String getServerName();
	
	/** Set the name of the server this game is active on
	 * 
	 * @param gameServer
	 */
	void setServerName(final String gameServer);
	
	/** Set Host to control wallets to true
	 * 
	 */
	void setHostControlledWallet(boolean hostControl);
	
	/** The length of time the game was played for in seconds
	 * 
	 * @return
	 */
	long getGameLength();
	/**
	 * @return the organiser's id
	 */
	@JsonIgnore
	String getOrganiserId();

	/**
	 * @param organiserId the organiserId to set
	 */
	@JsonProperty
	void setOrganiserId(String organiserId);

	/** Set the organiser's name
	 * 
	 * @param organiserName
	 */
	@JsonProperty
	void setOrganiserName(String organiserName);
	
	/** The name of the organiser (their handle)
	 * 
	 * @return
	 */
	@JsonIgnore
	String getOrganiserName();
	
	/** The name of the organiser (their handle)
	 * 
	 * @return
	 */
	@JsonIgnore
	String getOrganiserEmail();
	
	/** Set the organiser's email
	 * 
	 * @param organiserEmail
	 */
	@JsonProperty
	void setOrganiserEmail(String organiserEmail);
	
	/** Set the host's name
	 * 
	 * @param hostName
	 */
	@JsonProperty
	void setHostName(String hostName);
	
	/** The name of the host (their handle)
	 * 
	 * @return
	 */
	@JsonIgnore
	String getHostName();
	
	/** The host's email
	 * 
	 * @return
	 */
	@JsonIgnore
	String getHostEmail();
	
	/** Set the host's email
	 * 
	 * @param hostEmail
	 */
	@JsonProperty
	void setHostEmail(String hostEmail);
	
	/** Get any additional information the organiser has added
	 * 
	 * @return
	 */
	@JsonIgnore
	String getAdditionalInfo();
	
	/** Set additional information when a game is scheduled
	 * 
	 * @param additionalInfo
	 */
	@JsonProperty
	void setAdditionalInfo(String additionalInfo);
	
	/** The type of money
	 * 
	 * @return
	 */
	MoneyType getMoneyType();
	
	/**
	 * 
	 * @param moneyType
	 */
	void setMoneyType(MoneyType moneyType);
	
	/** The game format
	 * 
	 * @return
	 */
	GameFormat getFormat();

	/** The value of chips each player gets in a tournament
	 * 
	 * @return
	 */
	int getOpeningStack();
	
	/** For json creation - Set the amount each player's stack should be
	 * 
	 * @param stacksValue
	 */
	void setOpeningStack(int stacksValue);

	/** Get the % split of the prize fund
	 *  
	 * @return
	 */
	List<Integer> getTournamentSplit();
	
	/** Set the split of winnings for a tournament
	 * 
	 * @param prizeSplit An ordered list of the percentages to split the winnings. e.g. (1st = 0 = 60%, 2nd = 1 = 40%)
	 */
	void setTournamentSplit(List<Integer> prizeSplit);
	
	/** What is the latest round an entry to a tournament is permitted?
	 * 
	 * @return
	 */
	int getMaxRoundForTournamentEntry();
	/**
	 * 
	 * @param format
	 */
	void setFormat(GameFormat format);
	/**
	 * @return the gameType
	 */
	GameType getGameType();

	/**
	 * @return the initialBuyin
	 */
	int getBuyInAmount();

	/** The date-time stamp the settings were created
	 * 
	 * @return
	 */
	@JsonIgnore
	long getCreatedTime();
	
	/** When the game is scheduled for
	 * 
	 * @return
	 */
	@JsonIgnore
	long getScheduledTime();
	
	@JsonProperty
	void setScheduledTime(long scheduled);
	/** Have these settings been archived?
	 * Settings are archived if the game has been played 
	 * and the time has expired.
	 * 
	 * @return True if they are
	 */
	@JsonIgnore
	boolean isArchived();
	
	/** Were these game settings, and therefore game, ever played?
	 * 
	 * @return True if it was
	 */
	@JsonIgnore
	boolean wasPlayed();
	
	/** Flag that the game associated with these settings has been played
	 * 
	 */
	@JsonIgnore
	void setWasPlayed();
	
	/** Trigger an increase in the ante/ blinds
	 * 
	 */
	@JsonIgnore
	boolean increaseAnte();
	
	/** Can players buy-in part way through?
	 * 
	 */
	boolean isBuyInDuringGameAllowed();
	
	/**
	 * @return the ante or small blind
	 */
	public float getAnte();
	
	/**
	 * @param ante the smallBlind to set
	 */
	public void setAnte(float ante);
	
	/** Get the client locale 
	 * 
	 * @returnas [language]-[country]
	 */
	public Locale getLocale();
	
	/**
	 * @return the rebuyOption
	 */
	String getRebuyOption();
	/** Enable auto dealing
	 * 
	 * @return
	 */
	boolean isAutoDeal();
	
	/** Get the timeout for nudging
	 * 
	 * @return
	 */
	int getNudgeTime();
	
	/** Set the timeout in seconds for the nudge button
	 * 
	 * @param seconds
	 */
	void seetNudgeTime(int seconds);

	/** Set the game as started
	 * 
	 * @param startedAt
	 */
	@JsonIgnore
	public void setGameStartedAt(long startedAt);
	
	/** Set the game as complete, updating the game length
	 * 
	 */
	@JsonIgnore
	public void setCompleted();
	
	/** Email addresses of those invited to the game
	 * 
	 * @return
	 */
	@JsonIgnore
	List<String> getInvitedEmails();

	@JsonProperty
	void setInvitedEmails(List<String> invited);
}