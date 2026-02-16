package com.langleydata.homepoker.controllers;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.ChatMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.Messaging;
import com.langleydata.homepoker.message.SettingsUpdate;
import com.langleydata.homepoker.message.StatusMessage;
import com.langleydata.homepoker.persistence.SettingsProvider;

/** A Controller specific to the Texas Hold'em game table, which works in addition to the 
 * standard TableController
 * 
 * @author Mike Reynolds
 *
 */
@Controller
public class TexasHoldemController {
	public static final String GAME_PATH = "texas/";
	
	private final Logger logger = LoggerFactory.getLogger(TexasHoldemController.class);
	private static final long FIFTEEN_MINS = 60 * 1000 * 15;
	@Autowired
	private SettingsProvider settingProvider;
	@Autowired
	private TableController tableController;
	@Autowired
	private Messaging msgUtils;
	
	@Value("${game-server.test.hostEmail:mikereynolds158@gmail.com}")
	private String testHostEmail;
	@Value("${game-server.test.organiserId:1234}")
	private String organiserId;
	@Value("${game-server.name:Tewkesbury}")
	private String gameServerName;
	
	/**
	 * Get the actual game table web page. If the game isn't 'active', then update the settings,
	 * store, set active and return the page. 
	 * 
	 * @return The game web page
	 */
	@GetMapping(path = GAME_PATH + "{gameId}")
	public String getTexasHoldemTable(HttpServletRequest request, @PathVariable final String gameId) {

		if (StringUtils.isEmpty(gameId)) {
			return "/no-game-settings";
		}

		AbstractCardGame<?> theGame = tableController.getActiveGame(gameId.replace("#", ""));
		
		if (theGame==null) {
			// lookup saved settings in database...
			final GameSettings testSet = isTestGame(gameId);
			final GameSettings storedSet = (testSet==null ? settingProvider.retrieveSettings(gameId) : testSet);
			
			// If not valid, return error page
			if (storedSet==null || storedSet.isArchived()) {
				return "/views/no-game-settings";
			}
			
			// Too early?
			if ((storedSet.getScheduledTime() - System.currentTimeMillis()) > FIFTEEN_MINS) {
				return "/views/game-too-early";
			}
			
			// No active game, but have settings, so create the new game
			final String serverUrl = request.getRequestURL().toString().replace(GAME_PATH + gameId, "");
			
			if (tableController.setGameActive(storedSet, serverUrl) == null) {
				return "/views/no-game-settings";
			}
		}
		
		return "/views/texas-holdem/texas-table";
	}
	
	/**
	 * 
	 * @param gameId
	 * @return
	 */
	private GameSettings isTestGame(final String gameId) {
		TexasHoldemSettings set = null;
		
		if (gameId.equals("test")) {
			set = new TexasHoldemSettings();
			set.setFormat(GameFormat.CASH);
			
		} else if (gameId.equals("test-tour")) {
			
			set = new TexasHoldemSettings();
			set.setFormat(GameFormat.TOURNAMENT);
			set.setOpeningStack(100);
			set.setAnte(1);
			set.setBlindIncreaseInterval(1*60*1000);
			set.setTournamentSplit(Arrays.asList(new Integer[] {100}));
		}
		
		if (set !=null) {
			set.setOrganiserId(organiserId);
			set.setHostEmail(testHostEmail);
			set.setHostName("God");
			
			// Overwrite the gameId
			Field f = ReflectionUtils.findField(TexasHoldemSettings.class, "gameId");
			f.setAccessible(true);
			ReflectionUtils.setField(f, set, gameId);
		}
		
		return set;
	}
	
	@MessageMapping(GenericTableController.PATH + "increaseAnte")
	@SendTo(MessageUtils.TABLE_TOPIC + "{gameId}")
	public SettingsUpdate increaseAnte(@DestinationVariable final String gameId, @Header("playerId") final String playerId) {
		
		String error = "";
		final AbstractCardGame<?> aGame = tableController.getActiveGame(gameId);
		if (aGame==null) {
			error = "No game found with id: " + gameId;
		} else if (aGame.getGameState() == TexasGameState.PRE_DEAL) {
			error = "You can not increase the blinds until post deal - Please wait";
		} else if (aGame.getSettings().getFormat()==GameFormat.TOURNAMENT) {
			error = "Blinds are automatically controlled in tournament games";
		}
		
		// Validate the host sent the message
		final Player sender = aGame.getPlayers().getPlayerById(playerId);
		if (sender==null || sender.getState().isHost()==false) {
			error = "Only the host can increase the blinds";
		}

		if (StringUtils.isNotBlank(error)) {
			msgUtils.sendPrivateMessage(sender.getSessionId(), new ChatMessage(error));
			return null;
		}
		
		logger.debug("Increasing Ante for game {}", gameId);
		SettingsUpdate update = null; 
		if (aGame.getSettings().increaseAnte()) {
			update = new SettingsUpdate(aGame.getSettings());
			final float ante = Math.round(aGame.getSettings().getAnte() * 100) / 100f;
			update.setMessage( sender.getPlayerHandle() + " has doubled the ante to " + ante + " / " + (ante * 2) );
		} else {
			msgUtils.sendPrivateMessage(sender.getSessionId(), new StatusMessage(sender.getSessionId(), "Failed to increase the ante - it is probably at maximum"));
		}
		
		return update;
	}

}
