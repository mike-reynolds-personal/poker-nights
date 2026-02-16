package com.langleydata.homepoker.api.session;

import com.langleydata.homepoker.services.Account;

/** User session and token information provided by the MainSiteController
 *  
 *
 */
public class UserSession extends Account {
	private TokenInfo tokens;
	
	/**
	 * 
	 * @param acc
	 */
	public UserSession(Account acc) {
		super(acc.getPlayerId(), acc.getPlayerHandle());
		email = acc.getEmail();
		givenName = acc.getGivenName();
		familyName = acc.getFamilyName();
		fullName = acc.getFullName();
		locale = acc.getLocale();
		picture = acc.getPicture();
		accLevel = acc.getAccLevel();
		accStats = acc.getStats();
		roles = acc.getRoles();
	}

	public UserSession(String playerId, String playerHandle) {
		super(playerId, playerHandle);
	}

	/**
	 * @return the available tokens, or null
	 */
	public TokenInfo getTokens() {
		return tokens;
	}
	
	/**
	 * 
	 * @param tokenInfo
	 */
	public void setTokens(final TokenInfo tokenInfo) {
		this.tokens = tokenInfo;
	}

	public UserSession clearStats() {
		this.accStats = null;
		return this;
	}
}
