package com.langleydata.homepoker.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.UserAccount.ROLE;
import com.langleydata.homepoker.api.session.TokenInfo;
import com.langleydata.homepoker.api.session.TokenResponse;
import com.langleydata.homepoker.api.session.UserSession;

/** A Controller purely for localised development to allow downstream process to flow
 * 
 * @author Mike Reynolds
 *
 */
@Profile(value= {"dev"})
@RestController
public class TestSessionController {
	private int lastID = 0;
	
	private Gson gson = new Gson();

	@GetMapping(path = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getSession() {
		UserSession us = new UserSession("test" + lastID, "Test User " + lastID);
		us.setEmail("test" + lastID + "email@email.com");
		us.addRole(ROLE.Admin);
		us.addRole(ROLE.Player);
		us.setAccLevel(AccountLevel.TIER_3);
		TokenInfo ti = new TokenInfo();
		
		ti.setTokens(new TokenResponse());
		us.setTokens(ti);
		
		lastID++;
		return gson.toJson(us);
	}
}
