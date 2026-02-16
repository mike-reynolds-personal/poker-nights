package com.langleydata.homepoker.services;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.AccountStats;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.api.UserAccount.ROLE;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.persistence.GenericSettingsProvider;

/** A simple account service that should be replaced by actual management and storage of accounts at a later date
 * 
 */
@Service
public class SimpleAccountService implements AccountService {
	protected final Logger logger = LoggerFactory.getLogger(SimpleAccountService.class);
	
	static final String MUST_BE_HOST = "At your account tier you must host the game as well as organise it. Please upgrade your account to enable this feature.";
	static final String TOO_MANY_GAMES = "You cannot schedule more than %s game in 30 days. Please upgrade your account to enable this feature.";
	
	@Autowired
	GenericSettingsProvider accProvider;

	@Override
	public List<UserAccount> getAccounts(final int page, final int pageSize) {
		return accProvider.getAllAccounts(page, pageSize);
	}
	
	@Override
	public UserAccount getAccount(String userId) throws GameSchedulingException {
		Account acc = null;
		try {
			acc = Account.buildAccountInfo(accProvider.getAccountById(userId));
		} catch (IOException e) {
			throw new GameSchedulingException(e);
		}

		return acc;
	}
	
	@Override
	public boolean saveAccount(UserAccount userAccount) {
		try {
			return accProvider.storeAccount(userAccount);
		} catch (IOException e) {
			logger.error("Adding user account", e);
		}
		return false;
	}

	@Override
	public void canScheduleGame(String userId, GameSettings gameSettings) throws GameSchedulingException {
		final UserAccount acc = getAccount(userId);
		if (acc==null) {
			throw new GameSchedulingException("No user account for id '" + userId + "'");
		}
		final long gameCount = getScheduledGameCount( acc.getStats().getOrganisedGames(), gameSettings.getScheduledTime() );
		final int gpp =  acc.getAccLevel().getGamesPerMonth();
		
		if (gameCount > gpp) {
			throw new GameSchedulingException(String.format(TOO_MANY_GAMES, gpp));
		} else if (acc.getAccLevel().isMustHost()) {
			if (gameSettings.getHostEmail().equals(gameSettings.getOrganiserEmail())==false) {
				throw new GameSchedulingException(MUST_BE_HOST);
			}
		}
		
	}

	@Override
	public long getAccountCount() {
		try {
			return accProvider.getAccountCount();
		} catch (IOException e) {
			logger.error("Getting user account count {}", e.getMessage());
		}
		return -1;
	}
	/** Update an existing account, if it exists, to the new account information.
	 *  Only updated the account if newDetail.equals(existingAcc) == false
	 *  
	 * @param newDetail
	 * @return True if the account was updated, otherwise false
	 */
	@Override
	public boolean updateAccountProfile(UserAccount existingAcc, Account newDetail) {

		if (existingAcc == null || newDetail == null) {
			return false;
		}
		
		// TODO - Check email and picture
		if (newDetail.equals(existingAcc) ) {
			return false;
		}
		
		newDetail.setAccLevel(existingAcc.getAccLevel());
		existingAcc.getKnownAssociates().forEach(newDetail::addKnownAssociate);
		existingAcc.getRoles().forEach(newDetail::addRole);
		saveAccount(newDetail);
		return true;
	}
	
	
	@Override
	public boolean updateAccountStats(UserAccount player, AccountStats newStats, final String gameId) throws GameSchedulingException {
		UserAccount acc = getAccount(player.getPlayerId());
		if (acc == null) {
			/* The player didn't organise, just played. This should just be a full back
			 * as accounts are created as required when /session is accessed from the client */
			acc = player;
		}
		if (acc.getStats() !=null) {
			acc.getStats().updateStats(newStats, gameId);
		}

		try {
			return accProvider.storeAccount(acc);
		} catch (IOException e) {
			logger.error("Updating user account", e);
		}
		
		return false;
	}
	

	@Override
	public String deleteAccount(String userId) throws IOException {
		return accProvider.deleteAccountById(userId);
	}
	
	/** Count the number of games in the last 30 days
	 * 
	 * @param scheduled
	 * @return The number of games scheduled, including the planned game
	 */
	long getScheduledGameCount(final Map<String, Long> scheduled, final long nextSched) {

		// the last scheduled game
		final LocalDate nextLD = millisToLocalDate(nextSched);

		// The timestamp of next game less 30 days
		final long nextMinus30 = nextLD.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		// count all games in this period
		return scheduled.values().stream()
			.filter(s -> s > nextMinus30)
			.count() + 1;// Add the next game to the count
	}

	/** Check if an authorised user has any of the supplied roles
	 * 
	 * @param authUser The userId to check
	 * @param roles The roles to validate
	 * @return True if they do, otherwise false
	 */
	@Override
	public boolean hasRolesAnyOf(final String authUser, final ROLE... roles) {
		if (StringUtils.isNoneBlank(authUser)) {
			try {
				UserAccount ua = getAccount(authUser);
				if (ua !=null && ua.getRoles() != null) {
					for (ROLE r : roles) {
						if (ua.getRoles().contains(r)) {
							return true;
						}
					}
				}
			} catch (GameSchedulingException e) {
				logger.debug("Checking auth user roles: ", e);
			}
		}
		return false;
	}
	
	/** Check if an authorised user has all of the provided roles
	 * 
	 * @param authUser The userId to check
	 * @param roles The roles to validate
	 * @return True if they do, otherwise false
	 */
	@Override
	public boolean hasRoles(final String authUser, final ROLE... roles) {
		if (StringUtils.isNotBlank(authUser)) {
			try {
				UserAccount ua = getAccount(authUser);
				if (ua !=null && ua.getRoles() != null) {
					return ua.getRoles().containsAll(Arrays.asList(roles));
				}
			} catch (GameSchedulingException e) {
				logger.debug("Checking auth user roles: ", e);
			}
		}
		return false;
	}
	/** Convert milliseconds date to LocalDate
	 * 
	 * @param millisSinceEpoch
	 * @return
	 */
	private static LocalDate millisToLocalDate(long millisSinceEpoch) {
	    return Instant.ofEpochMilli(millisSinceEpoch)
	            .atOffset(ZoneOffset.UTC)
	            .toLocalDate();
	}
}
