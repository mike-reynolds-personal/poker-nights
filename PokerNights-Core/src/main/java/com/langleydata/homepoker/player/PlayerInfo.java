package com.langleydata.homepoker.player;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.AccountStats;
import com.langleydata.homepoker.api.UserAccount;

/** Abstract class for the minimum information for a player
 * 
 * @author reynolds_mj
 *
 */
public abstract class PlayerInfo implements UserAccount {
	private final String playerId, playerHandle;
	@Transient
	protected String picture;
	protected String email;
	protected AccountLevel accLevel;
	protected AccountStats accStats;
	protected boolean blocked = false;
	@Transient
	protected Set<ROLE> roles = new HashSet<>();
	/**
	 * 
	 * @param playerId
	 * @param playerHandle
	 */
	protected PlayerInfo(final String playerId, final String playerHandle) {
		this.playerId = playerId;
		this.playerHandle = playerHandle;
	}

	/**
	 * @return the playerHandle
	 */
	@Override
	public String getPlayerHandle() {
		return playerHandle==null ? "" : new String(playerHandle);
	}

	/**
	 * @return the playerId
	 */
	@Override
	public String getPlayerId() {
		return playerId == null ? "" : new String(playerId);
	}
	
	/**
	 * @return the picture
	 */
	@Override
	public String getPicture() {
		return picture == null ? "" : new String(picture);
	}

	@Override
	public Set<ROLE> getRoles() {
		if (roles == null) {
			roles = new HashSet<>();
		}
		return roles;
	}

	@JsonIgnore
	@Override
	public boolean isBlocked() {
		return blocked;
	}
	
	@Override
	public Set<String> getKnownAssociates() {
		return null;
	}
	/**
	 * @param picture the picture to set
	 */
	public void setPicture(String picture) {
		this.picture = picture;
	}

	/**
	 * @return the email
	 */
	@Override
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	
	@Override
	public AccountLevel getAccLevel() {
		return accLevel;
	}

	@Override
	public AccountStats getStats() {
		return accStats;
	}
	
	@Override
	public String getGivenName() {
		return null;
	}

	@Override
	public String getFamilyName() {
		return null;
	}

	@Override
	public String getFullName() {
		return null;
	}

	@Override
	public String getLocale() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((playerHandle == null) ? 0 : playerHandle.hashCode());
		result = prime * result + ((playerId == null) ? 0 : playerId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}

		PlayerInfo other = (PlayerInfo) obj;
		if (playerHandle == null) {
			if (other.playerHandle != null) {
				return false;
			}
		} else if (!playerHandle.equals(other.playerHandle)) {
			return false;
		}
		if (playerId == null) {
			if (other.playerId != null) {
				return false;
			}
		} else if (!playerId.equals(other.playerId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerInfo [playerId=");
		builder.append(playerId);
		builder.append(", playerHandle=");
		builder.append(playerHandle);
		builder.append(", picture=");
		builder.append(picture);
		builder.append("]");
		return builder.toString();
	}

}
