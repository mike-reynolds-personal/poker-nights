package com.langleydata.homepoker.player;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class PlayerStatsTest {

	@Test
	@Ignore("Accumulation code has been removed for now")
	public void testUpdateWithExisting() {
		TestStats ts = new TestStats();
		TestStats up = new TestStats();
		
		ts.balance = -2f;
		ts.gamesPlayedIn.add("test");
		ts.handsPlayed = 5;
		ts.lost = 2;
		ts.won = 5;
		ts.volBets=3;
		ts.rebuys = -1;
		ts.timePlayed = 10;
		
		up.balance = -2f;
		up.handsPlayed = 5;
		up.lost = 2;
		up.won = 5;
		up.volBets=3;
		up.rebuys = 2;
		up.timePlayed = 20;
		
		ts.updateStats(up, "test");
		
		assertEquals(-2f, ts.balance, 0.01);
		assertEquals(5, ts.handsPlayed, 0.01);
		assertEquals(2, ts.lost, 0.01);
		assertEquals(5, ts.won, 0.01);
		assertEquals(3, ts.volBets, 0.01);
		assertEquals(2, ts.getRebuys());
		assertEquals(20, ts.getTimePlayed());
		assertEquals("test", ts.getGamesPlayed().get(0));
		assertEquals(1, ts.getGamesPlayed().size());
	}

	@Test
	@Ignore
	public void testUpdateWithoutExisting() {
		TestStats ts = new TestStats();
		TestStats up = new TestStats();
		
		ts.balance = -2f;
		ts.gamesPlayedIn.add("test");
		ts.handsPlayed = 5;
		ts.lost = 2;
		ts.won = 5;
		ts.volBets = 3;
		ts.timePlayed = 10;
		
		up.balance = -2f;
		up.handsPlayed = 5;
		up.lost = 2;
		up.won = 5;
		up.volBets = 3;
		up.timePlayed = 20;
		
		ts.updateStats(up, "test1");
		
		assertEquals(-4f, ts.balance, 0.01);
		assertEquals(10, ts.handsPlayed, 0.01);
		assertEquals(4, ts.lost, 0.01);
		assertEquals(10, ts.won, 0.01);
		assertEquals(6, ts.volBets, 0.01);
		assertEquals(30, ts.getTimePlayed());
		assertEquals("test", ts.getGamesPlayed().get(0));
		assertEquals("test1", ts.getGamesPlayed().get(1));
		assertEquals(2, ts.getGamesPlayed().size());
	}
	
	private class TestStats extends PlayerStats {
		
	}
}
