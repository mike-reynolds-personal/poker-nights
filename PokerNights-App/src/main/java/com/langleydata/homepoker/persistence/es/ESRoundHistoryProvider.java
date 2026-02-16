package com.langleydata.homepoker.persistence.es;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.game.RoundHistory;
import com.langleydata.homepoker.persistence.RoundHistoryProvider;

@Service
@Profile(value = {"test", "prod"})
public class ESRoundHistoryProvider implements RoundHistoryProvider {
	final Logger logger = LoggerFactory.getLogger(ESRoundHistoryProvider.class);
	
	@Autowired
	private RoundHistoryRepository roundHistoryRepo;
	
	ESRoundHistoryProvider() {
		logger.info("Using Elasticsearch Round History Provider");
	}
	/** Get a list of all rounds for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	@Override
	public List<RoundHistory> getGameRounds(String gameId) {
		return roundHistoryRepo.findByGameId(gameId)
				.stream()
				.sorted(Comparator.comparing(RoundHistory::getRound, Comparator.naturalOrder()))
				.collect(Collectors.toList());
	}
	
	/** Get a single round within a specific game
	 *  
	 * @param gameId
	 * @param round
	 * @return
	 */
	@Override
	public RoundHistory getGameRound(final String gameId, final int round) {
		List<RoundHistory> rh = roundHistoryRepo.findByGameIdAndRound(gameId, round);
		if (rh.size() > 0) {
			return rh.get(0);
		} else {
			return null;
		}
	}

	/** Store a new RoundHistory object in the repository
	 * 
	 * @param round
	 * @return True if added
	 */
	@Override
	public boolean addGameRound(final RoundHistory round) {
		/* Ideally we'd put this on a message broker and then pull off
		 * again in a seperate thread (if performance an issue).
		 * Don't use a new thread as the objects get reset part way through saving
		 */
		try {
			roundHistoryRepo.save(round);
			return true;
		} catch (Exception e) { 
			logger.warn("Saving game round: {}", e.getMessage());
		}
		return false;
	}
	
	@Override
	public long deleteGameRounds(String gameId) {
		try {
			return roundHistoryRepo.deleteBygameId(gameId);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return 0;
		}
	}
	
	@Override
	public List<String> getGameIds() {
		return StreamSupport.stream(roundHistoryRepo.findAll().spliterator(), false)
				.map(RoundHistory::getGameId)
				.distinct()
				.collect(Collectors.toList());
	}
}
