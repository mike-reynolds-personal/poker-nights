package com.langleydata.homepoker.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.langleydata.homepoker.TestUtils;
import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.ShuffleOption;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.exception.InvalidPlayerException;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.GameUpdateMessage;

public class AbstractCardGameTest {
	TestGame tg;
	
	@Before
	public void setup() {
		TexasHoldemSettings settings = new TexasHoldemSettings();
		settings.setFormat(GameFormat.CASH);
		tg = new TestGame(settings);
	}
	
	@Test
	public void testTournamentPlayerDeposit() {
		TexasHoldemSettings settings = new TexasHoldemSettings();
		settings.setFormat(GameFormat.TOURNAMENT);
		settings.setBlindIncreaseInterval(10 * 60 * 1000);
		settings.setOpeningStack(1000);
		
		tg = new TestGame(settings);
		
		Player pA = mkBasicPlayer("A", 10);
		tg.addPlayer(pA);
		
		assertEquals(1, tg.getPlayers().size());
		assertEquals(1000, pA.getCurrentStack().getStack(), 0.1);
		assertEquals(0, pA.getCurrentStack().getWallet(), 0.1);
		
	}
	
	@Test
	public void testDeckShuffledBeforeFirstPlay() {
		final long firstSeed = tg.getDeck().getLastSeed();
		assertEquals(0, tg.getRound());
		tg.getSettings().setHostEmail("A");
		tg.getSettings().setShuffleOption(ShuffleOption.NEVER);// To ensure cards are still shuffled, regardless
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		
		tg.startNextRound(false);
		
		assertEquals(1, tg.getRound());
		assertNotEquals(firstSeed, tg.getDeck().getLastSeed());
	}
	@Test
	public void testAddPlayerWithHostControlFunds() {
		tg.getSettings().setHostControlledWallet(true);
		Player p = new Player("A", "A");
		p.setSeatingPos(0);
		p.setSessionId("A");
		p.setEmail("email");
		p.getCurrentStack().setStack(0f);
		
		tg.addPlayer(p);
		
		Assert.assertNotNull(tg.getPlayers().getPlayerById("A"));
		assertTrue(p.getState().isSittingOut());
		assertTrue(p.getState().isSONR());
	}
	
	@Test
	public void testJoiningExistingGame() {
		/* So before the game has been started, we want all new players to be 
		 * in the game and ready to play once the dealer starts.
		 * If the game has already started (and a player joins late), they
		 * want to be 'sitting out' until the game completes.
		 * The UI will show 'Sit out next round' if the sittingOut flag is true
		 */
		
		Player p1 = makePlayer("A", 1);
		Player p2 = makePlayer("B", 3);
		Player p3 = makePlayer("C", 5);
		Player p4 = makePlayer("D", 7);
		Player p5 = makePlayer("E", 2);
		
		tg.addPlayer(p1);
		tg.addPlayer(p2);
		tg.addPlayer(p3);
		
		// Initial state
		assertPlayerInitial(p1);
		assertPlayerInitial(p2);
		assertPlayerInitial(p3);
		assertFalse(p1.getState().isSittingOut());
		assertFalse(p2.getState().isSittingOut());
		assertFalse(p3.getState().isSittingOut());
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		assertEquals(3, tg.getPlayers().size());
		assertEquals(3, tg.getPlayers().getPlayersInHand().size());
		assertEquals(p1, tg.moveDealerPosition());// set the dealer
		assertEquals(p1, tg.getPlayers().getActionOn());
		
		// Got 3 players ready in-hand
		assertEquals(3, tg.getPlayers().getPlayersInHand().size());
		
		// Update the game state and add a new 'late' player
		tg.gameState = TexasGameState.PRE_DEAL;
		tg.addPlayer(p4);
		
		assertFalse(p4.isStillInHand());
		assertTrue(p4.getState().isSittingOut());
		
		// Update the game state and move the dealer
		tg.gameState = TexasGameState.FLOP;
		// move the dealer around a couple positions
		assertEquals(p2, tg.moveDealerPosition());
		assertEquals(p3, tg.moveDealerPosition());
		assertEquals(p1, tg.moveDealerPosition());// back to start, skipping the p4
		
		// p4 still out and not dealer
		assertFalse(p4.isStillInHand());
		assertFalse(p4.getState().isActionOnMe());
		assertFalse(p4.getState().isDealer());
		assertTrue(p4.getState().isSittingOut());
		
		// insert a new player between A and B
		tg.addPlayer(p5);
		assertFalse(p5.isStillInHand());
		assertTrue(p5.getState().isSittingOut());

		// move the dealer which should skip the new player
		assertEquals(p2, tg.moveDealerPosition());
		
		// test the new player state
		assertFalse(p5.isStillInHand());
		assertFalse(p5.getState().isActionOnMe());
		assertFalse(p5.getState().isDealer());
		assertTrue(p5.getState().isSittingOut());
	}
	
	@Test
	public void testTogglePlayerInNextRound() {
		Player p1 = makePlayer("A", 1);
		Player p2 = makePlayer("B", 3);
		Player p3 = makePlayer("C", 5);
		tg.addPlayer(p1);
		tg.addPlayer(p2);
		assertPlayerInitial(p1);
		assertPlayerInitial(p2);
		
		tg.gameState = TexasGameState.FLOP;
		
		// Add a new player (auto sit out)
		tg.addPlayer(p3);
		assertFalse(p3.isStillInHand());
		assertFalse(p3.getState().isActionOnMe());
		
		// toggle the player in for next round
		p3.getState().toggleSittingOut(TexasGameState.FLOP, false);
		assertFalse(p3.isStillInHand());// still out at the moment
		assertTrue(p3.getState().isSittingOut());
		
		tg.gameState = TexasGameState.COMPLETE;
		tg.getPlayers().forEach(p->p.resetForNewRound(10));
		
		// all players should now be in the next game
		assertEquals(3, tg.getPlayers().getPlayersInHand().size());
		assertPlayerInitial(p3);
		assertFalse(p3.getState().isSittingOut());
		
	}
	
	@Test
	public void testTogglePlayerOutNextRound() {
		
		
		Player p1 = makePlayer("A", 1);
		Player p2 = makePlayer("B", 3);
		tg.addPlayer(p1);
		tg.addPlayer(p2);
		assertPlayerInitial(p1);
		assertPlayerInitial(p2);
		
		tg.gameState = TexasGameState.PRE_DEAL;
		
		// Players joined before game starts, so both in
		assertEquals(2, tg.getPlayers().getPlayersInHand().size());
		
		// p2 sits out next round, but is still in this one
		p2.getState().toggleSittingOut(TexasGameState.PRE_DEAL, true);
		assertEquals(2, tg.getPlayers().getPlayersInHand().size());
		assertTrue(p2.isStillInHand());
		assertFalse(p2.getState().isSittingOut());
		
		// simulate the next card, player still in 
		tg.getPlayers().forEach(p->p.getState().resetForNewDeal());
		assertTrue(p2.isStillInHand());
		assertFalse(p2.getState().isSittingOut());
		
		// complete the current game
		tg.gameState = TexasGameState.COMPLETE;
		tg.getPlayers().forEach(p->p.resetForNewRound(10));
		
		// player now automatically sitting out
		assertEquals(1, tg.getPlayers().getPlayersInHand().size());
		assertFalse(p2.isStillInHand());
		assertTrue(p2.getState().isSittingOut());
		
		// toggle back in for the next hand, but still out this one
		p2.getState().toggleSittingOut(TexasGameState.FLOP, false);
		assertFalse(p2.isStillInHand());
		assertTrue(p2.getState().isSittingOut());
		
		// complete the current game
		tg.gameState = TexasGameState.COMPLETE;
		tg.getPlayers().forEach(p->p.resetForNewRound(10));
		assertTrue(p2.isStillInHand());
		assertFalse(p2.getState().isSittingOut());
		
	}
	
	private void assertPlayerInitial(Player player) {
		assertTrue(player.isStillInHand());
		assertFalse(player.getState().isFolded());
		assertFalse(player.getState().isActionOnMe());
		assertFalse(player.getState().isAllIn());
		assertFalse(player.getState().isDealer());
	}
	
	@Test
	public void testRemovePlayerCashesOut() {
		PlayerRemovedListener mkListener = Mockito.mock(PlayerRemovedListener.class);
		tg.addRemovePlayerListener(mkListener);
		final String gameId = tg.getSettings().getGameId();
		final Player a = TestUtils.makePlayer("A", -1);
		tg.addPlayer( a );
		tg.addPlayer( makePlayer("B", -1));
		tg.addPlayer( makePlayer("C", -1));
		
		assertEquals(10f, a.getCurrentStack().getStack(), 0.01);
		assertEquals(10f, a.getCurrentStack().getWallet(), 0.01);
		
		final Player removed = tg.removePlayer("A");
		assertEquals(20f, removed.getCurrentStack().getWallet(), 0.01);
		Assert.assertNull(tg.getPlayers().getPlayerById("A"));

		// The callback is called
		Mockito.verify(mkListener).playerRemoved(gameId, a);
	}
	
	@Test
	public void testRemoveDealerChangesDealer() {
		final Player a = makePlayer("A", -1);
		final Player b = makePlayer("B", -1);
		a.getState().setDealer(true);
		a.getState().setHost(true);
		
		tg.gameState = TexasGameState.FLOP;
		tg.addPlayer( a );
		tg.addPlayer( b );
		tg.addPlayer( makePlayer("C", -1));
		
		assertTrue(a.getState().isDealer());
		
		Assert.assertNotNull(tg.removePlayer("A"));
		assertTrue(b.getState().isDealer());
		assertTrue(b.getState().isHost());
		Assert.assertNull(tg.getPlayers().getPlayerById("A"));
		
		// Not enough players, so should now be complete
		assertEquals(TexasGameState.COMPLETE, tg.gameState);
	}
	
	@Test
	public void testCantAddTooManyPlayers() {
		tg.addPlayer( makePlayer("A", -1));
		tg.addPlayer( makePlayer("B", -1));
		tg.addPlayer( makePlayer("C", -1));
		tg.addPlayer( makePlayer("D", -1));
		tg.addPlayer( makePlayer("E", -1));
		tg.addPlayer( makePlayer("F", -1));
		tg.addPlayer( makePlayer("G", -1));
		tg.addPlayer( makePlayer("H", -1));
		tg.addPlayer( makePlayer("I", -1));
		
		try {
			tg.addPlayer( makePlayer("J", -1));
		} catch (InvalidPlayerException ipe) {
			assertEquals("Max players reached - rejecting user", ipe.getMessage());
		}
	}
	
	@Test
	public void testUserWalletsMustBeSufficient() {
		Player p1 = TestUtils.makePlayer("A", "A", 1, 5);
		try {
			tg.addPlayer(p1);
		} catch (InvalidPlayerException ipe) {
			assertEquals("Player has insufficient funds in wallet", ipe.getMessage());
		}
	}
	
	@Test
	public void testCantAddDuplicatePlayer() {
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("A", -1);
		
		tg.addPlayer(p1);
		try {
			tg.addPlayer(p2);
		} catch (InvalidPlayerException ipe) {
			assertEquals("Player with same details already at table", ipe.getMessage());
		}
		
		p2 = makePlayer("", -1);
		try {
			tg.addPlayer(p2);
		} catch (InvalidPlayerException ipe) {
			assertEquals("No player ID, handle or handle contains a space", ipe.getMessage());
		}
		
		p2 = makePlayer("A", "AAAA", -1);
		try {
			tg.addPlayer(p2);
		} catch (InvalidPlayerException ipe) {
			assertEquals("Player with same details already at table", ipe.getMessage());
		}
		
		p2 = makePlayer("B", "BBBB", -1);
		tg.addPlayer(p2);
		
		Player p3 = makePlayer("C", "BBBB", -1);
		try {
			tg.addPlayer(p3);
		} catch (InvalidPlayerException ipe) {
			assertEquals("Player with same details already at table", ipe.getMessage());
		}
		
		p3 = makePlayer("C", "", -1);
		try {
			tg.addPlayer(p3);
		} catch (InvalidPlayerException ipe) {
			assertEquals("No player ID, handle or handle contains a space", ipe.getMessage());
		}
		
		p3 = makePlayer(null, "CC", -1);
		try {
			tg.addPlayer(p3);
		} catch (InvalidPlayerException ipe) {
			assertEquals("No player ID, handle or handle contains a space", ipe.getMessage());
		}
		
		p3 = makePlayer("C", "CC", -1);
		tg.addPlayer(p3);
	}
	
	@Test
	public void testBuyInCorrectly() {
		// wallet created with 20, default settings = 10 buy-in
		Player p1 = TestUtils.makePlayer("A", -1);
		
		
		tg.addPlayer(p1);
		Assert.assertEquals(0, p1.getCurrentStack().getOnTable(), 0.0001);
		Assert.assertEquals(10, p1.getCurrentStack().getStack(), 0.0001);
		Assert.assertEquals(10, p1.getCurrentStack().getWallet(), 0.0001);
		
		// Check can't force a buy-in twice
		Player p2 = TestUtils.makePlayer("B", -1);
		tg.addPlayer(p2);
		Assert.assertEquals(0, p2.getCurrentStack().getOnTable(), 0.0001);
		Assert.assertEquals(10, p2.getCurrentStack().getWallet(), 0.0001);
		Assert.assertEquals(10, p2.getCurrentStack().getStack(), 0.0001);
	}
	
	@Test
	public void testMovingTheDealer() {
		
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("B", -1);
		Player p3 = makePlayer("C", -1);
		Player p4 = makePlayer("D", -1);
		
		tg.addPlayer(p1);
		tg.addPlayer(p2);
		tg.addPlayer(p3);
		tg.addPlayer(p4);

		// Will default to the first player being dealer
		Player dealer = tg.getPlayers().getDealer();
		Assert.assertEquals(0, dealer.getSeatingPos());

		tg.moveDealerPosition();
		testDealMoved(0, tg.getPlayers());
		
		// Move the dealer
		tg.moveDealerPosition();
		testDealMoved(1, tg.getPlayers());
		
		tg.moveDealerPosition();
		testDealMoved(2, tg.getPlayers());
		
		tg.moveDealerPosition();
		testDealMoved(3, tg.getPlayers());
		
		// last move resets back to the first player
		tg.moveDealerPosition();
		testDealMoved(0, tg.getPlayers());
	}

	private void testDealMoved(int newDealerPos, Players g) {
		Assert.assertEquals(newDealerPos, g.getDealer().getSeatingPos());
		Assert.assertTrue(g.getDealer().getState().isDealer());
	}

	private Player makePlayer(final String id, final int pos) {
		return makePlayer(id, id, pos);
	}
	private Player makePlayer(final String id, String handle, final int pos) {
		return TestUtils.makePlayer(id, handle, pos, 10 );
	}
	private Player mkBasicPlayer(String id, int buyin) {
		Player p = new Player(id, id);
		p.setSessionId(id);
		p.setEmail(id);
		p.getCurrentStack().setStack(buyin);
		return p;
	}
	
	private class TestGame extends AbstractCardGame<TexasHoldemSettings> {

		public TestGame(TexasHoldemSettings settings) {
			super(settings);
		}
		
		@Override
		public Player startNextRound(boolean moveDealer) {
			return super.startNextRound(moveDealer);
		}

		@Override
		public GameUpdateMessage doGameUpdateAction(PlayerAction playerAction) {
			return Mockito.mock(GameUpdateMessage.class);
		}

		@Override
		public DealResult deal() {
			return DealResult.NO_DEAL;
		}

		@Override
		protected String getActionSound(TexasGameState prevState, PlayerAction action) {
			return null;
		}

		@Override
		public Player pausePlayer(String playerId, String sessionId) {
			return null;
		}

		@Override
		public void completeGame() {
		}
	
	}
}
