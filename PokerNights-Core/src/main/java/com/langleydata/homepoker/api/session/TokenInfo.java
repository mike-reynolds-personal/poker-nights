package com.langleydata.homepoker.api.session;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** User token information supplied to the client when it calls getSession()
 * 
 */
public class TokenInfo {

	private Token accessToken = new Token("", 0L, null);
	private Token refreshToken = new Token("", 0L, null);

	/**
	 * @return the access token
	 */
	public Token getAccessToken() {
		return accessToken;
	}
	
	/**
	 * @return the refresh token
	 */
	@JsonIgnore
	public Token getRefreshToken() {
		return refreshToken;
	}

	/** Do we have a refresh token?
	 * 
	 * @return
	 */
	@JsonProperty
	public boolean hasRefreshToken() {
		return refreshToken!=null && StringUtils.isNotBlank(refreshToken.getToken());
	}
	/** 
	 * 
	 * @param accessToken
	 */
	public void setAccessToken(final OAuth2AccessToken accessToken) {
		if (accessToken == null) {
			return;
		}
		this.accessToken = new Token(
					accessToken.getTokenValue(),
					accessToken.getExpiresAt().toEpochMilli(),
					accessToken.getScopes());
	}
	/**
	 * 
	 * @param refreshToken
	 */
	public void setRefreshToken(final OAuth2RefreshToken refreshToken) {
		if (refreshToken == null) {
			return;
		}
		final Instant expires = refreshToken.getExpiresAt();
		this.refreshToken = new Token(
				refreshToken.getTokenValue(),
				expires != null ? expires.toEpochMilli() : null,
				null);
	}

	/** Build a new access and refresh tokens from an Okta based TokenResponse
	 * 
	 * @param tokenResponse
	 */
	public void setTokens(final TokenResponse tokenResponse) {
		if (tokenResponse==null) {
			return;
		}
		final Set<String> scopes = new HashSet<>( Arrays.asList(StringUtils.split(tokenResponse.getScope(), " ")) );
		final long expires = (System.currentTimeMillis() - 10000) + (tokenResponse.getExpiresIn() * 1000);
		
		this.accessToken = new Token(tokenResponse.getAccessToken(), expires, scopes);
		this.refreshToken = new Token(tokenResponse.getRefreshToken(), null, null);
	}

	/** POJO for holding token information
	 * 
	 *
	 */
	public class Token {
		private final String tokenValue;
		private final Long expiresAt;
		private final Set<String> scopes;
		
		/**
		 * 
		 * @param tokenValue
		 * @param expiresAt
		 * @param scopes
		 */
		public Token(String tokenValue, Long expiresAt, Set<String> scopes) {
			this.tokenValue = tokenValue;
			this.expiresAt = expiresAt;
			this.scopes = scopes;
		}
		
		/**
		 * 
		 * @return the refresh token
		 */
		public String getToken() {
//			final int firstDotPos = tokenValue.indexOf(".");
//			if (firstDotPos == -1)
//				throw new IllegalArgumentException("Invalid JWT serialization: Missing dot delimiter(s)");
//			
//			return Base64.encodeBase64String(tokenValue.getBytes());
			return tokenValue;
		}
		/**
		 * @return the expiresAt
		 */
		public Long getExpiresAt() {
			return expiresAt;
		}
		/**
		 * @return the scopes
		 */
		public Set<String> getScopes() {
			return scopes;
		}
	}
}