package com.langleydata.homepoker.persistence.mem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.persistence.GenericSettingsProvider;
import com.langleydata.homepoker.services.Account;

/** Memory backed generic settings provider
 * 
 * @author Mike Reynolds
 *
 */
@Profile( value = {"dev"})
@Component
public class MemoryGenericSettingsProvider implements GenericSettingsProvider {
	private Map<String, Map<String, Object>> gameSettings = new HashMap<>();
	private Map<String, String> userAccounts = new HashMap<>();
	private Gson gson = new Gson();
	
	@Override
	public boolean storeSettings(String gameId, Map<String, Object> document) throws IOException {
		gameSettings.put(gameId, document);
		return true;
	}

	@Override
	public Map<String, Object> getSettingsById(String gameId) throws IOException {
		return gameSettings.get(gameId);
	}

	@Override
	public boolean storeAccount(UserAccount document) throws IOException {
		userAccounts.put(document.getPlayerId(), gson.toJson(document));
		return true;
	}

	@Override
	public String getAccountById(String userId) throws IOException {
		return userAccounts.get(userId);
	}

	@Override
	public List<UserAccount> getAllAccounts(int page, int pageSize) {
		return userAccounts.values().stream()
			.map(v ->  Account.buildAccountInfo(v) )
			.collect(Collectors.toList());
	}

	@Override
	public String deleteAccountById(String userId) throws IOException {
		String acc = userAccounts.remove(userId);
		if (acc !=null) {
			final UserAccount uAcc = gson.fromJson(acc, UserAccount.class);
			return uAcc.getPlayerId();
		}
		return null;
	}

	@Override
	public long getSettingsCount() throws IOException {
		return gameSettings.size();
	}

	@Override
	public long getAccountCount() throws IOException {
		return userAccounts.size();
	}

}
