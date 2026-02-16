package com.langleydata.homepoker.game.players;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mock;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;

public class PlayerStateTest {
	@Mock
	private GameSettings settings;
	

	@Test
	public void testSitOutToggleSuccessInGame() {
		final Player p = mkPlayer("A");
		final PlayerState state = p.getState();
		TexasGameState gState = TexasGameState.FLOP;
		
		// Sit out next round
		assertTrue(state.toggleSittingOut(gState, true));
		assertFalse(state.isSittingOut());
		assertTrue(state.isSONR());
		
		// Same action is rejected
		assertFalse(state.toggleSittingOut(gState, true));
		assertFalse(state.isSittingOut());
		assertTrue(state.isSONR());
		
		// Toggle off
		assertTrue(state.toggleSittingOut(gState, false));
		assertFalse(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// Same action rejected
		assertFalse(state.toggleSittingOut(gState, false));
		
		// Toggle on again
		assertTrue(state.toggleSittingOut(gState, true));
		assertFalse(state.isSittingOut());
		assertTrue(state.isSONR());
	}
	
	@Test
	public void testSitOutToggleSuccessEndOfGame() {
		final Player p = mkPlayer("A");
		final PlayerState state = p.getState();
		TexasGameState gState = TexasGameState.COMPLETE;
		
		// Sit out now
		assertTrue(state.toggleSittingOut(gState, true));
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// Same action is rejected
		assertFalse(state.toggleSittingOut(gState, true));
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// Toggle off
		assertTrue(state.toggleSittingOut(gState, false));
		assertFalse(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// Same action rejected
		assertFalse(state.toggleSittingOut(gState, false));
		
		// Toggle on again
		assertTrue(state.toggleSittingOut(gState, true));
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
	}
	
	@Test
	public void testSitOutToggleOnAcrossRounds() {
		final Player p = mkPlayer("A");
		final PlayerState state = p.getState();
		
		// Sit out next round
		assertTrue(state.toggleSittingOut(TexasGameState.FLOP, true));
		assertFalse(state.isSittingOut());
		assertTrue(state.isSONR());
		
		// end of game
		state.resetForNewRound(1f);
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// We are sat-out, now indicate back in
		assertTrue(state.toggleSittingOut(TexasGameState.FLOP, false));
		assertTrue(state.isSittingOut());//still out
		assertFalse(state.isSONR());// Back in next round
		
		// end of game - sit back in
		state.resetForNewRound(1f);
		assertFalse(state.isSittingOut());
		assertFalse(state.isSONR());
	}
		
	@Test
	public void testSitOutToggleOffAcrossRounds() {
		final Player p = mkPlayer("A");
		final PlayerState state = p.getState();
		
		// Sit out immediately
		assertTrue(state.toggleSittingOut(TexasGameState.COMPLETE, true));
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// Now indicate to sit back in next round
		assertTrue(state.toggleSittingOut(TexasGameState.FLOP, false));
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// same action rejected
		assertFalse(state.toggleSittingOut(TexasGameState.FLOP, false));
		assertTrue(state.isSittingOut());
		assertFalse(state.isSONR());
		
		// end of game
		state.resetForNewRound(1f);
		assertFalse(state.isSittingOut());
		assertFalse(state.isSONR());
		
	}
	
	@Test
	public void testResetForNewGame() {
		final Player p = mkPlayer("A");
		// todo
	}
	
	private Player mkPlayer(final String id) {
		final Player p = new Player(id, id);
		p.getCurrentStack().initialise(20f, false);
		p.getCurrentStack().reBuy(10, 0);
		return p;
	}
}
