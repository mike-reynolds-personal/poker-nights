package com.langleydata.homepoker.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.controllers.TableController;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.persistence.SettingsProvider;


/** A service for scheduled clearing-down and tidying up of memory and storage
 * 
 * @author Mike Reynolds
 *
 */
@Service
public class GameMaintenance {
	final Logger logger = LoggerFactory.getLogger(GameMaintenance.class);
	final static long PLAY_DELAY = 24 * 60 * 60 * 1000;
	static final long PURGE_INACTIVE_TIME = 30 * 60 * 1000;
	
	@Autowired
	private SettingsProvider settingProvider;
	@Autowired
	private TableController tableController;
	
	/** Remove any old settings that haven't been used
	 * 
	 * @return True if successful
	 */
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	public boolean deleteUnplayedGames() {
		logger.info("Purging old game settings from storage");
		
		/* This wants to check if the game was ever active/ played, and
		 * if so, flag the settings as in-accessible. If the settings have
		 * never been used then they should be deleted after 24 hours of the
		 * scheduled start time. */
		final long now = System.currentTimeMillis();
		try {
			List<GameSettings> settings = settingProvider.findNotPlayed();
			for (GameSettings gs : settings) {
				if (gs.getScheduledTime() < (now-PLAY_DELAY) && gs.isArchived()==false) {
					settingProvider.deleteByGameId(gs.getGameId());
				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return false;
		}
		return true;
	}
	
	/** Purge games that are in memory, but are no longer being played
	 *  
	 */
	@Scheduled(fixedDelay = 10 * 60 * 1000)
	public void purgeOrphanedGamesFromController() {
		long oldestActivity = System.currentTimeMillis() - PURGE_INACTIVE_TIME;
		List<AbstractCardGame<?>> purge = tableController.getActiveGames().values().stream()
			.filter(g -> g.getLastActivityTime() <= oldestActivity)
			.collect(Collectors.toList());
		
		logger.debug("Purging {} active games from memory", purge.size());
		
		purge.forEach(game-> {
			final GameSettings settings = game.getSettings();
			tableController.getActiveGames().remove(settings.getGameId());
			if (game.getRound() > 0) {
				settings.setCompleted();
				settingProvider.storeSettings((TexasHoldemSettings) settings);
			}
		});
	}
}
