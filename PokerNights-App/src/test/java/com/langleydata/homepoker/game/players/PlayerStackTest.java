package com.langleydata.homepoker.game.players;

import static com.langleydata.homepoker.game.texasHoldem.TexasGameState.FLOP;
import static com.langleydata.homepoker.game.texasHoldem.TexasGameState.POST_DEAL;
import static com.langleydata.homepoker.game.texasHoldem.TexasGameState.PRE_DEAL;
import static com.langleydata.homepoker.game.texasHoldem.TexasGameState.RIVER;
import static com.langleydata.homepoker.game.texasHoldem.TexasGameState.TURN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.langleydata.homepoker.game.players.PlayerStack.RebuyState;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;


public class PlayerStackTest {
	private PlayerStack ps = null;
	
	@Before
	public void init() {
		ps = new PlayerStack();
		float deposit = 30f;
		ps.initialise(deposit, false);
		
		// transfer to stack
		ps.reBuy(10, 0);
	}
	
	
	@Test
	public void testReverseOverBet() {
		assertTrue(ps.addToTable(FLOP, 1.6f));
		ps.collectBets();
		assertTrue(ps.addToTable(TURN, 2.4f));
		assertEquals(2.4, ps.getOnTable(), 0.001);
		
		assertEquals(1.2f, ps.reverseBet(1.2f, TURN), 0.001);
		
		assertEquals(1.2, ps.getOnTable(), 0.001);
		assertEquals(1.2, ps.getCommitedPerRound().get(TURN), 0.001);
		assertEquals(1.6, ps.getCommitedPerRound().get(FLOP), 0.001);
	}
	
	@Test
	public void testStatsWhenWin() {

		Assert.assertEquals(0, ps.getGameStats().getBalance(), 0.01);
		
		// Player wins
		ps.transferWinAmount(3);
		ps.getGameStats().updateStats(true);
		
		assertEquals(3, ps.getGameStats().getBalance(), 0.01);
		assertEquals(1, ps.getGameStats().getWon());
		assertEquals(0, ps.getGameStats().getLost());
		assertEquals(1, ps.getGameStats().getHandsPlayed());
	}
	
	@Test
	public void testHostControlledWallet() {
		ps = new PlayerStack();
		ps.initialise(0, true);
		
		assertEquals(RebuyState.NO_FUNDS, ps.reBuy(10, 0f));
		assertEquals(0f, ps.getStack(), 0.001f);
		assertEquals(0f, ps.getWallet(), 0.001f);
		
		assertTrue(ps.assignWallet(20, 10));
		assertEquals(10f, ps.getStack(), 0.001f);
		assertEquals(10f, ps.getWallet(), 0.001f);
		assertEquals(0, ps.getGameStats().getRebuys());
		assertFalse(ps.isWOFA());
		
	}
	
	@Test
	public void testWOFAAfterAllIn() {
		ps = new PlayerStack();
		ps.initialise(0, true);
		
		assertTrue(ps.assignWallet(10, 10));
		assertFalse(ps.isWOFA());
		
		ps.addToTable(TURN, 10);
		ps.collectBets();
		
		assertTrue(ps.isWOFA());
		assertEquals(RebuyState.NO_FUNDS, ps.reBuy(10, 0f));
		
	}
	
	@Test
	public void testCantAddToWalletWhenHostControlled() {
		ps = new PlayerStack();
		ps.initialise(50, true);
		
		assertEquals(0f, ps.getStack(), 0.001f);
		assertEquals(0f, ps.getWallet(), 0.001f);
		
		assertTrue(ps.assignWallet(10, 10));
		
		assertEquals(10f, ps.getStack(), 0.001f);
		assertEquals(0f, ps.getWallet(), 0.001f);
	}
	
	@Test
	public void testAssignWalletFailsWhenNotEnough() {
		ps = new PlayerStack();
		ps.initialise(0, true);
		
		assertEquals(RebuyState.NO_FUNDS, ps.reBuy(10, 0f));
		assertEquals(0f, ps.getStack(), 0.001f);
		assertEquals(0f, ps.getWallet(), 0.001f);
		assertFalse(ps.assignWallet(5, 10));
	}
	
	@Test
	public void testStatsWhenLose() {
		
		// Player looses
		ps.addToTable(TexasGameState.COMPLETE, 3);
		ps.collectBets();
		ps.getGameStats().updateStats(false);
		
		Assert.assertEquals(-3, ps.getGameStats().getBalance(), 0.01);
		Assert.assertEquals(0, ps.getGameStats().getWon());
		Assert.assertEquals(1, ps.getGameStats().getLost());
		Assert.assertEquals(1, ps.getGameStats().getHandsPlayed());
	}
	
	@Test
	public void testStatsWhenLoseThenCashOut() {
		
		// Player looses
		ps.addToTable(TexasGameState.COMPLETE, 3);
		ps.collectBets();
		ps.getGameStats().updateStats(false);
		
		// Player wins
		ps.transferWinAmount(5);
		ps.getGameStats().updateStats(true);
		
		assertEquals(2, ps.getGameStats().getBalance(), 0.01);
		assertEquals(1, ps.getGameStats().getWon());
		assertEquals(1, ps.getGameStats().getLost());
		assertEquals(2, ps.getGameStats().getHandsPlayed());
		
		assertTrue(ps.cashOut());
		
		assertEquals(2, ps.getGameStats().getBalance(), 0.01);
		assertEquals(1, ps.getGameStats().getWon());
		assertEquals(1, ps.getGameStats().getLost());
		assertEquals(2, ps.getGameStats().getHandsPlayed());
		
	}
	
	@Test
	public void testRebuyWhenZero() {
		ps.addToTable(TexasGameState.COMPLETE, 10f);
		ps.collectBets();
		
		assertEquals(RebuyState.SUCCESS, ps.reBuy(10f, 0f));
		assertEquals(10f, ps.getStack(), 0.001f);
		assertEquals(10f, ps.getWallet(), 0.001f);
	}
	
	@Test
	public void testCantRebuyWithNoMoney() {
		
		ps = new PlayerStack();
		float deposit = 10f;
		ps.initialise(deposit, false);
		ps.reBuy(10, 0.1f);
		
		ps.addToTable(TexasGameState.FLOP, 10f);
		ps.collectBets();
		
		assertEquals(RebuyState.NO_FUNDS, ps.reBuy(10f, 0f));
		assertEquals(0f, ps.getStack(), 0.001f);
		assertEquals(0f, ps.getWallet(), 0.001f);
	}
	
	@Test
	public void testRebuyWhenZeroAfterMultipleBets() {
		ps.addToTable(PRE_DEAL, 0.4f);
		ps.collectBets();
		ps.addToTable(POST_DEAL, 0.8f);
		ps.collectBets();
		ps.addToTable(FLOP, 4.9f);
		ps.collectBets();
		ps.addToTable(TURN, 1.65f);
		ps.collectBets();
		ps.addToTable(RIVER, 2.25f);
		ps.collectBets();
		assertEquals(RebuyState.SUCCESS, ps.reBuy(10f, 0f));
		assertEquals(10f, ps.getStack(), 0.001f);
		assertEquals(10f, ps.getWallet(), 0.001f);
	}
	
	@Test
	public void testCantCashOutWhilstBetting() {
		ps.addToTable(PRE_DEAL, 0.4f);
		assertFalse(ps.cashOut());
	}
	
}
