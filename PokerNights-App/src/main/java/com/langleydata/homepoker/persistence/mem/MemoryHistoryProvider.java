package com.langleydata.homepoker.persistence.mem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.game.RoundHistory;
import com.langleydata.homepoker.persistence.RoundHistoryProvider;

/** An in-memory provider for historic game information
 * 
 * @author Mike Reynolds
 *
 */
@Service
@Profile("dev")
public class MemoryHistoryProvider implements RoundHistoryProvider {
	private final Map<String, List<RoundHistory>> roundHistory = new HashMap<>();
	
	
	@Override
	public List<RoundHistory> getGameRounds(String gameId) {
		List<RoundHistory> gh = roundHistory.get(gameId);
		return gh == null ? Collections.emptyList() : gh;
	}

	@Override
	public RoundHistory getGameRound(final String gameId, final int round) {
		List<RoundHistory> gh = roundHistory.get(gameId);
		if (gh==null) {
			return null;
		}
		return gh.stream()
			.filter(h -> h.getRound()==round)
			.findFirst()
			.orElse(null);
		
	}

	@Override
	public boolean addGameRound(RoundHistory round) {
		List<RoundHistory> gh = roundHistory.get(round.getGameId());
		if (gh==null) {
			gh = new ArrayList<>();
		}
		gh.add(round);
		roundHistory.put(round.getGameId(), gh);
		return true;
	}

	@Override
	public long deleteGameRounds(String gameId) {
		return roundHistory.remove(gameId) !=null ? 1 : 0;
	}

	@Override
	public List<String> getGameIds() {
		return new ArrayList<>(roundHistory.keySet());
	}

}
