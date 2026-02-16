package com.langleydata.homepoker.persistence;

import java.util.List;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;

/** An interface to provide saved game settings from a settings provider
 * 
 * @author reynolds_mj
 *
 */
public interface SettingsProvider {

	/** Get the count of game settings stored
	 * 
	 * @return
	 */
	long getSettingsCount();
	/** Retrieve game settings 
	 * 
	 * @param gameId
	 * @return
	 */
	GameSettings retrieveSettings(final String gameId);
	
	/** Get all settings currently stored
	 * 
	 * @param page which page to get, starting at 0
	 * @param size the size of each page of results
	 * @return
	 */
	public List<GameSettings> retrieveSettings(int page, int size);
	
	/** Persist the settings submitted by a user creating a game
	 * 
	 * @param settings
	 * @return
	 */
	boolean storeSettings(TexasHoldemSettings settings);
	
	/** Forcebly remove game settings matching the provided id
	 * 
	 * @param gameId
	 * @return
	 */
	long deleteSettings(final String gameId);
	
	/** Remove game settings
	 * 
	 */
	long deleteByGameId(final String gameId);
	
	/** Get all games that have not been played
	 * 
	 * @return
	 */
	List<GameSettings> findNotPlayed();
}
