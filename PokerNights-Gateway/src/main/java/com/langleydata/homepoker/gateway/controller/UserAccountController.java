package com.langleydata.homepoker.gateway.controller;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.api.UserAccount.ROLE;
import com.langleydata.homepoker.api.session.TokenInfo;
import com.langleydata.homepoker.api.session.UserSession;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.services.Account;
import com.nimbusds.oauth2.sdk.util.StringUtils;

import reactor.core.publisher.Mono;

@RestController
public class UserAccountController extends RedirectServerAuthenticationSuccessHandler {
	private final Logger logger = LoggerFactory.getLogger(UserAccountController.class);
	static final String USER_ACCOUNT_BASE = "/account/";
	
	@Autowired
	private AccountService accountService;
	
	@Value("${user-account.default-level:TIER_1}") String defaultAccLevel;
	
	@Autowired
	private ReactiveOAuth2AuthorizedClientService authService;
	
	private Gson gson = new Gson();
	private static String UPGRADE_FORM; 
	static {
		UPGRADE_FORM = ControllerUtils.getTemplate("static/views/upgrade-account.html");
	}

	/** Fallback if no game servers are available
	 * 
	 */
	@GetMapping(path = "/noserver", produces = MediaType.TEXT_HTML_VALUE)
	public String noServerFound() {
		return "<html><h2>Sorry, no server was found that could service this request</h2>"
				+ "Please try the <a href=\"/\">Home Page</a> instead.</html>";
	}
	
	/** User account upgrade endpoint
	 * 
	 * @param user
	 * @return
	 */
	@GetMapping(USER_ACCOUNT_BASE + "upgrade")
	public String getUpgradeAccountForm(@AuthenticationPrincipal final OAuth2User user) {
		
		if (user==null) {
			return "Sorry, you were not authenticated by the social provider";
		}
		
		final UserAccount account;
		try {
			account = accountService.getAccount(user.getName());
		} catch (GameSchedulingException e) {
			logger.error("Getting upgrade form {}", e.getMessage());
			return "Sorry, there was an error getting the upgrade form. Please contact <a href=\"mailto:support@pokerroomsnow.com\">Support</a>";
		}
		
		return UPGRADE_FORM.replace("{accountId}", account.getPlayerId()).replace("{redirectOnClose}", "/");
	}
	
	/**
	 * 
	 * @param user
	 * @param userId
	 * @return
	 */
	private JsonObject validateUserOrIntegration(final OAuth2User user, final String userId) {
		JsonObject error = new JsonObject();
		
		if (user == null || StringUtils.isBlank(userId)) {
			error.addProperty("errorMessage", "No userId or authorized user provided");
			return error;
		}
		
		 if (!accountService.hasRolesAnyOf(user.getName(), ROLE.Integration, ROLE.Admin) && !user.getName().equals(userId)) {
			 error.addProperty("errorMessage", "Account not accessible by the authenticated user");
			 return error;
		 }
		 return null;
	}
	
	/** Get a specific UserAccount info
	 * 
	 * @param userId
	 * @return
	 */
	@GetMapping(path = USER_ACCOUNT_BASE + "{userId}")
	public String getAccount(@AuthenticationPrincipal final OAuth2User user, @PathVariable final String userId) {
		JsonObject error = validateUserOrIntegration(user, userId);
		
		if (error != null) {
			return error.toString();
		}
		
		try {
			return gson.toJson(accountService.getAccount(userId));
		} catch (GameSchedulingException e) {
			JsonObject resp = new JsonObject();
			resp.addProperty("error", e.getMessage());
			resp.addProperty("success", false);
			return resp.toString();
		}
	}

	/** Delete a user's account
	 * 
	 * @param user
	 * @param userId
	 * @return
	 */
	@DeleteMapping(path = USER_ACCOUNT_BASE + "{userId}")
	public String deleteAccount(@AuthenticationPrincipal final OAuth2User user, @PathVariable final String userId) {
		JsonObject error = validateUserOrIntegration(user, userId);

		if (error != null) {
			return error.toString();
		}

		JsonObject resp = new JsonObject();
		resp.addProperty("success", false);

		try {
			final String deleted = accountService.deleteAccount(userId);
			if (StringUtils.isNotBlank(deleted)) {
				resp.addProperty("playedId", deleted);
				resp.addProperty("success", true);
			}
		} catch (IOException e) {
			resp.addProperty("error", e.getMessage());
		}

		return resp.toString();
	}
	
	/** List all of the current accounts
	 *  
	 * @return
	 */
	@GetMapping(path = USER_ACCOUNT_BASE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getUserAccounts(@AuthenticationPrincipal final OAuth2User user, @RequestParam int page, @RequestParam int pageSize) {
		if (!accountService.hasRoles(user.getName(), ROLE.Admin)) {
			return "[]";
		}
		return gson.toJson(accountService.getAccounts(page, pageSize));
	}
	
	/** Get the number of accounts indexed
	 * 
	 * @param user
	 * @return
	 */
	@GetMapping(path = USER_ACCOUNT_BASE + "/count", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getUserAccountCount(@AuthenticationPrincipal final OAuth2User user) {
		final JsonObject resp = new JsonObject();
		resp.addProperty("success", false);
		
		if (!accountService.hasRolesAnyOf(user.getName(), ROLE.Integration, ROLE.Admin)) {
			resp.addProperty("error", "Authenticated user is neither admin or integration account");
			return resp.toString();
		}
		
		resp.addProperty("count", accountService.getAccountCount());
		resp.addProperty("success", true);
		
		return resp.toString();

	}
	/** Change the account level of the UserAccount
	 * 
	 * @param userId
	 * @param newLevel
	 * @return
	 */
	@PutMapping(path = USER_ACCOUNT_BASE + "{userId}")
	public String updateAccountLevel(@AuthenticationPrincipal final OAuth2User user, @PathVariable final String userId, @RequestParam AccountLevel newLevel) {
		
		final JsonObject resp = new JsonObject();
		resp.addProperty("success", false);
		
		if (!accountService.hasRolesAnyOf(user.getName(), ROLE.Integration, ROLE.Admin)) {
			resp.addProperty("error", "Authenticated user is neither admin or integration account");
			return resp.toString();
		}
		
		try {
			final Account acc = (Account) accountService.getAccount(userId);
			if (acc.getAccLevel() != newLevel) {
				acc.setAccLevel(newLevel);
				resp.addProperty("success", accountService.saveAccount(acc));
			}
			
		} catch (GameSchedulingException e) {
			resp.addProperty("error", e.getMessage());

		}
		
		return resp.toString();
	}
	
	/** Apply a set of roles to a UserAccount
	 * 
	 * @param userId
	 * @param newRoles
	 * @return
	 */
	@PutMapping(path = USER_ACCOUNT_BASE + "{userId}/roles")
	public String updateRoles(@AuthenticationPrincipal final OAuth2User user, @PathVariable final String userId, @RequestParam Set<ROLE> newRoles) {

		final JsonObject resp = new JsonObject();
		resp.addProperty("success", false);
		
		if (!accountService.hasRoles(user.getName(), ROLE.Admin)) {
			resp.addProperty("error", "Authenticated user is not an administrator");
			return resp.toString();
		}
		
		if (newRoles != null) {
			try {
				final Account acc = (Account) accountService.getAccount(userId);
				if (newRoles.contains(ROLE.Player)) {
					newRoles.add(ROLE.Player);
				}
				newRoles.forEach(r -> acc.addRole(r));
				resp.addProperty("success", accountService.saveAccount(acc));
				
			} catch (GameSchedulingException e) {
				resp.addProperty("error", e.getMessage());
	
			}
		}
		
		return resp.toString();
	}
	
	/** Build an <code>Account</code> from the supplied user. Does NOT include account level
	 *  
	 * @param user The OAuth2User
	 * @return A new account without an account level
	 */
	public static Account buildAccountInfo(final OAuth2User user) {
		return new Account.Builder(user.getName(), getHandle(user))
				.email(user.getAttribute("email"))
				.givenName(user.getAttribute("given_name"))
				.familyName(user.getAttribute("family_name"))
				.fullName(user.getAttribute("name"))
				.locale(user.getAttribute("locale"))
				.picture(user.getAttribute("picture"))
				.role(ROLE.Player)
				.build();
	}
	
	/** Build a player handle string from the user
	 * 
	 * @param user
	 * @return
	 */
	private static String getHandle(final OAuth2User user) {
		String playerHandle = StringUtils.isNotBlank(user.getAttribute("nickname")) ? user.getAttribute("nickname") : user.getAttribute("given_name");
		if  (StringUtils.isBlank(playerHandle)) {
			final String[] nParts = user.getAttribute("name").toString().split(" ");
			if (nParts.length > 0) {
				playerHandle = nParts[0];
			}
		}
		return playerHandle;
	}

	@Override
	public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {

		final OAuth2User user = (OAuth2User) authentication.getPrincipal();

		if (user != null) {

			// Check if we have an existing account...
			UserAccount existing = null;
			boolean elasticError = false;
			try {
				existing = accountService.getAccount(user.getName());
				/*
				 * TODO Should check if there is an existing account with the same email address
				 * and reject if its under a different provider
				 */
			} catch (GameSchedulingException e) {
				logger.warn("Exception whilst getting user account {}: {}", user.getName(), e.getMessage());
				elasticError = true;
			}

			if (!elasticError) {
				final Account newDefaultAccount = buildAccountInfo(user);

				// no existing account, so build one at the default level and save
				if (existing == null || StringUtils.isBlank(existing.getFullName())) {
					newDefaultAccount.setAccLevel(AccountLevel.valueOf(defaultAccLevel));
					logger.debug("Storing new user account for {}", user.getName());

					try {
						accountService.saveAccount(newDefaultAccount);
					} catch (GameSchedulingException e) {
						logger.error("Saving account onSuccess: {}", e.getMessage());
					}
				} else {
					// We have an existing account, so update the profile info if changed...
					accountService.updateAccountProfile(existing, newDefaultAccount);
				}
			}
		}

		return super.onAuthenticationSuccess(webFilterExchange, authentication);
	}

	/** Get a session object for the logged in user
	 * 
	 * @param user
	 * @param authToken
	 * @return
	 */
	@GetMapping(path = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public UserSession getSession(@AuthenticationPrincipal final OAuth2User user, OAuth2AuthenticationToken authToken) {

		UserSession session = new UserSession(null, null); // Have to return an object to trigger callbacks
		boolean hasAccessToken = false, didRefresh = false;

		if (user ==null) {
			return session;
		}

		try {
			UserAccount existing = accountService.getAccount(user.getName());
			if (existing != null) {
				session = new UserSession((Account)existing);
			}
		} catch (GameSchedulingException e) {
			logger.warn("Exception whilst getting user account {}: {}", user.getName(), e.getMessage());
		}
		
		// Is the client authorised to talk to the server?
		OAuth2AuthorizedClient authorizedClient;
		try {
			authorizedClient = authService.loadAuthorizedClient(authToken.getAuthorizedClientRegistrationId(),
					authToken.getName()).toFuture().get();
		} catch (Exception e) {
			logger.warn("Getting auth-client", e);
			authorizedClient = null;
		}

		if (authorizedClient == null) {
			logger.debug("Have a user, but no authorised client - should request here");
			//Not working correctly - see comments on method
		} else {
			final OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
			final OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
			TokenInfo tokenInfo = new TokenInfo();
			tokenInfo.setAccessToken(accessToken);
			tokenInfo.setRefreshToken(refreshToken);

			// If the access token is going to expire then refresh it, otherwise return it
//				if (accessToken.getExpiresAt().toEpochMilli() > (System.currentTimeMillis() + GatewayTokenService.TOKEN_EXPIRY_MARGIN)) {
//					tokenInfo.setAccessToken(accessToken);
//					tokenInfo.setRefreshToken(refreshToken);
//				} else {
//					// refresh the token
//					try {
//						tokenInfo = tokenService.refreshToken(refreshToken.getTokenValue());
//						didRefresh = true;
//					} catch (IOException e) {
//						logger.error("Refreshing token:", e);
//					}
//					
//				}
			
			session.setTokens(tokenInfo);
			hasAccessToken = tokenInfo.getAccessToken()!=null && StringUtils.isNotBlank(tokenInfo.getAccessToken().getToken());
		}
		
		
		logger.trace("Returning session info. HasUserDetails: {}, HasAccessToken: {}, DidRefresh: {}", user!=null, hasAccessToken, didRefresh);

		return session;
	}
}
