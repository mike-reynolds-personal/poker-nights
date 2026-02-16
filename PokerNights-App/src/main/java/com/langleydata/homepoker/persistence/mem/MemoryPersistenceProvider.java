package com.langleydata.homepoker.persistence.mem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.api.Feedback;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.persistence.FeedbackProvider;
import com.langleydata.homepoker.persistence.MessageHistoryProvider;
import com.langleydata.homepoker.persistence.SettingsProvider;

/** For easier testing, this class provides an in-memory repository of game history - 
 * the game rounds and player actions, settings and Feedback forms
 *
 */
@Service
@Profile("dev")
public class MemoryPersistenceProvider implements SettingsProvider, FeedbackProvider, MessageHistoryProvider {
	final Logger logger = LoggerFactory.getLogger(MemoryPersistenceProvider.class);

	private final Map<String, GameSettings> storedSettings = new HashMap<>();
	private final Map<String, Feedback> feedbackForms = new HashMap<>();
	private final Map<String, List<PlayerAction>> actionHistory = new HashMap<>();
	
	MemoryPersistenceProvider() {
		logger.info("Using Memory Storage Provider");
	}


	@Override
	public GameSettings retrieveSettings(String gameId) {
		return storedSettings.get(gameId);
	}

	@Override
	public boolean storeSettings(TexasHoldemSettings settings) {
		
		storedSettings.put(settings.getGameId(), settings);
		
		return true;
	}

	@Override
	public List<GameSettings> retrieveSettings(int page, int pageSize) {
		return new ArrayList<>(storedSettings.values());
	}

	@Override
	public long deleteSettings(String gameId) {
		return storedSettings.remove(gameId) !=null ? 1 : 0;
	}

	@Override
	public List<Feedback> getFeedback(String gameId) {
		return feedbackForms.values().stream()
			.filter(f -> f.getGameId().equals(gameId))
			.collect(Collectors.toList());
	}

	@Override
	public List<Feedback> getFeedback(int page, int pageSize) {
		return new ArrayList<>(feedbackForms.values());
	}

	@Override
	public boolean storeFeedback(Feedback feedback) {
		feedbackForms.put(feedback.getMessageId(), feedback);
		return true;
	}
	
	@Override
	public boolean deleteFeedback(String id) {
		return feedbackForms.remove(id) != null;
	}
	
	@Override
	public List<PlayerAction> getPlayerActions(final String gameId) {
		return actionHistory.get(gameId);
	}

	@Override
	public List<PlayerAction> getPlayerActions(final String gameId, final int round) {
		List<PlayerAction> ah = actionHistory.get(gameId);
		final List<PlayerAction> inRound = ah.stream()
				.filter(a -> a.getRound()==round)
				.collect(Collectors.toList());
		return inRound.stream()
				.sorted(Comparator.comparing(PlayerAction::getTimestamp, Comparator.naturalOrder()))
				.collect(Collectors.toList());
	}

	@Override
	public List<PlayerAction> getPlayerActions(final String gameId, final String playerId) {
		List<PlayerAction> ah = actionHistory.get(gameId);
		final List<PlayerAction> inRound = ah.stream()
				.filter(a -> a.getPlayerId().equals(playerId))
				.collect(Collectors.toList());
		return inRound.stream()
				.sorted(Comparator.comparing(PlayerAction::getTimestamp, Comparator.naturalOrder()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean addPlayerAction(final PlayerActionMessage action) {
		List<PlayerAction> ah = actionHistory.get(action.getGameId());
		if (ah==null) {
			ah = new ArrayList<>();
		}
		ah.add(action);
		actionHistory.put(action.getGameId(), ah);
		return true;
	}

	@Override
	public long deletePlayerActions(final String gameId) {
		final List<PlayerAction> removed = actionHistory.remove(gameId);
		if (removed!=null) {
			return removed.size();
		}
		return 0L;
	}


	@Override
	public long deleteByGameId(String gameId) {
		if (storedSettings.remove(gameId) !=null) {
			return 1L;
		} else {
			return 0L;
		}
	}

	@Override
	public List<GameSettings> findNotPlayed() {
		return Collections.emptyList();
	}


	@Override
	public long getSettingsCount() {
		return storedSettings.size();
	}

	@Override
	public long getFeedbackCount() {
		return feedbackForms.size();
	}

}
