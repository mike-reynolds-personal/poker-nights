package com.langleydata.homepoker.persistence;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.exception.NoKnownServerException;

@Component
public interface GenericSettingsProvider {

	/**
	 * Store settings
	 * 
	 * @param gameId
	 * @param document
	 * @return True if updated or created
	 * @throws IOException
	 */
	boolean storeSettings(String gameId, Map<String, Object> document) throws IOException;

	/** Get the account of game settings
	 * 
	 * @return
	 * @throws IOException
	 */
	public long getSettingsCount() throws IOException;
	/**
	 * Get settings
	 * 
	 * @param gameId
	 * @return
	 * @throws NoKnownServerException
	 * @throws IOException
	 */
	Map<String, Object> getSettingsById(String gameId) throws IOException;


	/** Get the number of accounts indexed
	 * 
	 * @return
	 * @throws IOException
	 */
	public long getAccountCount() throws IOException;
	/**
	 * 
	 * @param userAccount
	 * @return
	 * @throws IOException
	 */
	boolean storeAccount(UserAccount userAccount) throws IOException;
	
	/** Delete a user account by the account id
	 * 
	 * @param userId
	 * @return The ID of the deleted document
	 * @throws IOException
	 */
	String deleteAccountById(final String userId) throws IOException;
	
	/** Get the Json string representation of the UserAccount
	 * 
	 * @param userId
	 * @return The json string, or null if it doesn't exist
	 * @throws IOException
	 */
	String getAccountById(final String userId) throws IOException;
	
	/** Get a list of all accounts
	 * 
	 * @param page 0 based index for the page
	 * @param pageSize The size of each page
	 * @return
	 */
	List<UserAccount> getAllAccounts(final int page, final int pageSize);
}