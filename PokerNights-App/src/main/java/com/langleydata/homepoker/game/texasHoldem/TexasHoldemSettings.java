package com.langleydata.homepoker.game.texasHoldem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.MoneyType;
import com.langleydata.homepoker.api.CardGame.ShuffleOption;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.exception.GameSettingsValidationException;
import com.langleydata.homepoker.persistence.ElasticSettings;

/**
 * Game settings specific to Texas Hold'em
 * 
 * @author Mike Reynolds
 *
 */
@Document(createIndex = true, indexName = ElasticSettings.TEXAS_SETTINGS_IDX, refreshInterval = "5s")
@TypeAlias("game-settings")
public class TexasHoldemSettings implements GameSettings {
	static final long ONE_DAY = 60 * 1000 * 60 * 24;
	public enum PotLimit {
		NO_LIMIT,
		LIMIT,
		POT_LIMIT
	}
	
	public enum RebuyType {
		INITIAL_BUY_IN,
		LARGEST_STACK,
		/**@deprecated Use INITIAL_BUY_IN instead. Only for backwards compatability with stored games */
		INITIAL_STAKE
	}
	
	private final long createdTime = System.currentTimeMillis();
	private long gameLength = 0, gameStartedTime = 0;
	private final String gameId = UUID.randomUUID().toString();
	private String hostId, hostName, hostEmail = "";
	private String organiserId, organiserName, organiserEmail;
	private GameType gameType = GameType.TEXAS_HOLDEM;
	private int buyInAmount = 10;
	private float ante = 0.1f;
	private int nudgeTime = 60;
	
	/** Organiser settings */
	private PotLimit potLimit = PotLimit.NO_LIMIT;
	private boolean enforceMinimumRaise = true, buyInDuringGameAllowed = false, rebuyInRound = false, hostControlledWallet = false;
	private RebuyType rebuyOption = RebuyType.INITIAL_BUY_IN;
	private ShuffleOption shuffleOption = ShuffleOption.ALWAYS;
	
	private long scheduledTime = 0;
	private int actionTimeout = -1;
	private List<String> invitedEmail = new ArrayList<>();
	private boolean isArchived = false, wasPlayed = false;
	private MoneyType moneyType = MoneyType.VIRTUAL;
	private GameFormat format = GameFormat.TOURNAMENT;
	private String additionalInfo = "None", serverName = "";
	private Locale locale = new Locale("en", "GB");
	private String assignedServer;
	
	// Tournament properties
	private int openingStacks = 100;
	private long blindIncreaseInterval = Long.MAX_VALUE;
	private List<Integer> prizeSplit = new ArrayList<>();
	private int maxTournamentRoundEntry = 1;
	
	@Override
	public void validate() throws GameSettingsValidationException {
		if (
				StringUtils.isBlank(getHostEmail()) ||
				StringUtils.isBlank(getOrganiserEmail()) ||
				StringUtils.isBlank(getHostName()) ||
				StringUtils.isBlank(getHostEmail()) ||
				StringUtils.isBlank(getGameId())
				) {
			throw new GameSettingsValidationException("Organiser or Host details are not set");
		} else if (getGameType()==null) {
			throw new GameSettingsValidationException("Invalid game type set");
		} else if (getScheduledTime() < System.currentTimeMillis() - (60 * 1000 * 10)) {
			throw new GameSettingsValidationException("You cannot schedule a game in the past");
		} else if (getScheduledTime() > System.currentTimeMillis() + (ONE_DAY * 182)) {
			throw new GameSettingsValidationException("You cannot schedule a game greater than six months away");
		} else if (getActionTimeout() > 0 && getActionTimeout() < 11) {
			throw new GameSettingsValidationException("Take action timeout must be greater than 10 seconds");
		} else if (isHostControlledWallet() && getMoneyType() != MoneyType.VIRTUAL) {
			throw new GameSettingsValidationException("Wallet allocation is only applicable to virtual money games");
		} else if (buyInAmount < 1 || ante < 0.1f || ante > buyInAmount) {
			throw new GameSettingsValidationException("Check the minimum buy-in and ante values");
		} else if (getFormat()==GameFormat.TOURNAMENT) {
			if (openingStacks < 1 || blindIncreaseInterval < 60000) {
				throw new GameSettingsValidationException("Opening stack must be > 1 and blinds increase must be greater than 1 min");
			}
			if (prizeSplit.stream().filter(v -> v==null).count() > 0) {
				throw new GameSettingsValidationException("Prize splits cannot be blank");
			}
			if (prizeSplit.stream().mapToInt(Integer::intValue).sum() !=100 || prizeSplit.size() == 0) {
				throw new GameSettingsValidationException("Prize splits must equal 100% and be > 0");
			}
		}
		
	}
	/**
	 * @return the hostId
	 */
	@Override
	public String getOrganiserId() {
		return organiserId;
	}

	/**
	 * @param hostId the hostId to set
	 */
	@Override
	public void setOrganiserId(String organiserId) {
		this.organiserId = organiserId;
	}

	@Override
	public void setOrganiserName(String organiserName) {
		this.organiserName = organiserName;
	}

	@Override
	public String getOrganiserName() {
		return organiserName;
	}

	@Override
	public String getOrganiserEmail() {
		return organiserEmail;
	}

	@Override
	public boolean isHostControlledWallet() {
		return hostControlledWallet;
	}
	
	@Override
	public void setHostControlledWallet(boolean hostControlledWallet) {
		this.hostControlledWallet = hostControlledWallet;
	}
	
	@Override
	public void setOrganiserEmail(String organiserEmail) {
		this.organiserEmail = organiserEmail;
	}

	@Override
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public String getHostName() {
		return hostName;
	}

	@Override
	public String getHostEmail() {
		return hostEmail;
	}

	@Override
	public void setHostEmail(String hostEmail) {
		this.hostEmail = hostEmail;
	}
	
	@Override
	@JsonIgnore
	public String getServerName() {
		return serverName;
	}
	
	@Override
	public void setServerName(final String gameServer) {
		this.serverName = gameServer;
	}
	/**
	 * @return the rebuyInRound
	 */
	public boolean isRebuyInRound() {
		return rebuyInRound;
	}

	/**
	 * @param rebuyInRound the rebuyInRound to set
	 */
	public void setRebuyInRound(boolean rebuyInRound) {
		this.rebuyInRound = rebuyInRound;
	}
 
	
	/** Can players buy-in part way through?
	 * 
	 */
	@Override
	public boolean isBuyInDuringGameAllowed() {
		return buyInDuringGameAllowed;
	}
	
	@Override
	public int getActionTimeout() {
		return actionTimeout;
	}
	
	@Override
	public void setActionTimeout(int timeInSecs) {
		if (timeInSecs > 0) {
			actionTimeout = timeInSecs;
		}
	}
	/**
	 * @return the rebuyOption
	 */
	@Override
	public String getRebuyOption() {
		return rebuyOption.name();
	}

	/**
	 * @param rebuyOption the rebuyOption to set
	 */
	public void setRebuyOption(RebuyType rebuyOption) {
		this.rebuyOption = rebuyOption;
	}

	/** Double-up the blinds. Only possible up to 50% of the buy-in
	 * 
	 */
	@Override
	public boolean increaseAnte() {
		float newValue = Math.round(ante * 2f * 100f) / 100f;
		final int maxAnte = (getFormat()==GameFormat.TOURNAMENT ? getOpeningStack() : getBuyInAmount()) / 2;
		boolean success = newValue <= maxAnte;
		if (success) {
			this.ante = Math.round(ante * 2f * 100f) / 100f;	
		}
		return success;
	}

	/**
	 * @param buyInDuringGameAllowed the buyInDuringGameAllowed to set
	 */
	public void setBuyInDuringGameAllowed(boolean buyInDuringGameAllowed) {
		this.buyInDuringGameAllowed = buyInDuringGameAllowed;
	}

	/**
	 * @param shuffleOption the shuffleOption to set
	 */
	public void setShuffleOption(ShuffleOption shuffleOption) {
		this.shuffleOption = shuffleOption;
	}

	/**
	 * @param scheduledTime the scheduledTime to set
	 */
	@Override
	public void setScheduledTime(long scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	/**
	 * @return the potLimit
	 */
	public PotLimit getPotLimit() {
		return potLimit;
	}
	/**
	 * @param potLimit the potLimit to set
	 */
	public void setPotLimit(PotLimit potLimit) {
		this.potLimit = potLimit;
	}
	/**
	 * @return the enforceMinimumRaise
	 */
	public boolean isEnforceMinimumRaise() {
		return enforceMinimumRaise;
	}
	/**
	 * @param enforceMinimumRaise the enforceMinimumRaise to set
	 */
	public void setEnforceMinimumRaise(boolean enforceMinimumRaise) {
		this.enforceMinimumRaise = enforceMinimumRaise;
	}

	/**
	 * @param buyInAmount the buyInAmount to set
	 */
	public void setBuyInAmount(int buyInAmount) {
		this.buyInAmount = buyInAmount;
	}
	/**
	 * @return the gameType
	 */
	@Override
	public GameType getGameType() {
		return gameType;
	}

	/**
	 * @param gameType the gameType to set
	 */
	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	/**
	 * @return the initialBuyin
	 */
	@Override
	public int getBuyInAmount() {
		return buyInAmount;
	}

	/**
	 * @param initialBuyin the initialBuyin to set
	 */
	public void setInitialBuyin(int initialBuyin) {
		this.buyInAmount = initialBuyin;
	}

	/**
	 * @return the ante or small blind
	 */
	@Override
	public float getAnte() {
		return ante;
	}

	/**
	 * @param ante the smallBlind to set
	 */
	@Override
	public void setAnte(float ante) {
		this.ante = ante;
	}

	/**
	 * @return the blindIncreaseInterval in milliseconds
	 */
	public long getBlindIncreaseInterval() {
		return blindIncreaseInterval;
	}
	/**
	 * @param blindIncreaseInterval the blindIncreaseInterval in milliseconds
	 */
	public void setBlindIncreaseInterval(long blindIncreaseInterval) {
		this.blindIncreaseInterval = blindIncreaseInterval;
	}
	/**
	 * @return the bigBlind
	 */
	public float getBigBlind() {
		return this.ante * 2f;
	}

	/**
	 * @return the gameId
	 */
	@Override
	public String getGameId() {
		return gameId;
	}
	
	@Override
	public boolean isAutoDeal() {
		return true;
	}
	
	@Override
	public int getOpeningStack() {
		return openingStacks;
	}
	@Override
	public void setOpeningStack(int stacksValue) {
		this.openingStacks = stacksValue;
	}
	
	@Override
	public long getScheduledTime() {
		return scheduledTime;
	}

	@Override
	@JsonIgnore
	public List<String> getInvitedEmails() {
		return invitedEmail;
	}

	@Override
	public void setInvitedEmails(List<String> invited) {
		if (invited!=null) {
			this.invitedEmail = invited;
		}
	}

	@Override
	public ShuffleOption getShuffleOption() {
		return shuffleOption;
	}

	@Override
	public long getCreatedTime() {
		return createdTime;
	}

	@Override
	public boolean isArchived() {
		return isArchived;
	}

	@Override
	public boolean wasPlayed() {
		return wasPlayed;
	}

	@Override
	public void setWasPlayed() {
		this.wasPlayed = true;
	}

	@Override
	public String getAdditionalInfo() {
		return additionalInfo;
	}

	@Override
	public void setAdditionalInfo(final String additionalInfo) {
		if (StringUtils.isNotBlank(additionalInfo)) {
			this.additionalInfo = additionalInfo;
		}
	}
	
	@Override
	public GameFormat getFormat() {
		return this.format;
	}

	@Override
	public MoneyType getMoneyType() {
		return moneyType;
	}

	@Override
	public void setMoneyType(MoneyType moneyType) {
		this.moneyType = moneyType;
	}

	@Override
	public void setFormat(GameFormat format) {
		this.format = format;
	}
	
	@Override
	public List<Integer> getTournamentSplit() {
		return prizeSplit;
	}
	
	@Override
	public void setTournamentSplit(List<Integer> prizeSplit) {
		if (prizeSplit.stream().filter(v -> v==null).count()==0) {
			prizeSplit.sort(Comparator.reverseOrder());
		}
		this.prizeSplit = prizeSplit;
	}
	
	@Override
	public int getMaxRoundForTournamentEntry() {
		return maxTournamentRoundEntry;
	}
	
	public void setMaxRoundForTournamentEntry(int maxRound) {
		maxTournamentRoundEntry = maxRound;
	}
	
	@Override
	public int getNudgeTime() {
		return nudgeTime;
	}

	@Override
	public void seetNudgeTime(int seconds) {
		nudgeTime = seconds;
	}

	@Override
	public String getAssignedServer() {
		return assignedServer;
	}

	@Override
	public void setAssignedServer(String uri) {
		this.assignedServer = uri;
	}
	
	@Override
	public long getGameLength() {
		return gameLength;
	}
	
	@Override
	public void setGameStartedAt(long startedAt) {
		this.gameStartedTime = startedAt;
	}
	
	@Override
	public void setCompleted() {
		if (gameLength == 0) {
			gameLength = (System.currentTimeMillis() - gameStartedTime) / 1000;
			if (gameLength > 120) {
				wasPlayed = true;
				isArchived = true;
			}
		}
	}
	/** Set the locale string for this game
	 * 
	 * @param locale as [language code]-[country code]
	 */
	public void setLocale(final Locale locale) {
		this.locale = locale;
	}
	
	@Override
	public Locale getLocale() {
		return this.locale;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TexasHoldemSettings [gameId=");
		builder.append(gameId);
		builder.append(", hostId=");
		builder.append(hostId);
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append(", initialBuyin=");
		builder.append(buyInAmount);
		builder.append(", ante=");
		builder.append(ante);
		builder.append(", potLimit=");
		builder.append(potLimit);
		builder.append(", Locale=");
		builder.append(locale);
		builder.append("]");
		return builder.toString();
	}
}
