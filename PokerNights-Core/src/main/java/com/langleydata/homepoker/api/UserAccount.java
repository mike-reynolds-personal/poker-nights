package com.langleydata.homepoker.api;

import java.util.Set;

import org.springframework.data.annotation.Id;

/** An interface for a PokerRoomsNow user account
 * 
 *
 */
public interface UserAccount {
	public enum ROLE {
		Player, Admin, Integration
	}
	public static final String AUTH_USER_HEADER = "authenticated-user";
	
	/** The ID taken from the OAuth2 provider ID for the user
	 * 
	 * @return
	 */
	@Id
	String getPlayerId();
	
	/** The user's nickname
	 * 
	 * @return
	 */
	String getPlayerHandle();
	
	/** By default this is ignored in JSON responses and returns null
	 * 
	 * @return the givenName
	 */
	String getGivenName();

	/** By default this is ignored in JSON responses and returns null
	 * 
	 * @return the familyName
	 */
	String getFamilyName();

	/** By default this is ignored in JSON responses and returns null
	 * 
	 * @return the fullName
	 */
	String getFullName();

	/** By default this is ignored in JSON responses and returns null
	 * 
	 * @return the locale
	 */
	String getLocale();

	/** The profile picture, if set
	 * 
	 * @return
	 */
	String getPicture();
	
	/** The User's registered email
	 * 
	 * @return
	 */
	String getEmail();
	
	/** The PKR account level
	 * 
	 * @return
	 */
	AccountLevel getAccLevel();

	/** The player's stats
	 * 
	 * @return
	 */
	AccountStats getStats();
	
	/** Is this account blocked from playing?
	 * 
	 * @return
	 */
	boolean isBlocked();
	/** The internal roles we've set for the user
	 * 
	 * @return
	 */
	Set<ROLE> getRoles();
	
	/** Get the email addresses of players this player has played with
	 * 
	 * @return
	 */
	Set<String> getKnownAssociates();
}
