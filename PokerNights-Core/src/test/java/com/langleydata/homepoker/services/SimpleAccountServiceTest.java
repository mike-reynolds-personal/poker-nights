package com.langleydata.homepoker.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.AccountStats;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.persistence.mem.MemoryGenericSettingsProvider;
import com.langleydata.homepoker.player.PlayerStats;

@RunWith(MockitoJUnitRunner.class)
public class SimpleAccountServiceTest {
	static final long DAY = 60 * 1000 * 60 * 24; 
	
	@InjectMocks
	private SimpleAccountService sas;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sas.accProvider = new MemoryGenericSettingsProvider();
	}
	
	@Test
	public void testCanScheduleGameAtDifferentTiers() throws GameSchedulingException {
		String org = "mike";
		String host = "mike";
		final Account acc = new Account.Builder(org, org).email(org + "@" + org).accLevel(AccountLevel.TIER_1).build();
		
		// must have a stored account
		assertTrue(sas.saveAccount(acc));
		
		// Creates at tier 1
		sas.canScheduleGame(org, mkSettings(org, host, DAY));
		
		// Can't schedule another game
		try {
			sas.canScheduleGame(org, mkSettings(org, host, DAY * 2));
		} catch (GameSchedulingException e) {
			assertEquals(SimpleAccountService.TOO_MANY_GAMES, e.getMessage());
		}
		
		// Upgrade the account
		acc.setAccLevel(AccountLevel.TIER_2);
		sas.saveAccount(acc);
		
		// Can now schedule up to the maximum
		for (int i=0; i < AccountLevel.TIER_2.getGamesPerMonth()-1; i++) { 
			sas.canScheduleGame(org, mkSettings(org, host, DAY * i));
		}
		
		// Now fails
		try {
			sas.canScheduleGame(org, mkSettings(org, host, DAY * 4));
		} catch (GameSchedulingException e) {
			assertEquals(SimpleAccountService.TOO_MANY_GAMES, e.getMessage());
		}
		
	}
	
	@Test
	public void testCanScheduleWhenNotHosting() throws GameSchedulingException {
		String org = "mike";
		String host = "bob";
		final Account acc = new Account.Builder(org, org).email(org + "@" + org).accLevel(AccountLevel.TIER_1).build();
		
		// must have a stored account
		assertTrue(sas.saveAccount(acc));
		
		// Can't schedule a game as not hosting
		try {
			sas.canScheduleGame(org, mkSettings(org, host, DAY * 2));
		} catch (GameSchedulingException e) {
			assertEquals(SimpleAccountService.MUST_BE_HOST, e.getMessage());
		}
		
		// Upgrade the account
		acc.setAccLevel(AccountLevel.TIER_3);
		sas.saveAccount(acc);
		
		// Can now schdule
		sas.canScheduleGame(org, mkSettings(org, host, DAY));
		
	}
	
	@Test
	public void testSavingStatsWhenNoAccount() throws GameSchedulingException {
		String org = "mike";
		final Account acc = new Account.Builder(org, org).email(org + "@" + org).accLevel(AccountLevel.TIER_1).build();
		
		TestStats newStats = new TestStats();
		
		// Orginal stats, don't need an account
		assertTrue(sas.updateAccountStats(acc, newStats, "test"));
		
		UserAccount newAcc = sas.getAccount(org);
		assertNotNull(newAcc);
		AccountStats st = newAcc.getStats();
		assertNotNull(st);
		
		assertEquals(-2f, st.getBalance(), 0.01f);
		assertEquals(5, st.getHandsPlayed());
		assertEquals(2, st.getLost());
		assertEquals(5, st.getWon());
		assertEquals(3, st.getVolBets());
		assertEquals(0, st.getRebuys());
		assertEquals("test", st.getGamesPlayed().get(0));
		
		// Update stats for a new game
		assertTrue(sas.updateAccountStats(acc, newStats, "test2"));
		
		newAcc = sas.getAccount(org);
		assertNotNull(newAcc);
		st = newAcc.getStats();
		assertNotNull(st);
		assertEquals(-4f, st.getBalance(), 0.01f);
		assertEquals(10, st.getHandsPlayed());
		assertEquals(4, st.getLost());
		assertEquals(10, st.getWon());
		assertEquals(6, st.getVolBets());
		assertEquals(0, st.getRebuys());
		assertEquals("test", st.getGamesPlayed().get(0));
		assertEquals("test2", st.getGamesPlayed().get(1));
	}
	
	@Test
	public void testSchedulingCount() {
		Map<String, Long> scheduled = new HashMap<>();
		final long now = System.currentTimeMillis();
		
		// no games scheduled, except new
		assertEquals(1, sas.getScheduledGameCount(scheduled, now + DAY));
		
		// 1 yesterday, 1 tomorrow
		scheduled.put("A", now-DAY);
		assertEquals(2, sas.getScheduledGameCount(scheduled, now+DAY));
		
		// 4 games in last 30 days incl. new
		scheduled.put("B", now - (DAY * 10));
		scheduled.put("C", now - (DAY * 25));
		assertEquals(4, sas.getScheduledGameCount(scheduled, now + DAY));
		
		// Older than 30 days discounted - Okay
		scheduled.put("D", now - (DAY * 30));
		scheduled.put("E", now - (DAY * 32));
		assertEquals(4, sas.getScheduledGameCount(scheduled, now + DAY));
		
		// just 1 in 35 days - Okay 
		scheduled.clear();
		assertEquals(1, sas.getScheduledGameCount(scheduled, now + (35 * DAY)));
		
		// a day ago, and 30 days time - okay
		scheduled.put("A", now-DAY);
		assertEquals(1, sas.getScheduledGameCount(scheduled, now + (30 * DAY)));
		
		// a day ago and 29 days time
		assertEquals(2, sas.getScheduledGameCount(scheduled, now + (29 * DAY)));
		
		// In 10 days and another 10 days - not okay
		scheduled.clear();
		scheduled.put("A", now + (10 * DAY));
		assertEquals(2, sas.getScheduledGameCount(scheduled, now + (10 * DAY)));
		
		// In 10 days and 30 days - not okay
		assertEquals(2, sas.getScheduledGameCount(scheduled, now + (30 * DAY)));
		
		// In 10 days and 41 days - Okay
		assertEquals(1, sas.getScheduledGameCount(scheduled, now + (41 * DAY)));
	}
	
	private GameSettings mkSettings(String oName, String hName, long timeAdd) {
		GameSettings ths = mock(GameSettings.class);
		lenient().when(ths.getOrganiserName()).thenReturn(oName);
		lenient().when(ths.getOrganiserId()).thenReturn(oName);
		lenient().when(ths.getOrganiserEmail()).thenReturn(oName + "@" + oName);
		
		lenient().when(ths.getHostName()).thenReturn(hName);
		lenient().when(ths.getHostEmail()).thenReturn(hName + "@" + hName);
		lenient().when(ths.getScheduledTime()).thenReturn(System.currentTimeMillis() + timeAdd);
		return ths;
	}
	
	private class TestStats extends PlayerStats {
		TestStats() {
			this.balance = -2f;
			this.handsPlayed = 5;
			this.lost = 2;
			this.won = 5;
			this.volBets=3;
		}
		
	}
}
