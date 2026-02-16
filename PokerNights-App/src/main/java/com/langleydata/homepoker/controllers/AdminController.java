package com.langleydata.homepoker.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.api.UserAccount.ROLE;
import com.langleydata.homepoker.exception.NoKnownServerException;
import com.langleydata.homepoker.game.RoundHistory;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.persistence.FeedbackProvider;
import com.langleydata.homepoker.persistence.MessageHistoryProvider;
import com.langleydata.homepoker.persistence.RoundHistoryProvider;
import com.langleydata.homepoker.persistence.SettingsProvider;
import com.langleydata.homepoker.services.AbstractGameServiceDiscovery;
import com.langleydata.homepoker.services.SimpleAccountService;

/** A controller for administration functions and retrieving game history

 * @author reynolds_mj
 *
 */
@RestController
public class AdminController {
	private final Logger logger = LoggerFactory.getLogger(AdminController.class);

	private static final long TWENTY_FOUR_HRS = 1000 * 60 * 1440;
	private static final String HISTORY_BASE = "/admin/game/history/";
	private static final String SETTINGS_BASE = "/admin/settings/";
	private static final String FEEDBACK_BACK = "admin/feedback/";
	private static final String ACTIVE_BASE = "admin/activegames/";
	
	@Autowired
	private RoundHistoryProvider historyProvider;
	@Autowired
	private SettingsProvider settingsProvider;
	@Autowired
	private MessageHistoryProvider actionProvider;
	@Autowired
	private SimpleAccountService accountService;
	@Autowired
	private FeedbackProvider feedbackProvider;
	@Autowired
	private AbstractGameServiceDiscovery serviceDiscovery;
	
	@Value("${spring.profiles.active:Unknown}")
	private String activeProfile;
	
	@Autowired
	private Gson gson;

	
	/** Get the count of stored settings
	 * 
	 * @return
	 */
	@GetMapping(path = FEEDBACK_BACK + "count", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getFeedbackCount() {
		final JsonObject resp = new JsonObject();
		resp.addProperty("success", true);
		resp.addProperty("count", feedbackProvider.getFeedbackCount());
		return resp.toString();
	}
	
	/** Get all stored feedback
	 * 
	 * @return
	 */
	@GetMapping(path = FEEDBACK_BACK, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAllFeedback(@RequestHeader(value = UserAccount.AUTH_USER_HEADER, required=false) final String authUser, @RequestParam int page, @RequestParam int pageSize) {
		if (!accountService.hasRoles(authUser, ROLE.Admin)) {
			return "[]";
		}
		return gson.toJson(feedbackProvider.getFeedback(page, pageSize));
	}
	
	/** Delete a feedback form by id
	 * 
	 * @param authUser
	 * @param feedId
	 * @return
	 */
	@DeleteMapping(path = FEEDBACK_BACK + "{feedId}")
	public String deleteFeedback(@RequestHeader(value = UserAccount.AUTH_USER_HEADER, required=false) final String authUser, @PathVariable final String feedId) {
		if (!accountService.hasRoles(authUser, ROLE.Admin)) {
			return "{\"success\":false}";
		}
		return "{\"success\":" + feedbackProvider.deleteFeedback(feedId) + "}";
	}

	
	/** Get all currently active games
	 * 
	 * @param authUser
	 * @return
	 */
	@GetMapping(path = ACTIVE_BASE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getActiveGames(@RequestHeader(value = UserAccount.AUTH_USER_HEADER, required=false) final String authUser) {
		if (!accountService.hasRoles(authUser, ROLE.Admin)) {
			return "[]";
		}
		
		final List<GameServer> servers = serviceDiscovery.lookupServices(GameType.TEXAS_HOLDEM);
		final List<GameServer> fullDetails = servers.stream()
			.map(r -> {
				try {
					return serviceDiscovery.getGameServerInfo(r.getUri());
				} catch (NoKnownServerException | IOException e) {
					logger.error("Getting gameserver details", e);
				}
				return null;
			})
			.collect(Collectors.toList());
		
		return gson.toJson(fullDetails);
	}
	
	/** Get the round history for a game
	 * 
	 * @param gameId
	 * @return
	 */
	@GetMapping(path = HISTORY_BASE + "{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getRoundHistory(@PathVariable final String gameId) {
		return gson.toJson(historyProvider.getGameRounds(gameId));
	}
	
	/** Get a specific round's history for a game
	 * 
	 * @param gameId
	 * @param roundId
	 * @return
	 */
	@GetMapping(path = HISTORY_BASE + "{gameId}/{roundId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getRoundHistory(@PathVariable final String gameId, @PathVariable final int roundId) {
		return gson.toJson(historyProvider.getGameRound(gameId, roundId));
	}

	/** Get the round history for a specific player, within a game
	 * 
	 * @param gameId
	 * @param roundId
	 * @param player
	 * @return
	 */
	@GetMapping(path = HISTORY_BASE + "{gameId}/{roundId}/{player}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getRoundHistory(@PathVariable final String gameId, @PathVariable final int roundId, @PathVariable final String player) {
		final RoundHistory rh = historyProvider.getGameRound(gameId, roundId);
		if (rh == null) {
			return "{\"error\":\"Round not found\"}";
		}
		final Player jp = rh.getPlayers().stream().filter(p -> p.getPlayerId().equals(player)).findFirst().orElse(null);
		if (jp == null) {
			return "{\"error\":\"Player not found\"}";
		}
		return gson.toJson(jp);

	}
	
	/** Delete the round history for a game
	 * 
	 * @param gameId
	 * @return
	 */
	@DeleteMapping( path = HISTORY_BASE + "{gameId}")
	public String deleteHistory(@RequestHeader(UserAccount.AUTH_USER_HEADER) final String authUser, @PathVariable final String gameId) {
		
		if (!accountService.hasRoles(authUser, ROLE.Admin)) {
			return "Not authorized";
		}
		
		final GameSettings settings = settingsProvider.retrieveSettings(gameId);
		if (settings.wasPlayed()==false && settings.getScheduledTime() < (System.currentTimeMillis() - TWENTY_FOUR_HRS)) {
			historyProvider.deleteGameRounds(gameId);
			return "Deleted";
		} else {
			return "Game was played or less than 24 hours since start";
		}
	}

	/** Get the settings for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	@GetMapping(path = SETTINGS_BASE + "{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getSettings(@PathVariable final String gameId) {
		return gson.toJson(settingsProvider.retrieveSettings(gameId));
	}
	
	/** Get the count of stored settings
	 * 
	 * @return
	 */
	@GetMapping(path = SETTINGS_BASE + "/count", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getSettingsCount() {
		final JsonObject resp = new JsonObject();
		resp.addProperty("success", true);
		resp.addProperty("count", settingsProvider.getSettingsCount());
		return resp.toString();
	}
	
	/** Get all game settings that are stored
	 * 
	 * @return
	 */
	@GetMapping(path = SETTINGS_BASE +"all", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getSettings(@RequestParam int page, @RequestParam int pageSize) {
		return gson.toJson(settingsProvider.retrieveSettings(page, pageSize));
	}
	
	/** Delete the game settings for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	@DeleteMapping( path = SETTINGS_BASE + "{gameId}")
	public long deleteSettings(@RequestHeader(UserAccount.AUTH_USER_HEADER) final String authUser, @PathVariable final String gameId) {
		if (!accountService.hasRoles(authUser, ROLE.Admin)) {
			return -1;
		}
		
		return settingsProvider.deleteSettings(gameId);
	}
	
	
	/** Get all player actions for a game
	 * 
	 * @param gameId
	 * @return
	 */
	@GetMapping(path = HISTORY_BASE + "actions/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getPlayerActionHistory(@PathVariable final String gameId) {
		return gson.toJson(actionProvider.getPlayerActions(gameId));
	}
	
	/** Get all player actions in a specific round for a game
	 * 
	 * @param gameId
	 * @param roundId
	 * @return
	 */
	@GetMapping(path = HISTORY_BASE + "actions/{gameId}/{roundId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getPlayerActionHistory(@PathVariable final String gameId, @PathVariable final int roundId) {
		return gson.toJson(actionProvider.getPlayerActions(gameId, roundId));
	}
	
	/** Get player actions for a specific player within a game
	 * 
	 * @param gameId
	 * @param playerId
	 * @return
	 */
	@GetMapping(path = HISTORY_BASE + "actions/{gameId}/player/{playerId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getPlayerActionHistory(@PathVariable final String gameId, @PathVariable final String playerId) {
		return gson.toJson(actionProvider.getPlayerActions(gameId, playerId));
	}
	
	/** Delete all player actions for a specific game
	 * 
	 * @param gameId
	 * @return
	 */
	@DeleteMapping( path = HISTORY_BASE + "actions/{gameId}")
	public long deletePlayerActions(@RequestHeader(UserAccount.AUTH_USER_HEADER) final String authUser, @PathVariable final String gameId) {
		if (!accountService.hasRoles(authUser, ROLE.Admin)) {
			return -1;
		}
		
		return actionProvider.deletePlayerActions(gameId);
	}
}