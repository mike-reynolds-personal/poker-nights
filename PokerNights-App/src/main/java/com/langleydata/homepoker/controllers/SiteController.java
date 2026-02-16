package com.langleydata.homepoker.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.FeedbackForm;

@Controller("/")
public interface SiteController {

	/** Create a new game
	 * 
	 * @param request
	 * @param gameType
	 * @param settings
	 * @return
	 */
	@PostMapping(path = "/creategame/{gameType}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody // so it doesn't return a web page
	String createNewGame(HttpServletRequest request, String gameType, TexasHoldemSettings settings);

	/** Get a template of settings for a specific type
	 * 
	 * @param gameType
	 * @return
	 */
	@GetMapping(path = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody // so it doesn't return a web page
	GameSettings getSettings(String gameType);

	/** Submit a feedback form
	 * 
	 * @param gameType
	 * @return
	 */
	@PostMapping(path = "/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody // so it doesn't return a web page
	boolean submitFeedback(FeedbackForm feedback);
}