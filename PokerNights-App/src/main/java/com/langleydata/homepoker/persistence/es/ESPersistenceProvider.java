package com.langleydata.homepoker.persistence.es;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.api.Feedback;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.persistence.FeedbackProvider;
import com.langleydata.homepoker.persistence.MessageHistoryProvider;
import com.langleydata.homepoker.persistence.SettingsProvider;

/** A service for storing and retrieving game settings and Feedback forms
 * 
 * @author reynolds_mj
 * @param <T>
 *
 */
@Service
@Profile(value = {"test", "prod"})
public class ESPersistenceProvider implements SettingsProvider, FeedbackProvider, MessageHistoryProvider {
	final Logger logger = LoggerFactory.getLogger(ESPersistenceProvider.class);

	
	@Autowired
	private SettingsRepository settingsRepo;

	@Autowired
	private FeedbackRepository feedbackRepo;
	
	@Autowired
	private MessageHistoryRepository playerActionRepo;
	
	ESPersistenceProvider() {
		logger.info("Using Elasticsearch Storage Provider");
	}
	
	/** Retrieve game settings by the game id
	 * 
	 * @param gameId
	 * @return
	 */
	@Override
	public GameSettings retrieveSettings(final String gameId) {
		return settingsRepo.findById(gameId).orElse(null);
	}
	
	@Override
	public List<GameSettings> retrieveSettings(final int page, final int size) {
		List<GameSettings> ret = new ArrayList<>();
		final PageRequest pr = PageRequest.of(page, size, Sort.by(Direction.DESC, "scheduledTime"));
		settingsRepo.findAll(pr).forEach(s -> ret.add(s) );
		return ret;
	}

	/** Store game settings 
	 * 
	 * @param settings
	 * @return
	 */
	@Override
	public boolean storeSettings(final TexasHoldemSettings settings) {
		try {
			settingsRepo.save(settings);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return true;
	}

	@Override
	public long deleteSettings(final String gameId) {
		try {
			return settingsRepo.deleteByGameId(gameId);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return 0;
		}
	}

	@Override
	public List<Feedback> getFeedback(String gameId) {
		return feedbackRepo.findByGameId(gameId);
	}

	@Override
	public boolean deleteFeedback(String id) {
		try {
			feedbackRepo.deleteById(id);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public List<Feedback> getFeedback(final int page, final int size) {
		List<Feedback> ret = new ArrayList<>();
		final PageRequest pr = PageRequest.of(page, size, Sort.by(Direction.DESC, "created"));
		feedbackRepo.findAll(pr).forEach(s -> ret.add(s) );
		return ret;
	}

	@Override
	public boolean storeFeedback(Feedback feedback) {
		try {
			feedbackRepo.save(feedback);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return true;
	}


	@Override
	public List<PlayerAction> getPlayerActions(final String gameId) {
		return playerActionRepo.findByGameId(gameId);
	}

	@Override
	public List<PlayerAction> getPlayerActions(final String gameId, final int round) {
		return playerActionRepo.findByGameIdAndRound(gameId, round);
	}

	@Override
	public List<PlayerAction> getPlayerActions(final String gameId, final String playerId) {
		return playerActionRepo.findByGameIdAndPlayerId(gameId, playerId);
	}

	@Override
	public boolean addPlayerAction(final PlayerActionMessage action) {
		new Thread(()-> {
			try {
				playerActionRepo.save(action);
			} catch (Exception e) {
				logger.warn("Saving player action: {}", e.getMessage());
			}
		}).start();
		return false;
	}

	@Override
	public long deletePlayerActions(final String gameId) {
		try {
			return playerActionRepo.deleteBygameId(gameId);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return 0;
		}
	}

	@Override
	public long deleteByGameId(String gameId) {
		return settingsRepo.deleteByGameId(gameId);
	}

	@Override
	public List<GameSettings> findNotPlayed() {
		return settingsRepo.findNotPlayed();
	}

	@Override
	public long getSettingsCount() {
		return settingsRepo.count();
	}

	@Override
	public long getFeedbackCount() {
		return feedbackRepo.count();
	}

}
