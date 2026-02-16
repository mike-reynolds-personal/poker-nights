package com.langleydata.homepoker.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.AccountStats;
import com.langleydata.homepoker.player.PlayerInfo;
import com.langleydata.homepoker.player.PlayerStats;

/** A player's account information
 * 
 * @author Mike Reynolds
 *
 */
public class Account extends PlayerInfo {
	private static final String STATS_FIELD = "accStats";
	protected String givenName, familyName, fullName, locale;
	protected Set<String> knownAssocs = new HashSet<>();
	
	protected Account(String playerId, String playerHandle) {
		super(playerId, playerHandle);
	}

	protected void setStats(AccountStats stats) {
		this.accStats = stats;
	}

	public void setAccLevel(AccountLevel level) {
		this.accLevel = level;
	}

	@Override
	public Set<ROLE> getRoles() {
		return roles;
	}
	
	/** Set or unset if the player is blocked
	 * 
	 * @param block
	 */
	public void setIsBlocked(boolean block) {
		this.blocked = block;
	}
	
	@JsonProperty
	public boolean getIsBlocked() {
		return super.isBlocked();
	}

	/** Add a new role to the user account
	 * 
	 * @param role
	 */
	public void addRole(ROLE role) {
		if (roles==null) {
			roles = new HashSet<>();
		}
		if (role!=null) {
			roles.add(role);
		}
	}
	
	/** Remove a specific role from the user account
	 * 
	 * @param role
	 */
	public void removeRole(ROLE role) {
		roles.remove(role);
	}
	
	/** Add a new associate to the user account
	 * 
	 * @param associate
	 */
	public void addKnownAssociate(String associate) {
		if (knownAssocs==null) {
			knownAssocs = new HashSet<>();
		}
		if (StringUtils.isNotBlank(associate)) {
			knownAssocs.add(associate);
		}
	}
	
	@Override
	public Set<String> getKnownAssociates() {
		return knownAssocs;
	}
	/** Remove a specific role from the user account
	 * 
	 * @param role
	 */
	public void removeKnownAssociate(String associate) {
		knownAssocs.remove(associate);
	}

	/**
	 * @return the givenName
	 */
	@Override
	@JsonProperty
	public String getGivenName() {
		return givenName;
	}

	/**
	 * @return the familyName
	 */
	@Override
	@JsonProperty
	public String getFamilyName() {
		return familyName;
	}

	/**
	 * @return the fullName
	 */
	@Override
	@JsonProperty
	public String getFullName() {
		return fullName;
	}

	/**
	 * @return the locale
	 */
	@Override
	@JsonProperty
	public String getLocale() {
		return locale;
	}

	/**
	 * Build an Account object from a Json representation
	 * 
	 * @param json
	 * @return
	 */
	public static Account buildAccountInfo(final String json) {
		Account acc = null;
		final Gson gson = new Gson();
		
		if (StringUtils.isNotBlank(json)) {
			JsonObject jo = gson.fromJson(json, JsonObject.class);
			final String stats = jo.getAsJsonObject(STATS_FIELD).toString();

			if (StringUtils.isNotBlank(stats)) {
				AccountStats gs = gson.fromJson(stats, StoredStats.class);
				jo.remove(STATS_FIELD);
				acc = gson.fromJson(jo, Account.class);
				acc.setStats(gs);
			}
		}
		return acc;
	}

	/** To help building a User Account
	 * 
	 */
	public static class Builder {
		Account acc = null;
		/**
		 * 
		 * @param playerId
		 * @param playerHandle
		 */
		public Builder(String playerId, String playerHandle) {
			acc = new Account(playerId, playerHandle);
			acc.accStats = new StoredStats();
		}
		public Builder accLevel(AccountStats stats) {
			acc.accStats = stats;
			return this;
		}
		public Builder accLevel(AccountLevel level) {
			acc.accLevel = level;
			return this;
		}
		public Builder email(String email) {
			acc.setEmail(email);
			return this;
		}
		public Builder picture(String picture) {
			acc.setPicture(picture);
			return this;
		}
		public Builder fullName(String fullName) {
			acc.fullName = fullName;
			return this;
		}
		public Builder givenName(String givenName) {
			acc.givenName = givenName;
			return this;
		}
		public Builder familyName(String familyName) {
			acc.familyName = familyName;
			return this;
		}
		public Builder locale(String locale) {
			acc.locale = locale;
			return this;
		}
		public Builder role(ROLE role) {
			acc.addRole(role);
			return this;
		}
		public Builder associate(String associate) {
			acc.addKnownAssociate(associate);
			return this;
		}
		public Builder associates(List<String> associates) {
			if (associates!=null) {
				acc.knownAssocs = new HashSet<>(associates);
			}
			return this;
		}
		public Account build() {
			return acc;
		}
	}

	private static class StoredStats extends PlayerStats {
	}


}