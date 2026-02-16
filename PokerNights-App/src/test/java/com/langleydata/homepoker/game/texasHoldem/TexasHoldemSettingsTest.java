package com.langleydata.homepoker.game.texasHoldem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.MoneyType;
import com.langleydata.homepoker.exception.GameSettingsValidationException;

public class TexasHoldemSettingsTest {

	@Test(expected = GameSettingsValidationException.class)
	public void testValidateDefault() throws GameSettingsValidationException {
		TexasHoldemSettings ths = new TexasHoldemSettings();
		
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testValidateInFuture() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setScheduledTime(System.currentTimeMillis() + (TexasHoldemSettings.ONE_DAY * 200));
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testValidateInPast() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setScheduledTime(System.currentTimeMillis() - (60 * 1000 * 11));
		ths.validate();
	}
	
	@Test
	public void testValidateRealMoney() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setMoneyType(MoneyType.REAL);
		ths.setHostControlledWallet(false);
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testValidateHostControlled() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setMoneyType(MoneyType.REAL);
		ths.setHostControlledWallet(true);
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testValidateNoBuyin() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setBuyInAmount(0);
		ths.validate();
	}
	
	@Test
	public void testValidatePasses() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.validate();
	}
	
	@Test
	public void testIncreaseAnteDoubles() {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		assertEquals(0.1f, ths.getAnte(), 0.0001);
		
		assertTrue(ths.increaseAnte());
		assertEquals(0.2f, ths.getAnte(), 0.0001);
		
		assertTrue(ths.increaseAnte());
		assertEquals(0.4f, ths.getAnte(), 0.0001);
		
	}
	
	@Test
	public void testIncreaseAnteNotBeyondBuyin() {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setAnte(2.5f);
		assertEquals(2.5f, ths.getAnte(), 0.0001);
		assertEquals(10, ths.getBuyInAmount(), 0.0001);
		
		assertTrue(ths.increaseAnte());
		assertEquals(5f, ths.getAnte(), 0.0001);
		
		assertFalse(ths.increaseAnte());
		assertEquals(5f, ths.getAnte(), 0.0001);
		
	}
	
	@Test
	public void testValidTournamentSettings() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setFormat(GameFormat.TOURNAMENT);
		ths.setBlindIncreaseInterval(5*60*1000);
		ths.setOpeningStack(10000);
		ths.setTournamentSplit(new ArrayList<>(List.of(10,30,60)));
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testInvalidTournamentSplitBlank() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setFormat(GameFormat.TOURNAMENT);
		List<Integer> splits = new ArrayList<>();
		splits.add(20);splits.add(null);splits.add(80);
		ths.setTournamentSplit(splits);
		
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testInvalidTournamentSplit() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setFormat(GameFormat.TOURNAMENT);
		ths.setTournamentSplit(new ArrayList<>(List.of(20,40,60)));
		
		ths.validate();
	}
	
	@Test(expected = GameSettingsValidationException.class)
	public void testInvalidTournamentInterval() throws GameSettingsValidationException {
		TexasHoldemSettings ths = mkSettings("mike", "bob", 5000L);
		ths.setFormat(GameFormat.TOURNAMENT);
		ths.setTournamentSplit(new ArrayList<>(List.of(100)));
		ths.setBlindIncreaseInterval(1000);
		ths.validate();
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
}
