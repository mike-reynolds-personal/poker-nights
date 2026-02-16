package com.langleydata.homepoker.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.api.UserAccount.ROLE;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.exception.GameSettingsValidationException;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;
import com.langleydata.homepoker.game.texasHoldem.PokerHandEvaluator;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePot;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.message.FeedbackForm;
import com.langleydata.homepoker.message.HandRankingMessage;
import com.langleydata.homepoker.persistence.FeedbackProvider;
import com.langleydata.homepoker.persistence.SettingsProvider;
import com.langleydata.homepoker.services.Account;
import com.langleydata.homepoker.services.EmailSender;
import com.langleydata.homepoker.services.EmailSender.ScreenConversion;

/** A controller for general site actions/ methods
 * 
 *
 */
@Controller
public class MainSiteController implements SiteController {
	private final Logger logger = LoggerFactory.getLogger(MainSiteController.class);

	@Value("${game-server.gateway-url:https://app.pokerroomsnow.com/}")
	private String gatewayUrl;
	
	@Autowired
	private SettingsProvider settingsProvider;
	@Autowired
	private FeedbackProvider feedbackProvider;
	@Autowired
	private AccountService accountService;
	
	@Autowired
	protected PokerHandEvaluator handEvaluator;

	@Autowired
	private EmailSender emailSender;
	
	@Value("${spring.profiles.active:Unknown}")
	private String activeProfile;
	
	@GetMapping("/")
	public String homePage() {
		return "index";
	}

	@GetMapping("/admin")
	public String getAdmin(@RequestHeader(value = UserAccount.AUTH_USER_HEADER, required=false) final String authUser) {
		if (accountService.hasRoles(authUser, ROLE.Admin)) {
			return "views/main/admin";
		} else {
			return "403";
		}
	}
	
	@GetMapping("/evaluator")
	public String getHandEvaluator() {
		return "views/main/evaluator";
	}
	
	@GetMapping("/how-to-play")
	public String getHowToPlay() {
		return "views/main/how-to-play";
	}
	
	@GetMapping("/sidepots")
	public String geSidePotEvaluator() {
		return "views/main/sidepots";
	}
	
	@GetMapping("/privacy")
	public String gePrivacyTemp() {
		return "views/main/privacy";
	}
	
	@GetMapping("/terms")
	public String geTermsTemp() {
		return "views/main/terms";
	}
	
	@GetMapping("/schedule-game")
	public String getCreateGame(@RequestHeader(value = UserAccount.AUTH_USER_HEADER, required=false) final String authUser, @RequestParam(required = false) final String gameType) {
		if (StringUtils.isBlank(authUser) && !activeProfile.contains("dev")) {
			return "403";
		}
		if ("texas_holdem".equalsIgnoreCase(gameType) || StringUtils.isBlank(gameType)) {
			return "views/main/schedule-game";
		}
		return "views/no-game-settings";
	}
	
	@GetMapping("/loginUser")
	@ResponseBody
	public String devLogin() {
		return "<html>" +
				"<p>You are attempting to logon through the Game Server.</p>" +
				"If you are seeing this page you are in testing mode and should initiate a session through the " +
				"<a href=\"https:\\\\localhost:8090\\loginUser\">Gateway Service</a>" +
				"</html>";
	}
	
	@Override
	public String createNewGame( HttpServletRequest request, 
								 @PathVariable final String gameType,
								 @RequestBody final TexasHoldemSettings settings) {

		settings.setLocale(request.getLocale() == null ? LocaleContextHolder.getLocale() : request.getLocale());
		
		UserAccount organiserAccount = null;
		
		try {
			settings.validate();
			
			// Try to get an existing account, and create one if it doesn't exist
			organiserAccount = accountService.getAccount(settings.getOrganiserId());
			if (organiserAccount == null) {
				// This is a fallback as should always be created and stored through the GatewayController
				organiserAccount = new Account.Builder(settings.getOrganiserId(), settings.getOrganiserName())
					.email(settings.getOrganiserEmail())
					.locale(settings.getLocale().toString())
					.associates(settings.getInvitedEmails())
					.accLevel(AccountLevel.TIER_1)
					.build();
			} else if (settings.getInvitedEmails().size() > 0) {
				organiserAccount.getKnownAssociates().addAll(settings.getInvitedEmails());
			}
			accountService.canScheduleGame(settings.getOrganiserId(), settings);
			
		} catch (GameSchedulingException | GameSettingsValidationException e) {
			return doError(e);
		}
		
		// store in database
		if (!settingsProvider.storeSettings(settings)) {
			return doError("Failed to store settings for game " + settings.getGameId());
		}
		
		try {
			emailSender.sendInviteEmails( settings );
		} catch (MessagingException e) {
			// don't care too much
			logger.warn("Unable to send invite emails", e);
		}

		// Update the account stats for the new game, after we've confirmed the game was stored
		try {
			organiserAccount.getStats().getOrganisedGames().put(settings.getGameId(), settings.getScheduledTime());
			accountService.saveAccount(organiserAccount);
		} catch (GameSchedulingException e) {
			return doError(e);
		}
		
		if (StringUtils.isBlank(gatewayUrl)) {
			gatewayUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "/");
		}
		if (!gatewayUrl.endsWith("/")) {
			gatewayUrl += "/";
		}
		// All passed so send game link and success response
		final JsonObject resp = new JsonObject();
		resp.add("settings", new Gson().toJsonTree(settings));
		resp.addProperty("gameUrl", gatewayUrl + TexasHoldemController.GAME_PATH + settings.getGameId());
		resp.addProperty("success", true);
		logger.trace("Successfully stored new game settings");
		
		return resp.toString();
	}
	
	/** Evaluate a Texas Hold'em poker hand to obtain the hand rank
	 * 
	 * @param playerCards
	 * @param tableCards
	 * @return
	 */
	@GetMapping("/handrank")
	@ResponseBody
	public HandRankingMessage getHandRank(@RequestParam final String tableCards, @RequestParam final String playerCards) {
		final String[] pCards = playerCards.split(",");
		final String[] tCards = tableCards.split(",");
		if (pCards==null || pCards.length !=2) {
			return new HandRankingMessage("2 player cards not supplied");
		}
		if (tCards==null || tCards.length !=5) {
			return new HandRankingMessage("5 table cards not supplied");
		}

		try {
			final HandRank hr = handEvaluator.getPlayerHandRank(Card.makeCards(pCards), Card.makeCards(tCards));
			return new HandRankingMessage(null, new ExtHandRank(hr));
		} catch (Exception e) {
			return new HandRankingMessage(e.getMessage());
		}
	}
	
	/** Perform an independent side-pot calculation
	 * 
	 * @param pots
	 * @return
	 */
	@PostMapping(path = "/calcpots", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public GamePots getSidePots(@RequestBody List<PotCalc> pots) {
		final GamePots gp = new GamePots();
		
		if (pots!=null && pots.size() == 0) {
			return gp;
		}
		
		final List<Player> players = pots.stream()
				.filter(pc -> pc.getBet() > 0 && pc.getRank() > 0 && StringUtils.isNotBlank(pc.getName()))
				.map(PotCalc::toPlayer)
				.collect(Collectors.toList());
		
		if (players.size() == 0) {
			return gp;
		}
		
		final float total = (float) pots.stream()
				.mapToDouble(PotCalc::getBet)
				.sum();
		
		if (total < 0.1) {
			return gp;
		}
		
		gp.addSidePots( handEvaluator.calculateSidePots(new GamePot(TexasGameState.RIVER, players)) );
		
		return gp;
	}

	/**
	 * 
	 * @param e
	 * @return
	 */
	private String doError(String e) {
		final JsonObject resp = new JsonObject();
		
		logger.warn("Error scheduling game: '{}'", e);
		resp.addProperty("error", e);
		resp.addProperty("success", false);
		
		return resp.toString();
	}
	/**
	 * 
	 * @param e
	 * @return
	 */
	private String doError(Exception e) {
		return doError(e.getMessage());
	}
	
	@Override
	public boolean submitFeedback(@RequestBody final FeedbackForm feedback) {
		
		try {
			emailSender.sendEmail( feedback );
		} catch (MessagingException e) {
			logger.error("Sending feedback to email", e);
		}
		
		saveFeedbackScreenShot(feedback);
		return feedbackProvider.storeFeedback(feedback);
	}
	
	/**
	 * 
	 * @param feedback
	 */
	void saveFeedbackScreenShot(final FeedbackForm feedback) {
		if ( StringUtils.isNotBlank(feedback.getScreen())) {
			
			try {
				File toDisk = File.createTempFile(feedback.getMessageId(), ".png");
				final byte[] img = EmailSender.screenshotToByteArray(feedback.getScreen(), ScreenConversion.DECODE);
				if (img != null) {
					FileUtils.writeByteArrayToFile(toDisk, img);
					logger.debug("Screenshot written to {}", toDisk.toString());
				}
			} catch (IOException e) {
				logger.warn("Writing screenshot to disk: {}", e.getMessage());
			}
		}
		feedback.setScreen(null);
	}
	
	@Override
	public GameSettings getSettings(@RequestParam final String gameType) {
		
		if (gameType.equalsIgnoreCase("texas-holdem")) {
			return new TexasHoldemSettings();
		}
		return null;
	}
	
	/** A class for displaying additional hand rank object fields
	 * 
	 */
	static class ExtHandRank extends HandRank {
		final List<Card> rankedCards;
		ExtHandRank(HandRank hr) {
			super(hr);
			rankedCards = hr.getCards();
		}
		public List<Card> getRankedCards() {
			return rankedCards;
		}
	}
	

	/** Used for receiving a json object from the main website
	 * pot calculation tool
	 */
	static class PotCalc {
		private final String name;
		private final float bet;
		private final int rank;
		
		/**
		 * 
		 * @param name
		 * @param rank
		 * @param bet
		 */
		PotCalc(String name, final int rank, float bet) {
			this.name = name;
			this.bet = bet;
			this.rank = rank;
		}
		float getBet() {
			return this.bet;
		}
		String getName() {
			return this.name;
		}
		int getRank() {
			return this.rank;
		}
		Player toPlayer() {
			Player p = new Player(name, name);
			p.getCurrentStack().initialise(bet, false);
			p.getCurrentStack().reBuy(bet, 0f);
			p.getCurrentStack().addToTable(TexasGameState.RIVER, bet);
			p.setRankedHand(new HandRank(rank, PokerHand.PAIR, Collections.emptyList()));
			return p;
		}
	}
}