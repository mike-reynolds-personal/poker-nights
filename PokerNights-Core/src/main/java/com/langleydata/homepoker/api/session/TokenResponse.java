package com.langleydata.homepoker.api.session;

public class TokenResponse {
	private String token_type = "";
	private long expires_in = 0;
	private String access_token = "";
	private String scope = "";
	private String refresh_token = "";
	private String id_token = "";
	/**
	 * @return the token_type
	 */
	public String getTokenType() {
		return token_type;
	}
	/**
	 * @return the expires_in value in seconds
	 */
	public long getExpiresIn() {
		return expires_in;
	}
	/**
	 * @return the new access_token value
	 */
	public String getAccessToken() {
		return access_token;
	}
	/**
	 * @return the scope
	 */
	public String getScope() {
		return scope;
	}
	/**
	 * @return the refresh_token
	 */
	public String getRefreshToken() {
		return refresh_token;
	}
	/**
	 * @return the id_token
	 */
	public String getIdToken() {
		return id_token;
	}
	
}
