package com.langleydata.homepoker.game.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mock;

import com.langleydata.homepoker.api.GameSettings;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.mock;

public class PlayerTest {

	@Mock
	private GameSettings settings;

	
	@Test 
	public void testDoPlayerAction() {
		final Player p = mkPlayer("A");
		
		// TODO Test doing player actions
		//p.doPlayerAction(action, gameState, settings, maxRebuy);
	}
	@Test
	public void testCashOut() {
		final Player p = mkPlayer("A");
		
		assertEquals(10, p.getCurrentStack().getStack(), 0.1);
		assertEquals(10, p.getCurrentStack().getWallet(), 0.1);
		
		// when
		final float wallet = p.cashOut();
		assertEquals(20f, wallet, 0.1);
		assertTrue(p.getState().isCashedOut());
	}
	
	@Test
	public void testClonePlayer() {
		
	}
	
	private Player mkPlayer(final String id) {
		final Player p = new Player(id, id);
		p.getCurrentStack().initialise(20f, false);
		p.getCurrentStack().reBuy(10, 0);
		return p;
	}
}
