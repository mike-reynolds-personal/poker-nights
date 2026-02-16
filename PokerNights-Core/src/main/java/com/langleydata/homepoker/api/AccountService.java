package com.langleydata.homepoker.api;

import java.io.IOException;
import java.util.List;

import com.langleydata.homepoker.api.UserAccount.ROLE;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.services.Account;

/** A service that provides player account information
 * 
 */
public interface AccountService {
	
	/** Get the number of accounts stored
	 * 
	 * @return
	 */
	long getAccountCount();
	
	/** Get all currently stored user accounts
	 * 
	 * @param page The page index, 0 based
	 * @param pageSize The number of records in a page
	 * @return
	 */
	List<UserAccount> getAccounts(final int page, final int pageSize);
	
	/** Get a user account based on their persistent ID
	 * 
	 * @param userId
	 * @return The account info or null if not found
	 * @throws GameSchedulingException If there is an error getting the account
	 */
	UserAccount getAccount(String userId) throws GameSchedulingException;

	/** Delete a user's account
	 * 
	 * @param userId
	 * @return The account ID that was deleted, or null
	 */
	String deleteAccount(String userId)  throws IOException;
	
	/** Add a new UserAccount
	 * 
	 * @param user
	 * @return
	 */
	boolean saveAccount(UserAccount user) throws GameSchedulingException;
	
	/** Validate the user is allowed to schedule the provided game
	 * 
	 * @param userId The user to validate
	 * @param gameSettings The game settings provided
	 * @return True if they can
	 * @throws GameSchedulingException The reason they can't schedule the game
	 */
	void canScheduleGame(String userId, GameSettings gameSettings) throws GameSchedulingException;
	
	/** Update the account game statistics
	 * 
	 * @param player The playerInfo
	 * @param newStats
	 * @param gameId The current game id
	 * @return
	 */
	boolean updateAccountStats(UserAccount player, AccountStats newStats, String gameId) throws GameSchedulingException;
	
	/** Check if an authorised user has any of the supplied roles
	 * 
	 * @param authUser The userId to check
	 * @param roles The roles to validate
	 * @return True if they do, otherwise false
	 */
	boolean hasRolesAnyOf(final String authUser, final ROLE... roles);
	
	/** Check if an authorised user has all of the provided roles
	 * 
	 * @param authUser The userId to check
	 * @param roles The roles to validate
	 * @return True if they do, otherwise false
	 */
	boolean hasRoles(final String authUser, final ROLE... roles);
	
	/** Update an existing account, if it exists, to the new account information.
	 *  Only updated the account if newDetail.equals(existingAcc) == false
	 *  
	 * @param existingAcc The existing account to update
	 * @param newDetail
	 * @return True if the account was updated, otherwise false
	 */
	boolean updateAccountProfile(UserAccount existingAcc, Account newDetail) ;
}
