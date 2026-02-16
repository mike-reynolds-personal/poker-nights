package com.langleydata.homepoker.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.AccountStats;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.MoneyType;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.controllers.MainSiteController.PotCalc;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.game.texasHoldem.PokerHandEvaluator;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;
import com.langleydata.homepoker.persistence.SettingsProvider;
import com.langleydata.homepoker.services.Account;
import com.langleydata.homepoker.services.EmailSender;

@RunWith(MockitoJUnitRunner.class)
public class MainSiteControllerTest {
	static final long ONE_HOUR = 60 * 1000 * 60;
	@Mock
	private SettingsProvider settingsProvider;
	@Mock
	private AccountService accountService;
	@Mock
	private EmailSender emailSender;
	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private MainSiteController msc;
	private Gson gson = new Gson();
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(request.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8443/creategame/TEXAS_HOLDEM"));
		when(request.getRequestURI()).thenReturn("/creategame/TEXAS_HOLDEM");
	}
	
	@Test
	public void testSidePotMethod() {
		msc.handEvaluator = new PokerHandEvaluator();
		
		PotCalc a = new PotCalc("A", 1, 10f);// 10 - 6.5 - 1.5 = 2 left
		PotCalc b = new PotCalc("B", 2, 8f);// 8 - 6.5 = 1.5 off a + 1.5stake
		PotCalc c = new PotCalc("C", 3, 6.5f);// takes 6.5 off other 2 + 6.5stake
		
		List<PotCalc> pots = new ArrayList<>();
		pots.add(a);pots.add(b);pots.add(c);
		
		// When
		final GamePots sidePots = msc.getSidePots(pots);
		Assert.assertNotNull(sidePots);
		
		final Map<String, SidePot> allPots = sidePots.getAllPots();
		Assert.assertNotNull(allPots);
		
		Assert.assertEquals(GamePots.FIRST_POT_NAME, allPots.get(GamePots.FIRST_POT_NAME).getName());
		Assert.assertEquals(6.5f * 3f, allPots.get(GamePots.FIRST_POT_NAME).getPotTotal(), 0.01);
		
		Assert.assertEquals("Pot A", allPots.get("Pot A").getName());
		Assert.assertEquals(3f, allPots.get("Pot A").getPotTotal(), 0.01);
		
		Assert.assertEquals("Pot B", allPots.get("Pot B").getName());
		Assert.assertEquals(2f, allPots.get("Pot B").getPotTotal(), 0.01);
	}
	
	@Test
	public void testCreateNewGameCreatesWithExistingAccount() throws GameSchedulingException, MessagingException {
		final String oName = "mike";
		TexasHoldemSettings ths = mkSettings(oName, "mike", ONE_HOUR);
		
		
		UserAccount acc = mock(UserAccount.class);
		AccountStats stats = mock(AccountStats.class);
		when(acc.getStats()).thenReturn(stats);
		when(stats.getOrganisedGames()).thenReturn(mock(Map.class));

		when(accountService.getAccount(eq(oName))).thenReturn(acc);//wont store
		//when(accountService.addAccount(any(UserAccount.class))).thenReturn(true);
		when(settingsProvider.storeSettings(ths)).thenReturn(true);
		
		final String resp = msc.createNewGame(request, "TEXAS_HOLDEM", ths);
		assertNotNull(resp);
		
		// Validate response
		JsonObject jResp = gson.fromJson(resp, JsonObject.class);
		assertEquals("https://localhost:8443/texas/" + ths.getGameId(), jResp.get("gameUrl").getAsString());
		assertTrue(jResp.get("success").getAsBoolean());
		JsonObject retSetting = jResp.getAsJsonObject("settings");
		assertNotNull(retSetting);
		assertNotNull(retSetting.get("gameId").getAsString());
		assertEquals(oName, retSetting.get("organiserName").getAsString());

		// Verify calls
		verify(accountService).saveAccount(any(UserAccount.class));
		verify(accountService).canScheduleGame(oName, ths);
		verify(settingsProvider).storeSettings(ths);
		verify(emailSender).sendInviteEmails(ths);
		verify(stats).getOrganisedGames();
	}
	
	@Test
	public void testCreateNewGameCreatesWithNoAccount() throws GameSchedulingException, MessagingException {
		final String oName = "mike";
		TexasHoldemSettings ths = mkSettings(oName, "mike", ONE_HOUR);
		
		when(accountService.getAccount(eq(oName))).thenReturn(null);
		when(accountService.saveAccount(any(UserAccount.class))).thenReturn(true);
		when(settingsProvider.storeSettings(ths)).thenReturn(true);
		
		final String resp = msc.createNewGame(request, "TEXAS_HOLDEM", ths);
		assertNotNull(resp);
		
		// Validate response
		JsonObject jResp = gson.fromJson(resp, JsonObject.class);
		assertEquals("https://localhost:8443/texas/" + ths.getGameId(), jResp.get("gameUrl").getAsString());
		assertTrue(jResp.get("success").getAsBoolean());
		JsonObject retSetting = jResp.getAsJsonObject("settings");
		assertNotNull(retSetting);
		assertNotNull(retSetting.get("gameId").getAsString());
		assertEquals(oName, retSetting.get("organiserName").getAsString());

		// Verify calls
		verify(accountService).saveAccount(any(UserAccount.class));
		verify(accountService).canScheduleGame(oName, ths);
		verify(settingsProvider).storeSettings(ths);
		verify(emailSender).sendInviteEmails(ths);
	}
	
	@Test
	public void testCreateNewGameWithInvalidHostControlledWallet() throws GameSchedulingException, MessagingException {
		final String oName = "mike";
		TexasHoldemSettings ths = mkSettings(oName, "mike", ONE_HOUR);
		ths.setHostControlledWallet(true);
		ths.setMoneyType(MoneyType.REAL);
		
		final String resp = msc.createNewGame(request, "TEXAS_HOLDEM", ths);
		assertNotNull(resp);
		
		// Validate response
		JsonObject jResp = gson.fromJson(resp, JsonObject.class);
		assertFalse(jResp.get("success").getAsBoolean());
		assertTrue(jResp.get("error").getAsString().contains("only applicable"));
		
		// Verify calls
		verify(accountService, never()).saveAccount(any(UserAccount.class));// not updated
		verify(accountService, never()).canScheduleGame(oName, ths);
		verify(settingsProvider, never()).storeSettings(ths);
		verify(emailSender, never()).sendInviteEmails(ths);
	}
	
	@Test
	public void testCreateNewGameWithInvalidSettings() throws GameSchedulingException, MessagingException {
		final String oName = "mike";
		TexasHoldemSettings ths = mkSettings(oName, "mike", ONE_HOUR);
		ths.setOrganiserEmail(null);
		
		final String resp = msc.createNewGame(request, "TEXAS_HOLDEM", ths);
		assertNotNull(resp);
		
		// Validate response
		JsonObject jResp = gson.fromJson(resp, JsonObject.class);
		assertFalse(jResp.get("success").getAsBoolean());
		assertTrue(jResp.get("error").getAsString().contains("Organiser or Host details are not set"));
		
		// Verify calls
		verify(accountService, never()).saveAccount(any(UserAccount.class));// not updated
		verify(accountService, never()).canScheduleGame(oName, ths);
		verify(settingsProvider, never()).storeSettings(ths);
		verify(emailSender, never()).sendInviteEmails(ths);
	}
	
	@Test
	public void testCreateNewGameWhenAccountNotAllowed() throws GameSchedulingException, MessagingException {
		final String oName = "mike";
		TexasHoldemSettings ths = mkSettings(oName, "mike", ONE_HOUR);
		
		Mockito.doThrow(new GameSchedulingException("test")).when(accountService).canScheduleGame(oName, ths);
		
		// when
		final String resp = msc.createNewGame(request, "TEXAS_HOLDEM", ths);
		assertNotNull(resp);
		
		// Validate response
		JsonObject jResp = gson.fromJson(resp, JsonObject.class);
		assertFalse(jResp.get("success").getAsBoolean());
		assertTrue(jResp.get("error").getAsString().contains("test"));
		
		// Verify calls
		verify(accountService, never()).saveAccount(any(UserAccount.class)); // no updates
		verify(accountService).canScheduleGame(oName, ths);
		verify(settingsProvider, never()).storeSettings(ths);
		verify(emailSender, never()).sendInviteEmails(ths);
	}
	
	private TexasHoldemSettings mkSettings(String oName, String hName, long timeAdd) {
		TexasHoldemSettings ths = new TexasHoldemSettings();
		ths.setOrganiserEmail(oName);
		ths.setHostEmail(hName);
		ths.setHostName(hName);
		ths.setOrganiserId(oName);
		ths.setOrganiserName(oName);
		ths.setFormat(GameFormat.CASH);
		ths.setScheduledTime(System.currentTimeMillis() + timeAdd);
		return ths;
	}
	private Account mkAccount(GameSettings settings) {
		return new Account.Builder(settings.getOrganiserId(), settings.getOrganiserName())
				.email(settings.getOrganiserEmail())
				.build();
	}
}
