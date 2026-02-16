package com.langleydata.homepoker.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.players.PlayerState;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;

public class PlayerStateTest {
	GameSettings mkSettings = mock(GameSettings.class);
	
	@Before
	public void setup() {
		Mockito.when(mkSettings.isHostControlledWallet()).thenReturn(false);
		Mockito.when(mkSettings.getActionTimeout()).thenReturn(300);
	}
	
	@Test
	public void testSettingActionSetsAutoInaction() {
		final PlayerState ps = new PlayerState();
		ps.initialise(TexasGameState.POST_DEAL, mkSettings);
		
		ps.setActionOn(true);
		assertTrue(ps.getNextAutoInaction() > System.currentTimeMillis());
		
		ps.setActionOn(false);
		assertEquals(-1, ps.getNextAutoInaction());
	}
	
	@Test
	public void testPlayerSitsOutWhenJoiningLateButDealtInNextRound() {
		final PlayerState ps = new PlayerState();
		ps.initialise(TexasGameState.POST_DEAL, mkSettings);
		
		// a new round
		ps.resetForNewDeal();
		
		assertTrue(ps.isSittingOut());
		
		// new game starts
		ps.resetForNewRound(2);

		assertFalse(ps.isSittingOut());
		
		ps.resetForNewDeal();
		assertFalse(ps.isSittingOut());
		
		ps.resetForNewRound(2);
		assertFalse(ps.isSittingOut());
	}
	
	@Test 
	public void testPlayerInGameIfJoinsBeforeStart() {
		final PlayerState ps = new PlayerState();
		ps.initialise(TexasGameState.COMPLETE, mkSettings);
		
		assertFalse(ps.isSittingOut());
		
		// a new round
		ps.resetForNewDeal();
		assertFalse(ps.isSittingOut());
		
		// new game
		ps.resetForNewRound(2);
		
		// Still in
		assertFalse(ps.isSittingOut());
	}
	
	@Test
	public void testPlayerSitsOutInRoundButStaysActive() {
		final PlayerState ps = new PlayerState();

		assertFalse(ps.isSittingOut());
		
		ps.resetForNewRound(2);
		
		// Player toggles out
		ps.toggleSittingOut(TexasGameState.PRE_DEAL, true);
		assertFalse(ps.isSittingOut());
		
		// a new round
		ps.resetForNewDeal();
		assertFalse(ps.isSittingOut());
		
		// new game
		ps.resetForNewRound(2);
		
		// Is now out
		assertTrue(ps.isSittingOut());
		
		// a new round
		ps.resetForNewDeal();
		assertTrue(ps.isSittingOut());
		
		// Another new game
		ps.resetForNewRound(2);
		
		// Player still out (as haven't toggled in)
		assertTrue(ps.isSittingOut());
		
	}
	
	@Test
	public void testSatOutWhenWOFA() {
		PlayerState ps = new PlayerState();
		Mockito.when(mkSettings.isHostControlledWallet()).thenReturn(true);
		ps.initialise(TexasGameState.COMPLETE, mkSettings);
		assertTrue(ps.isSittingOut());
		assertTrue(ps.isSONR());
		
		ps = new PlayerState();
		ps.initialise(TexasGameState.FLOP, mkSettings);
		assertTrue(ps.isSittingOut());
		assertTrue(ps.isSONR());
		
		ps.resetForNewDeal();
		assertTrue(ps.isSittingOut());
		assertTrue(ps.isSONR());
	}
	
	@Test
	public void testPlayerTogglesOutThenIn() {
		final PlayerState ps = new PlayerState();
		ps.initialise(TexasGameState.COMPLETE, mkSettings);
		
		assertFalse(ps.isSittingOut());
		
		// Player toggles out
		ps.toggleSittingOut(TexasGameState.FLOP, true);
		assertFalse(ps.isSittingOut());
		
		// Player toggles back in
		ps.toggleSittingOut(TexasGameState.FLOP, false);
		assertFalse(ps.isSittingOut());
		
		// New Game
		ps.resetForNewRound(2);
		
		// Player still in 
		assertFalse(ps.isSittingOut());
	}
	
	@Test
	public void testPlayerTogglesOutThenInBeforeGameStarts() {
		final PlayerState ps = new PlayerState();

		assertFalse(ps.isSittingOut());
		
		// Player toggles out
		ps.toggleSittingOut(TexasGameState.COMPLETE, true);
		assertTrue(ps.isSittingOut());
		
		// Player toggles back in
		ps.toggleSittingOut(TexasGameState.COMPLETE, false);
		assertFalse(ps.isSittingOut());
		
		// New Game
		ps.resetForNewRound(2);
		
		// Player still in 
		assertFalse(ps.isSittingOut());
	}
	
	@Test
	public void testPlayerTogglesOutBeforeGameStarts() {
		final PlayerState ps = new PlayerState();

		assertFalse(ps.isSittingOut());
		
		// Player toggles out
		ps.toggleSittingOut(TexasGameState.COMPLETE, true);
		assertTrue(ps.isSittingOut());
		
		// New Game
		ps.resetForNewRound(2);
		
		// Player still in 
		assertTrue(ps.isSittingOut());
		
		ps.resetForNewDeal();
		
		// Another new Game, and they're still out
		ps.resetForNewRound(2);
		assertTrue(ps.isSittingOut());
	}
}
