package com.langleydata.homepoker.game.texasHoldem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import com.langleydata.homepoker.TestUtils;
import com.langleydata.homepoker.api.AccountStats;
import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.api.CardGame.GameFormat;
import com.langleydata.homepoker.api.CardGame.GameUpdateType;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.exception.GameStartException;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.PlayerStack;
import com.langleydata.homepoker.game.players.PlayerState;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;
import com.langleydata.homepoker.message.AnteDueMessage;
import com.langleydata.homepoker.message.CompleteGameMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.message.ShowCardsMessage;

@RunWith(MockitoJUnitRunner.class)
public class TexasHoldemGameTest {
	final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	
	private TexasHoldemGame tg;
	
	@Mock
	private GamePots gamePot;
	@Mock
	private MessageUtils msgUtils;
	@Mock
	private PokerHandEvaluator sevenCardEvaluator;
	private TexasHoldemSettings settings = new TexasHoldemSettings();
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		settings.setFormat(GameFormat.CASH);
		tg = new TexasHoldemGame(settings, msgUtils);
		tg.setEvaluator(sevenCardEvaluator);
		Mockito.
			when(sevenCardEvaluator.calculatePotsAndWinners(Mockito.any(), Mockito.any())).thenReturn(gamePot);
		assertNull(tg.blindIncreaseTimer);
		assertNull(tg.inActionTimer);
	}
	
	private void initTournament() {
		settings.setFormat(GameFormat.TOURNAMENT);
		settings.setOpeningStack(1000);
		settings.setHostEmail("A");
		settings.setBlindIncreaseInterval(2*60*1000);
		settings.setTournamentSplit(Arrays.asList( new Integer[] {60,30,10}));
		tg = new TexasHoldemGame(settings, msgUtils);
		tg.setEvaluator(sevenCardEvaluator);
	}
	
	@Test
	public void testAutoActionPostBlind() throws InterruptedException {
		settings.setActionTimeout(2);
		
		tg = new TexasHoldemGame(settings, msgUtils);
		assertNotNull(tg.inActionTimer);
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		
		assertFalse(tg.doInactionProcess());
		
		tg.startNextRound(true);// So round num > 0

		assertEquals(Blinds.SMALL, pB.getState().getBlindsDue());
		
		assertFalse(tg.doInactionProcess());// below time threshold
		
		Thread.sleep(2000 + PlayerState.ACTION_MARGIN);
		assertTrue(tg.doInactionProcess());//force the process
		
		assertEquals(Blinds.NONE, pB.getState().getBlindsDue());
		assertEquals(PlayerActionType.POST_BLIND, pB.getState().getLastAction());
		
		assertTrue(pC.getState().isActionOnMe());// now on C
		
		Mockito.verify(msgUtils).sendBroadcastToTable(Mockito.anyString(), Mockito.any(GameUpdateMessage.class), Mockito.eq(500L));
		Mockito.verify(msgUtils).sendPrivateMessage(Mockito.eq("B"), Mockito.any(PlayerActionMessage.class));
	}
	
	@Test
	public void testAutoActionCheck() throws InterruptedException {
		settings.setActionTimeout(2);
		
		tg = new TexasHoldemGame(settings, msgUtils);
		assertNotNull(tg.inActionTimer);
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		
		assertFalse(tg.doInactionProcess());
		
		tg.startNextRound(true);// So round num > 0
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.8f);
		assertValidAction("A", PlayerActionType.BET, tg.getSettings().getBigBlind(), 9.8f);
		assertValidAction("B", PlayerActionType.BET, tg.getSettings().getBigBlind(), 9.8f);
		
		assertTrue(pC.getState().isActionOnMe());
		
		Thread.sleep(2000 + PlayerState.ACTION_MARGIN);
		assertTrue(tg.doInactionProcess());//force the process - Action now on C and as big-blind can simply check
		
		assertEquals(PlayerActionType.CHECK, pC.getState().getLastAction());
		
		assertFalse(pC.getState().isActionOnMe());// now on C
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		Mockito.verify(msgUtils).sendBroadcastToTable(Mockito.anyString(), Mockito.any(GameUpdateMessage.class), Mockito.eq(500L));
		Mockito.verify(msgUtils).sendPrivateMessage(Mockito.eq("C"), Mockito.any(PlayerActionMessage.class));
	}
	
	@Test
	public void testAutoActionFold() throws InterruptedException {
		settings.setActionTimeout(2);
		
		tg = new TexasHoldemGame(settings, msgUtils);
		assertNotNull(tg.inActionTimer);
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		
		assertFalse(tg.doInactionProcess());
		
		tg.startNextRound(true);// So round num > 0
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.8f);
		
		assertEquals("A", tg.getPlayers().getActionOn().getPlayerId());
		
		Thread.sleep(2000 + PlayerState.ACTION_MARGIN);
		assertTrue(tg.doInactionProcess());// force
		
		// A forced out
		assertTrue(pA.getState().isFolded());
		assertEquals("B", tg.getPlayers().getActionOn().getPlayerId());
				
		Mockito.verify(msgUtils).sendBroadcastToTable(Mockito.anyString(), Mockito.any(GameUpdateMessage.class), Mockito.eq(500L));
		Mockito.verify(msgUtils).sendPrivateMessage(Mockito.eq("A"), Mockito.any(PlayerActionMessage.class));
		
		Thread.sleep(2000 + PlayerState.ACTION_MARGIN);
		
		assertTrue(tg.doInactionProcess());// force
		
		// Game completed because two players auto-folded
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		assertTrue(pB.getState().isDealer());
		assertTrue(pC.getState().getNextAutoInaction() > System.currentTimeMillis()+1000);
	}
	// ------------------------- TOURNAMENT GAMES --------------------------------- //
	@Test
	public void testStartTournamentRandomDealer() {
		initTournament();
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		
		tg.startNextRound(true);
		assertNotNull(tg.blindIncreaseTimer);
		assertTrue( tg.getBlindIncreaseAt() > System.currentTimeMillis() );
		
	}
	
	//TODO This test isn't 100% as it fails randomly based on the random dealer
	@Test
	public void testSatOutPlayerPostsBlindInTourney() {
		initTournament();
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		
		pA.getState().toggleSittingOut(tg.getGameState(), true);
		
		tg.startNextRound(false);
		assertTrue( tg.getBlindIncreaseAt() > System.currentTimeMillis() );
		
		assertTrue(pA.getState().isSittingOut());
		
		Player small = null;
		Player big = null;
		
		// Dealer is randomised, so work with that
		final String dealer = tg.getPlayers().getDealer().getPlayerHandle();
		switch(dealer) {
		case "A":
			small = pB; big = pC;
			assertTrue( pB.getState().isActionOnMe() );
			break;
		case "B":
			small = pC; big = pA;
			assertTrue( pB.getState().isActionOnMe() );
			break;
		case "C":
			small = pA; big = pB;
			assertTrue( pC.getState().isActionOnMe() );
			break;
		}
		assertFalse(pA.getState().isActionOnMe());// A sat-out, action never on them
		assertEquals(999.9, small.getCurrentStack().getStack(), 0.001);
		assertEquals(999.8, big.getCurrentStack().getStack(), 0.001);

		Mockito.verify(msgUtils, Mockito.never()).sendPrivateMessage(Mockito.anyString(), Mockito.any(AnteDueMessage.class));
	}
	
	@Test
	public void testRankingKnockouts() {
		initTournament();
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		final Player pD = TestUtils.makePlayer("D", 1);
		
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		tg.addPlayer(pD);
		
		// when
		tg.rankTourneyLoosers();
		
		// All players still in, so 0 with ranks
		assertEquals(0, tg.getPlayers().getRankedPlayers().size());
		
		// knockout C
		pC.getCurrentStack().setStack(0);
		tg.rankTourneyLoosers();
		assertEquals(4, pC.getCurrentStack().getGameStats().getRank());
		assertTrue(pC.getState().isCashedOut());
		
		// now knockout 2 players together
		pB.getCurrentStack().addToTable(TexasGameState.FLOP, 10);
		pD.getCurrentStack().addToTable(TexasGameState.FLOP, 7);
		pD.getCurrentStack().setStack(0);
		pB.getCurrentStack().setStack(0);
		
		tg.rankTourneyLoosers();
		
		assertTrue(pB.getState().isCashedOut());
		assertTrue(pD.getState().isCashedOut());
		
		assertEquals(2, pB.getCurrentStack().getGameStats().getRank());
		assertEquals(3, pD.getCurrentStack().getGameStats().getRank());
		assertEquals(3, tg.getPlayers().getRankedPlayers().size());
	}
	
	@Test
	public void testRankTourneyWinners() {
		
		initTournament();
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		final Player pD = TestUtils.makePlayer("D", 1);
		final Player pE = TestUtils.makePlayer("E", 7);
		
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		tg.addPlayer(pD);
		tg.addPlayer(pE);
		
		// no-one out, so not set
		assertFalse(tg.rankTourneyWinners());
		
		// knockout C
		pC.getCurrentStack().setStack(0);
		tg.rankTourneyLoosers();
		
		assertFalse(tg.rankTourneyWinners());
		
		// knockout E
		pE.getCurrentStack().setStack(0);
		tg.rankTourneyLoosers();
		
		// Top 3 stacks
		pA.getCurrentStack().setStack(10);
		pB.getCurrentStack().setStack(20);
		pD.getCurrentStack().setStack(50);
		
		// when
		assertTrue(tg.rankTourneyWinners());
		
		assertEquals(5, pC.getCurrentStack().getGameStats().getRank());
		assertEquals(4, pE.getCurrentStack().getGameStats().getRank());
		assertEquals(3, pA.getCurrentStack().getGameStats().getRank());
		assertEquals(2, pB.getCurrentStack().getGameStats().getRank());
		assertEquals(1, pD.getCurrentStack().getGameStats().getRank());
		
		// All players now cashed out
		assertTrue(pC.getState().isCashedOut());
		assertTrue(pE.getState().isCashedOut());
		assertTrue(pA.getState().isCashedOut());
		assertTrue(pB.getState().isCashedOut());
		assertTrue(pD.getState().isCashedOut());
		
		// Test the winnings split (60,30,10) of 5 x 10 buy-in = £100
		assertEquals(0, pC.getCurrentStack().getWallet(), 0.01);
		assertEquals(0, pE.getCurrentStack().getWallet(), 0.01);
		assertEquals(5, pA.getCurrentStack().getWallet(), 0.01);
		assertEquals(15, pB.getCurrentStack().getWallet(), 0.01);
		assertEquals(30, pD.getCurrentStack().getWallet(), 0.01);
		
		// Check the game-end-stats match
		CompleteGameMessage cgm = new CompleteGameMessage(tg.getPlayerStats());
		assertEquals(5, cgm.getStats().get("A").getTransfer() ,0.01);
		assertEquals(15, cgm.getStats().get("B").getTransfer() ,0.01);
		assertEquals(30, cgm.getStats().get("D").getTransfer() ,0.01);
		
		// ranks
		assertEquals(5, cgm.getStats().get("C").getRank() ,0.01);
		assertEquals(3, cgm.getStats().get("A").getRank() ,0.01);
		assertEquals(2, cgm.getStats().get("B").getRank() ,0.01);
		assertEquals(1, cgm.getStats().get("D").getRank() ,0.01);
		
		// Done through calculateAndNotifyWinners()
		//Mockito.verify(msgUtils).sendBroadcastToTable(Mockito.anyString(), Mockito.any(CompleteGameMessage.class), Mockito.anyLong());
		//assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
	}
	// ------------------------- CASH GAMES --------------------------------- //
	
	
	//	TODO Add checking of the correct winners being notified
	@Test
	public void testExactCallBetMatchesPreviousBet() {
		
		float required = 2f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.CALL);
		pa.setBetValue(1f);
		
		// Current action
		tg.setRequiredBet(required);
		
		final Player mkPlayer = setupForBets(1f, 10f, 10f);
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		assertTrue(bet);
		assertEquals(required, pa.getBetValue(), 0.001); // Total now on table
		assertEquals(PlayerActionType.CALL, pa.getAction());
		
	}
	
	@Test
	public void testNonTurnActions() {
		//tg.doNonTurnActions(player, action);
		// TODO test non-turn actions
	}
	
	@Test
	public void testPlayerSitOutOutNextRoundWorks() {
		settings.setHostEmail("A");
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);

		// C sits out this round
		assertTrue(TestUtils.doSitOut(pC, tg.getGameState(), true));
		assertTrue(pC.getState().isSittingOut());
		
		tg.startNextRound(false);
		assertEquals("A", tg.getPlayers().getDealer().getPlayerId());
		
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("A", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.8f);
		assertValidAction("B", PlayerActionType.CALL, 0.2f, 9.8f);
		assertValidAction("A", PlayerActionType.CHECK, 0f, 9.8f);
		
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// C sits in next round
		assertTrue(TestUtils.doSitOut(pC, tg.getGameState(), false));
		assertTrue(pC.getState().isSittingOut());
		assertFalse(pC.getState().isSONR());
		
		checkFlopToComplete("BA", 9.8f, tg);
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		assertEquals(0.4f, tg.getCurrentPot(), 0.01f);
		
		// Starting a new game resets player states
		tg.startNextRound(false);
		
		assertFalse(pC.getState().isSittingOut());
		assertFalse(pC.getState().isSONR());
		assertEquals("B", tg.getPlayers().getDealer().getPlayerId());
		assertTrue(pB.getState().isDealer());
		
		// Only 2 players in the hand
		assertEquals(TexasGameState.PRE_DEAL, tg.getGameState());
		assertEquals(3, tg.getPlayers().getPlayersInHand().size());
	}
	
	@Test
	public void testPlayerSitOutOnNextRoundWorks() {
		settings.setHostEmail("A");
		
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 2);
		final Player pC = TestUtils.makePlayer("C", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);

		tg.startNextRound(false);
		assertEquals("A", tg.getPlayers().getDealer().getPlayerId());
		
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.8f);
		assertValidAction("A", PlayerActionType.CALL, 0.2f, 9.8f);
		assertValidAction("B", PlayerActionType.CALL, 0.2f, 9.8f);
		assertValidAction("C", PlayerActionType.CHECK, 0f, 9.8f);
		
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// C sits out next round
		TestUtils.doSitOut(pC, tg.getGameState(), true);
		assertFalse(pC.getState().isSittingOut());
		assertTrue(pC.getState().isSONR());
		
		checkFlopToComplete("BCA", 9.8f, tg);
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		assertEquals(0.6f, tg.getCurrentPot(), 0.01f);
		
		// Starting a new game resets player states
		tg.startNextRound(false);
		
		assertTrue(pC.getState().isSittingOut());
		assertFalse(pC.getState().isSONR());
		assertEquals("B", tg.getPlayers().getDealer().getPlayerId());
		assertTrue(pB.getState().isDealer());
		
		// Only 2 players in the hand
		assertEquals(TexasGameState.PRE_DEAL, tg.getGameState());
		assertEquals(2, tg.getPlayers().getPlayersInHand().size());
	}

	@Test
	public void testAutoCompleteGetsCorrectWinner() {
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 4);
		final Player pC = TestUtils.makePlayer("C", 2);
		final Player pD = TestUtils.makePlayer("D", 3);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		tg.addPlayer(pC);
		tg.addPlayer(pD);
		
		pB.getState().toggleSittingOut(TexasGameState.COMPLETE, true);
		pD.getState().toggleSittingOut(TexasGameState.COMPLETE, true);
		
		// A and C in game
		assertFalse(pA.getState().isSittingOut());
		assertTrue(pB.getState().isSittingOut());
		assertFalse(pC.getState().isSittingOut());
		assertTrue(pD.getState().isSittingOut());

		tg.startNextRound(true);
		
		assertTrue(pA.getState().isDealer());
		assertEquals(Blinds.BIG, pA.getState().getBlindsDue());
		assertEquals(Blinds.SMALL, pC.getState().getBlindsDue());
		
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("A", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.80f);
		
		assertEquals(TexasGameState.POST_DEAL, tg.getGameState());
		
		assertValidAction("C", PlayerActionType.CALL, tg.getSettings().getBigBlind(), 9.80f);
		assertValidAction("A", PlayerActionType.CHECK, 0f, 9.80f);
		
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		assertValidAction("C", PlayerActionType.BET, 0.4f, 9.40f);
		assertValidAction("A", PlayerActionType.FOLD, 0f, 9.80f);
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
		assertEquals(9.8f, pA.getCurrentStack().getStack(), 0.01);
		assertEquals(10.2f, pC.getCurrentStack().getStack(), 0.01);// C takes pot
		
	}
	
	@Test
	public void testRevealClearsPlayerTableValue() {
		
		float required = 2f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.REVEAL);
		pa.setBetValue(required);//ignored
		
		// Current action
		tg.setRequiredBet(required);
		
		final Player mkPlayer = setupForBets(2f, 10f, 10f);
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		assertTrue(bet);
		assertEquals(0, pa.getBetValue(), 0.001); // Forfeit
		assertEquals(PlayerActionType.REVEAL, pa.getAction());
		assertEquals(2f, tg.getCurrentPot(), 0.001);
		
	}

	@Test
	public void testFoldClearsPlayerTableValue() {
		
		float required = 2f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.FOLD);
		pa.setBetValue(required);//ignored
		
		// Current action
		tg.setRequiredBet(required);
		
		final Player mkPlayer = setupForBets(2f, 10f, 10f);
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		assertTrue(bet);
		assertEquals(0, pa.getBetValue(), 0.001); // Forfeit
		assertEquals(PlayerActionType.FOLD, pa.getAction());
		assertEquals(2f, tg.getCurrentPot(), 0.001);
		
	}
	
	@Test
	public void testOverCallBetMatchesPreviousBet() {
		
		float required = 2f;
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.CALL);
		pa.setBetValue(5f);
		
		// Current action
		tg.setRequiredBet(required);
		
		final Player mkPlayer = setupForBets(1f, 10f, 10f);
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		assertTrue(bet);
		assertEquals(required, pa.getBetValue(), 0.001); // Total now on table
		assertEquals(PlayerActionType.CALL, pa.getAction());
		
	}
	
	@Test
	public void testUnderCallBetMatchesPreviousBet() {
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.CALL);
		pa.setBetValue(0.5f);
		
		// Current action
		tg.setRequiredBet(2f);
		
		final Player mkPlayer = setupForBets(1f, 10f, 10f);
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// Then
		assertTrue(bet);
		assertEquals(2f, pa.getBetValue(), 0.001); // Total now on table
		assertEquals(PlayerActionType.CALL, pa.getAction());
		
	}
	
	@Test
	public void testCallAllInCantMatchStillCalls() {
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.CALL);
		pa.setBetValue(2f);
		
		// Current action
		tg.setRequiredBet(10.5f);
		
		// Only 5 in stack, nothing on table
		final Player mkPlayer = setupForBets(0f, 5f, 15f);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertTrue(bet);
		assertEquals(5f, pa.getBetValue(), 0.001); // 10.5 required, but we bet everything
		assertEquals(PlayerActionType.ALL_IN, pa.getAction());
		
	}
	
	@Test
	public void testOverBetMaximumPossibleGivesRefund() {
		
		float required = 9.25f;
		float maximum = 9.25f;
		float betValue = 11.5f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.BET);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet( required );
		
		// Loads available, 2 already on the table
		final Player mkPlayer = setupForBets(2f, 35f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertTrue(bet);
		assertEquals(maximum, pa.getBetValue(), 0.001); // 9.25 is required, and maximum possible
		assertEquals(PlayerActionType.BET, pa.getAction());
	}
	
	@Test
	public void testOverBetRequiredUpToMaximumGivesRefund() {
		
		float required = 8.25f;
		float maximum = 11.25f;
		float betValue = 13.8f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.BET);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet(required);
		
		// Loads available, 2 already on the table
		final Player mkPlayer = setupForBets(2f, 35f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertTrue(bet);
		assertEquals(maximum, pa.getBetValue(), 0.001); // We can bet more than required, but up to the maximum
		assertEquals(PlayerActionType.BET, pa.getAction());
	}
	
	@Test
	public void testUnderBetIsUnsuccessful() {
		
		float required = 8.25f;
		float maximum = 11.25f;
		float betValue = 5f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.BET);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet(required);
		
		// Loads available, 2 already on the table but only betting 5 (7 total)
		final Player mkPlayer = setupForBets(2f, 35f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertFalse(bet);
		assertEquals(betValue, pa.getBetValue(), 0.001); // We can bet more than required, but up to the maximum
		assertEquals(PlayerActionType.BET, pa.getAction());
	}
	
	@Test
	public void testBetAllInIsActuallyAllIn() {
		
		float required = 5f;
		float maximum = 15f;
		float betValue = 10f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.BET);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet(required);
		
		// Loads available, 2 already on the table and going all-in
		final Player mkPlayer = setupForBets(2.5f, 10f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertTrue(bet);
		assertEquals(betValue, pa.getBetValue(), 0.001); // Should equal our whole stack
		assertEquals(PlayerActionType.ALL_IN, pa.getAction());
	}
	
	@Test
	public void testAllInIsActuallyAllIn() {
		
		float required = 5f;
		float maximum = 15f;
		float betValue = 2f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.ALL_IN);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet(required);
		
		// Loads available, 2 already on the table and going all-in
		final Player mkPlayer = setupForBets(2.8f, 10f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertTrue(bet);
		assertEquals(10f, pa.getBetValue(), 0.001); // Should equal our whole stack
		assertEquals(PlayerActionType.ALL_IN, pa.getAction());
	}
	
	@Test
	public void testCanCallAZeroAmount() {
		
		float required = 0f;
		float maximum = 11.25f;
		float betValue = 5f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.CALL);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet(required);
		
		// Loads available, 0 already on the table 
		final Player mkPlayer = setupForBets(0f, 35f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertTrue(bet);
		assertEquals(0, pa.getBetValue(), 0.001); // Bet set to zero
		assertEquals(PlayerActionType.CALL, pa.getAction());
	}
	
	@Test
	public void testCantBetAZeroAmount() {
		
		float required = 5f;
		float maximum = 11.25f;
		float betValue = 0f;
		
		// Player action
		PlayerAction pa = new PlayerActionMessage("sessionId");
		pa.setAction(PlayerActionType.BET);
		pa.setBetValue(betValue);
		
		// Current action
		tg.setRequiredBet(required);
		
		// Loads available, 0 already on the table 
		final Player mkPlayer = setupForBets(0f, 35f, maximum);
		
		// When
		final boolean bet = tg.addPlayerBet(pa, mkPlayer);
		
		// then
		assertFalse(bet);
		assertEquals(0, pa.getBetValue(), 0.001); // Bet set to zero
		assertEquals(PlayerActionType.BET, pa.getAction());
	}
	
	/**
	 * 
	 * @param onTable What's already on the table
	 * @param fStack The total in their stack
	 * @param maxBet The maximum of all virtual players
	 * @return The player constructed
	 */
	private Player setupForBets(final float onTable, float fStack, final float maxBet) {
		Player p = Mockito.mock(Player.class);
		PlayerStack stack = new PlayerStack();
		stack.initialise(10f, false);
		stack.setStack(fStack);
		stack.addToTable(TexasGameState.FLOP, onTable);
		
		Mockito.when(p.getCurrentStack()).thenReturn(stack);
		Mockito.when(p.isStillInHand()).thenReturn(true);
		Mockito.when(p.getState()).thenReturn(Mockito.mock(PlayerState.class));
		
		// Players
		Players mkPlayers = Mockito.mock(Players.class);
		Field f = ReflectionUtils.findField(TexasHoldemGame.class, "players");
		f.setAccessible(true);
		ReflectionUtils.setField(f, tg, mkPlayers);
		
		// Game state
		tg.setGameState(TexasGameState.FLOP);
		
		Mockito.when(mkPlayers.getMaxBetPossible(p)).thenReturn(maxBet);
		
		
		return p;
	}
	
	@Test
	public void testNewGameStartSetsPlayersAndStates() {
		
		addPlayers(5, tg);
		tg.startNextRound(true);
		final Player a = tg.getPlayers().getPlayerById("A");
		final Player b = tg.getPlayers().getPlayerById("B");
		final Player c = tg.getPlayers().getPlayerById("C");
		
		assertTrue(a.getState().isDealer());
		assertEquals(Blinds.SMALL, b.getState().getBlindsDue());
		assertTrue(b.getState().isActionOnMe());
		assertEquals(Blinds.BIG, c.getState().getBlindsDue());
		assertFalse(c.getState().isActionOnMe());
		
		assertEquals(0, a.getCards().size());
		assertNull(a.getRankedHand());
		assertEquals(0, b.getCards().size());
		assertNull(b.getRankedHand());
		assertEquals(0, c.getCards().size());
		assertNull(c.getRankedHand());
	}
	
	@Test
	public void testPostBlindsSetsFlags() {
		addPlayers(5, tg);
		tg.startNextRound(true);
		final Player a = tg.getPlayers().getPlayerById("A");
		final Player b = tg.getPlayers().getPlayerById("B");
		final Player c = tg.getPlayers().getPlayerById("C");
		final Player d = tg.getPlayers().getPlayerById("D");
		
		GameUpdateMessage gum = tg.doGameUpdateAction(mockAction("B", PlayerActionType.POST_BLIND, 0));
		
		assertEquals(MessageTypes.GAME_UPDATE, gum.getMessageType());
		assertNotNull(gum.getCurrentGame());
		assertEquals(TexasGameState.PRE_DEAL, gum.getCurrentGame().getGameState());
		assertEquals(0, gum.getCurrentGame().getCardsOnTable().size());
		
		// B no longer requires blinds, but action is on C for posting
		assertEquals(Blinds.NONE, b.getState().getBlindsDue());
		assertFalse(b.getState().isActionOnMe());
		assertEquals(Blinds.BIG, c.getState().getBlindsDue());
		assertTrue(c.getState().isActionOnMe());
		
		// C posts blind
		gum = tg.doGameUpdateAction(mockAction("C", PlayerActionType.POST_BLIND, 0));
		assertNotNull(gum.getCurrentGame());
		
		assertEquals(Blinds.NONE, b.getState().getBlindsDue());
		assertFalse(b.getState().isActionOnMe());
		assertEquals(Blinds.NONE, c.getState().getBlindsDue());
		assertFalse(c.getState().isActionOnMe());
		
		// action now on D and gamestate updated
		assertTrue(d.getState().isActionOnMe());
		
		// No cards dealt, but all players now have cards
		assertEquals(TexasGameState.POST_DEAL, gum.getCurrentGame().getGameState());
		assertEquals(0, gum.getCurrentGame().getCardsOnTable().size());
		
		assertEquals(2, a.getCards().size());
		assertEquals(2, b.getCards().size());
		assertEquals(2, c.getCards().size());
		assertEquals(2, d.getCards().size());
		assertEquals(2, tg.getPlayers().getPlayerById("E").getCards().size());
	}
	
	@Test
	public void testBlindsMostBePostedBeforeContinueGame() {
		addPlayers(5, tg);
		tg.startNextRound(true);
		final float betVal = tg.getSettings().getBigBlind();
		// B tries to check
		assertInvalidAction("B", PlayerActionType.CHECK, 0, true);
		assertInvalidAction("B", PlayerActionType.CALL, 0, true);
		assertInvalidAction("B", PlayerActionType.FOLD, 0, true);
		assertInvalidAction("B", PlayerActionType.RAISE, 0.50f, true);
		assertInvalidAction("B", PlayerActionType.BET, 0.5f, true);
		
		// Post the blind to move action to next player (C)
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		
		// Test invalid actions
		assertInvalidAction("C", PlayerActionType.CHECK, 0, true);
		assertInvalidAction("C", PlayerActionType.CALL, 0, true);
		assertInvalidAction("C", PlayerActionType.FOLD, 0, true);
		assertInvalidAction("C", PlayerActionType.RAISE, 0.50f, true);
		assertInvalidAction("C", PlayerActionType.BET, 0.5f, true);

		// C now posts blind
		assertValidAction("C", PlayerActionType.POST_BLIND, betVal, 10-betVal);
		assertEquals(TexasGameState.POST_DEAL, tg.getGameState());
		
		// All remaining players call, so the big-blind should now be able to check
		assertValidAction("D", PlayerActionType.CALL, betVal, 10-betVal);
		assertValidAction("E", PlayerActionType.CALL, betVal, 10-betVal);
		assertValidAction("A", PlayerActionType.CALL, betVal, 10-betVal);
		assertValidAction("B", PlayerActionType.CALL, betVal, 10-betVal);
		
		// zero on table as bets collected
		assertValidAction("C", PlayerActionType.CHECK, 0f, 10-betVal);
		assertEquals(TexasGameState.FLOP, tg.getGameState());
	}
	
	@Test
	public void testPlayerJoiningMidGameDoesntEffectStateOnCalcWinners() {

		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		assertEquals(0, pA.getSeatingPos());
		assertEquals(4, pB.getSeatingPos());
		assertFalse(pA.getState().isSittingOut());
		assertFalse(pB.getState().isSittingOut());
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("A", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 10- tg.getSettings().getBigBlind());
		
		assertEquals(TexasGameState.POST_DEAL, tg.getGameState());
		
		assertValidAction("B", PlayerActionType.CALL, tg.getSettings().getBigBlind(), 9.80f);
		assertValidAction("A", PlayerActionType.CHECK, 0f, 9.80f);
		
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		assertValidAction("B", PlayerActionType.CHECK, 0f, 9.80f);
		assertValidAction("A", PlayerActionType.CHECK, 0f, 9.80f);
		
		assertEquals(TexasGameState.TURN, tg.getGameState());
		
		assertValidAction("B", PlayerActionType.BET, 0.2f, 9.60f);
		assertValidAction("A", PlayerActionType.CALL, 0f, 9.60f);
		
		assertEquals(TexasGameState.RIVER, tg.getGameState());

		// New player joins
		final Player pC = TestUtils.makePlayer("C", 2);
		tg.addPlayer(pC);
		assertTrue(pC.getState().isSittingOut());
		
		assertValidAction("B", PlayerActionType.BET, tg.getSettings().getBigBlind(), 9.40f);
		assertValidAction("A", PlayerActionType.CALL, 0f, 9.40f);
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		Mockito.verify(sevenCardEvaluator).calculatePotsAndWinners(Mockito.any(), Mockito.any());
		
		// Either C didn't win
		assertEquals(10f, pC.getCurrentStack().getStack(), 0.01);
		
	}

	@Test
	public void testPlayerJoiningPreDealDoesntGetCards() {

		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		assertEquals(0, pA.getSeatingPos());
		assertEquals(4, pB.getSeatingPos());
		assertFalse(pA.getState().isSittingOut());
		assertFalse(pB.getState().isSittingOut());
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		
		// New player joins
		final Player pC = TestUtils.makePlayer("C", 2);
		tg.addPlayer(pC);
		assertTrue(pC.getState().isSittingOut());
		assertEquals(10f, pC.getCurrentStack().getStack(), 0.01);
		
		assertValidAction("A", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 10- tg.getSettings().getBigBlind());
		
		assertEquals(TexasGameState.POST_DEAL, tg.getGameState());
		
		assertEquals(2, pA.getCards().size());
		assertEquals(2, pB.getCards().size());
		assertEquals(0, pC.getCards().size());
	}
	
	@Test
	public void testPlayerJoiningMidGameDoesntEffectStateOnFold() {

		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 4);
		tg.addPlayer(pA);
		tg.addPlayer(pB);
		assertEquals(0, pA.getSeatingPos());
		assertEquals(4, pB.getSeatingPos());
		assertFalse(pA.getState().isSittingOut());
		assertFalse(pB.getState().isSittingOut());
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("A", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 10- tg.getSettings().getBigBlind());
		
		assertEquals(TexasGameState.POST_DEAL, tg.getGameState());
		
		assertValidAction("B", PlayerActionType.CALL, tg.getSettings().getBigBlind(), 9.80f);
		assertValidAction("A", PlayerActionType.CHECK, 0f, 9.80f);
		
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		assertValidAction("B", PlayerActionType.CHECK, 0f, 9.80f);
		assertValidAction("A", PlayerActionType.CHECK, 0f, 9.80f);
		
		assertEquals(TexasGameState.TURN, tg.getGameState());
		
		// New player joins
		final Player pC = TestUtils.makePlayer("C", 2);
		tg.addPlayer(pC);
		assertTrue(pC.getState().isSittingOut());
		assertEquals(10f, pC.getCurrentStack().getStack(), 0.01);
		
		assertValidAction("B", PlayerActionType.BET, tg.getSettings().getBigBlind(), 9.60f);
		assertValidAction("A", PlayerActionType.CALL, 0f, 9.60f);// triggers auto-complete
		
		assertEquals(TexasGameState.RIVER, tg.getGameState());
		assertEquals(0, pC.getCards().size());
		
		assertValidAction("B", PlayerActionType.BET, tg.getSettings().getBigBlind(), 9.40f);
		assertValidAction("A", PlayerActionType.FOLD, 0f, 9.60f);// triggers auto-complete
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
		// Either C didn't win
		assertEquals(10f, pC.getCurrentStack().getStack(), 0.01);
		
	}
	
	@Test
	public void testDealContinuesWhenOnePlayerAllInAndMatchedBet() {
		addPlayers(5, tg);
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 10- tg.getSettings().getBigBlind());
		
		// Set D as all-in
		Player d = tg.getPlayers().getPlayerById("D");
		d.getCurrentStack().setStack(0.5f);
		assertValidAction("D", PlayerActionType.BET, 0.50f, 0f);
		
		// Rest of players call
		assertValidAction("E", PlayerActionType.CALL, 0.50f, 9.50f);
		assertValidAction("A", PlayerActionType.CALL, 0.50f, 9.50f);
		assertValidAction("B", PlayerActionType.CALL, 0.50f, 9.50f);
		assertValidAction("C", PlayerActionType.CALL, 0, 9.50f);// nothing on table as deal worked
		
		// check the game state
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		assertEquals(5 * 0.5f, tg.getCurrentPot(), 0.001); 
	}
	
	@Test
	public void testGameCompletesIfNoBetsPossible() {
		addPlayers(5, tg);
		
		// Set all players with 0.20
		float bb = tg.getSettings().getBigBlind();
		tg.getPlayers().forEach(p->p.getCurrentStack().setStack(bb));
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, settings.getAnte(), 0.10f);
		assertValidAction("C", PlayerActionType.POST_BLIND, bb, 0f);
		
		// Set D as all-in
		assertValidAction("D", PlayerActionType.BET, bb, 0f);
		assertValidAction("E", PlayerActionType.CALL,bb, 0f);
		assertValidAction("A", PlayerActionType.CALL, bb, 0f);
		assertValidAction("B", PlayerActionType.CALL, 0f, 0f); // nothing on table as deal worked
		
		
		// check the game state
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		assertEquals(1f, tg.getCurrentPot(), 0.001);// pot is zero as someone won 
		
		Mockito.verify(sevenCardEvaluator).calculatePotsAndWinners(Mockito.any(), Mockito.any());
		
		// Check VPIP
		assertEquals(100, getStats("A").getVpip(), 0.1);// called
		assertEquals(100, getStats("B").getVpip(), 0.1);// called
		assertEquals(0, getStats("C").getVpip(), 0.1);// checked
		assertEquals(100, getStats("D").getVpip(), 0.1);// bet
		assertEquals(100, getStats("E").getVpip(), 0.1);// called
	}
	
	@Test
	public void testLastPlayerAllInDoesntCompleteGame() {
		addPlayers(3, tg);
		
		float bb = tg.getSettings().getBigBlind();
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, settings.getAnte(), 9.9f);
		assertValidAction("C", PlayerActionType.POST_BLIND, bb, 9.8f);
		assertValidAction("A", PlayerActionType.CALL, bb, 9.8f);
		assertValidAction("B", PlayerActionType.CALL, bb, 9.8f);
		assertValidAction("C", PlayerActionType.CHECK, 0, 9.8f);
		
		// Check state
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		assertPlayRound(PlayerActionType.CHECK, 9.8f, TexasGameState.TURN);
		assertPlayRound(PlayerActionType.CHECK, 9.8f, TexasGameState.RIVER);
		
		// In the last round, first players check, last one goes all-in
		assertValidAction("B", PlayerActionType.CHECK, 0, 9.8f);
		assertValidAction("C", PlayerActionType.CHECK, 0, 9.8f);
		assertValidAction("A", PlayerActionType.ALL_IN, 9.8f, 0);
		
		// Should still be on River and back to first player to match
		assertEquals(TexasGameState.RIVER, tg.getGameState());
		assertTrue(tg.getPlayers().getPlayerById("B").getState().isActionOnMe());
	}
	
	/** for 3 players perform the round and check state
	 * 
	 * @param action
	 * @param expectStack
	 * @param postRound
	 */
	private void assertPlayRound(PlayerActionType action, float expectStack, TexasGameState postRound) {
		assertValidAction("B", action, 0, expectStack);
		assertValidAction("C", action, 0, expectStack);
		assertValidAction("A", action, 0, expectStack);
		assertEquals(postRound, tg.getGameState());
	}
	
	@Test
	public void testMinimumPlayerStartGameAfterRebuy() {
		testGameCompletesIfOnlyOneBetsPossible();
		
		float buyin = settings.getBuyInAmount();
		
		Player a = tg.getPlayers().getPlayerById("A");
		Player b = tg.getPlayers().getPlayerById("B");
		
		// Check players are auto-sat-out and then re-buy
		assertTrue(a.getState().isSittingOut());
		setWallet(buyin, a.getCurrentStack());
		
		assertTrue(b.getState().isSittingOut());
		setWallet(buyin, b.getCurrentStack());
		
		// Player D has money, no-one else does. A and B re-buy
		playerRebuy(a, buyin);
		playerRebuy(b, buyin);

		// Game won't start as we still have two players with 
		// zero stacks and we're within 30 seconds
		try {
			tg.startNextRound(true);
		} catch (GameStartException e) {
			assertNotNull(e);
		}
		
		// C and E are still out... so set the last activity time so we can proceed
		Field f = ReflectionUtils.findField(AbstractCardGame.class, "lastRoundCompletedTime");
		f.setAccessible(true);
		ReflectionUtils.setField(f, tg, System.currentTimeMillis() - (31 * 1000));
		
		// Game will restart as buy-in toggles them in and we worked around the timer
		tg.startNextRound(true);

		assertTrue(tg.getPlayers().getPlayerById("D").getState().isDealer());
		
		assertFalse(a.getState().isSittingOut());
		assertFalse(b.getState().isSittingOut());
	}
	
	private void playerRebuy(final Player player, final float buyin) {
		player.doPlayerAction(mockAction(player.getPlayerId(), PlayerActionType.RE_BUY, buyin), tg.getGameState(), tg.getSettings(), buyin);
		assertEquals(10f, player.getCurrentStack().getStack(), 0.01);
		//Mockito.verify(msgUtils, Mockito.atLeast(1)).sendPrivateMessage(Mockito.eq(player.getPlayerId()), Mockito.any());
	}
	
	private void setWallet(float value, PlayerStack stack) {
		Field f = ReflectionUtils.findField(PlayerStack.class, "currentWallet");
		f.setAccessible(true);
		ReflectionUtils.setField(f, stack, value);
	}
	
	@Test
	public void testTwoGamesAtOnce() {
		TexasHoldemGame game1 = new TexasHoldemGame(settings, msgUtils);
		game1.setEvaluator(sevenCardEvaluator);
		Mockito.
		when(sevenCardEvaluator.calculatePotsAndWinners(Mockito.any(), Mockito.any())).thenReturn(gamePot);
		
		TexasHoldemGame game2 = new TexasHoldemGame(settings, msgUtils);
		game2.setEvaluator(sevenCardEvaluator);
		Mockito.
		when(sevenCardEvaluator.calculatePotsAndWinners(Mockito.any(), Mockito.any())).thenReturn(gamePot);
		addPlayers(5, game2);
		game2.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, game2.getSettings().getAnte(), 9.90f, game2);
		assertValidAction("C", PlayerActionType.POST_BLIND, game2.getSettings().getBigBlind(), 9.8f, game2);
		assertValidAction("D", PlayerActionType.BET, 2f, 8f, game2); // D raises
		assertEquals(TexasGameState.POST_DEAL, game2.getGameState());
		
		// Now go through to flop on game 1
		checkHandsThroughToCompletion(game1);

		assertValidAction("E", PlayerActionType.CALL, 2f, 8f, game2);
		assertValidAction("A", PlayerActionType.FOLD, 0f, 10f, game2);
		assertEquals(TexasGameState.POST_DEAL, game2.getGameState());
		assertEquals(TexasGameState.COMPLETE, game1.getGameState());
		
		// Start next round on 1
		game1.startNextRound(true);
		assertEquals(TexasGameState.POST_DEAL, game2.getGameState());
		assertEquals(TexasGameState.PRE_DEAL, game1.getGameState());
		
		// Check player bets
		final Player a1 = game1.getPlayers().getPlayerById("A");
		final Player a2 = game2.getPlayers().getPlayerById("A");

		final Player d1 = game1.getPlayers().getPlayerById("D");
		final Player d2 = game2.getPlayers().getPlayerById("D");
		
		assertFalse(a1.getState().isFolded());
		assertTrue(a2.getState().isFolded());
		
		assertEquals(1.8f, d1.getCurrentStack().getStack(), 0.001);
		assertEquals(8f, d2.getCurrentStack().getStack(), 0.001);
		assertEquals(0, d1.getCards().size());
		assertEquals(2, d2.getCards().size());
		
	}
		
	@Test
	public void testFoldAction() {
		final Player pA = TestUtils.makePlayer("A", 0);
		tg.addPlayer(pA);
		final PlayerAction action = mockAction("A", PlayerActionType.FOLD, 0);
		action.setSuccessful(false);
		
		tg.setGameState(TexasGameState.FLOP);
		// Fold in round, action on player
		pA.getState().setActionOn(true);
		assertTrue(tg.foldOrRevealPlayer(pA, action));
		
		assertEquals(PlayerActionType.FOLD, pA.getState().getLastAction());
		assertTrue(action.isSuccessful());
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Fold in round, action not on player
		pA.getState().setActionOn(false);
		pA.getState().setLastAction(PlayerActionType.BET);
		action.setSuccessful(false);
		assertFalse(tg.foldOrRevealPlayer(pA, action));
		
		assertEquals(PlayerActionType.BET, pA.getState().getLastAction());
		assertFalse(action.isSuccessful());
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		
		// Test can't fold when blinds due
		pA.getState().setActionOn(true);
		pA.getState().setBlindsDue(Blinds.BIG);
		action.setSuccessful(false);
		assertFalse(tg.foldOrRevealPlayer(pA, action));
		
		assertEquals(PlayerActionType.BET, pA.getState().getLastAction());
		assertFalse(action.isSuccessful());
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Fold end of round
		tg.setGameState(TexasGameState.COMPLETE);
		pA.getState().setBlindsDue(Blinds.NONE);
		
		// Fold, action on player
		pA.getState().setActionOn(true);
		action.setSuccessful(false);
		assertFalse( tg.foldOrRevealPlayer(pA, action) );// Action doesn't continue
		
		assertEquals(PlayerActionType.FOLD, pA.getState().getLastAction());
		assertTrue(action.isSuccessful());
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
		// Fold end of round, action not on player
		pA.getState().setActionOn(false);
		pA.getState().setLastAction(PlayerActionType.BET);
		action.setSuccessful(false);
		assertFalse( tg.foldOrRevealPlayer(pA, action) ); // Action doesn't continue
		
		assertEquals(PlayerActionType.FOLD, pA.getState().getLastAction());
		assertTrue(action.isSuccessful());
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
	}
	
	@Test
	public void testRevealAction() {
		final Player pA = TestUtils.makePlayer("A", 0);
		tg.addPlayer(pA);
		final PlayerAction action = mockAction("A", PlayerActionType.REVEAL, 0);
		action.setSuccessful(false);
		
		tg.setGameState(TexasGameState.FLOP);
		// Reveal in round, action on player
		pA.getState().setActionOn(true);
		assertTrue( tg.foldOrRevealPlayer(pA, action) );
		
		assertEquals(PlayerActionType.REVEAL, pA.getState().getLastAction());
		assertTrue(action.isSuccessful());
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Reveal in round, not on player
		pA.getState().setActionOn(false);
		pA.getState().setLastAction(PlayerActionType.BET);
		action.setSuccessful(false);
		assertFalse(tg.foldOrRevealPlayer(pA, action));
		
		assertEquals(PlayerActionType.BET, pA.getState().getLastAction());
		assertFalse(action.isSuccessful());
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Test can't Reveal when blinds due
		pA.getState().setActionOn(true);
		pA.getState().setBlindsDue(Blinds.BIG);
		action.setSuccessful(false);
		assertFalse(tg.foldOrRevealPlayer(pA, action));
		
		assertEquals(PlayerActionType.BET, pA.getState().getLastAction());
		assertFalse(action.isSuccessful());
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Reveal end of round
		tg.setGameState(TexasGameState.COMPLETE);
		pA.getState().setBlindsDue(Blinds.NONE);
		
		// Reveal end of round, action on player
		pA.getState().setActionOn(true);
		action.setSuccessful(false);
		assertFalse(tg.foldOrRevealPlayer(pA, action)); // Action doesn't continue
		
		assertEquals(PlayerActionType.REVEAL, pA.getState().getLastAction());
		assertTrue(action.isSuccessful());
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
		// Reveal end of round, not on player
		pA.getState().setActionOn(false);
		pA.getState().setLastAction(PlayerActionType.BET);
		action.setSuccessful(false);
		assertFalse( tg.foldOrRevealPlayer(pA, action) );// Action doesn't continue
		
		assertEquals(PlayerActionType.REVEAL, pA.getState().getLastAction());
		assertTrue(action.isSuccessful());
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		
		Mockito.verify(msgUtils, Mockito.times(3)).sendBroadcastToTable(Mockito.eq(settings.getGameId()), Mockito.any(ShowCardsMessage.class));
		
	}
	
	@Test
	public void testCantFoldOrRevealTwice() {
		final Player pA = TestUtils.makePlayer("A", 0);
		tg.addPlayer(pA);
		final PlayerAction action = mockAction("A", PlayerActionType.REVEAL, 0);
		action.setSuccessful(false);
		pA.getState().setLastAction(PlayerActionType.REVEAL);
		
		assertFalse( tg.foldOrRevealPlayer(pA, action) );// Action doesn't continue
		
		pA.getState().setLastAction(PlayerActionType.FOLD);
		action.setAction(PlayerActionType.FOLD);
		assertFalse( tg.foldOrRevealPlayer(pA, action) );// Action doesn't continue
		
		Mockito.verifyNoInteractions(msgUtils);
	}
	
	@Test
	public void testGameCompletesIfOnlyOneBetsPossible() {
		addPlayers(5, tg);
		float betVal = settings.getBigBlind();
		
		// Set all players with 0.25
		tg.getPlayers().forEach(p->p.getCurrentStack().setStack(betVal));
		
		// D still has cash to bet
		PlayerStack dStack = tg.getPlayers().getPlayerById("D").getCurrentStack();
		dStack.setStack(5f);
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 0.10f);
		assertValidAction("C", PlayerActionType.POST_BLIND, betVal, 0f);
		
		// Set D as all-in
		assertValidAction("D", PlayerActionType.RAISE, betVal, 5-betVal);
		assertValidAction("E", PlayerActionType.CALL, betVal, 0f);
		assertValidAction("A", PlayerActionType.CALL, betVal, 0f);
		assertValidAction("B", PlayerActionType.CALL, 0f, 0f); // nothing on table as deal worked
		
		
		// check the game state
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
		assertEquals(1f, tg.getCurrentPot(), 0.001);
		
		Mockito.verify(sevenCardEvaluator).calculatePotsAndWinners(Mockito.any(), Mockito.any());
		
		// Check D got a refund
		assertEquals(5 - betVal, dStack.getStack(), 0.001f);
		
		// Check VPIP
		assertEquals(100, getStats("A").getVpip(), 0.1);// called
		assertEquals(100, getStats("B").getVpip(), 0.1);// big-blind
		assertEquals(0, getStats("C").getVpip(), 0.1);// called
		assertEquals(100, getStats("D").getVpip(), 0.1);// called
		assertEquals(100, getStats("E").getVpip(), 0.1);// called
		
	}
	/////////////////// Round completion tests ///////////
	@Test
	public void testDealContinuesWhenOnePlayerAllInAndUnmatchedBet() {
		addPlayers(5, tg);
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 10-tg.getSettings().getAnte());
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 10-tg.getSettings().getBigBlind());
		
		// Set D as all-in
		Player d = tg.getPlayers().getPlayerById("D");
		d.getCurrentStack().setStack(0.5f);
		assertValidAction("D", PlayerActionType.BET, 0.50f, 0f);
		assertTrue(d.getState().isAllIn());
		
		// E calls
		assertValidAction("E", PlayerActionType.CALL, 0.50f, 9.50f);
		
		// A re-raises
		assertValidAction("A", PlayerActionType.BET, 2f, 8f);
		
		// rest of players call the raise
		assertValidAction("B", PlayerActionType.CALL, 2f, 8f);
		assertValidAction("C", PlayerActionType.CALL, 2f, 8f);
		
		// D is missed as can't match bet
		assertFalse(d.getState().isActionOnMe());
		
		assertTrue(tg.getPlayers().getPlayerById("E").getState().isActionOnMe());
		assertValidAction("E", PlayerActionType.CALL, 0f, 8f);// deal goes through
		
		// All players except (D) equalised
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		assertEquals(4 * 2f + 0.5f, tg.getCurrentPot(), 0.001);
		
		// Only four players in FLOP
		assertEquals(4, tg.getPlayers().getPlayersInHand(true).size());
		
		// D is still all-in
		assertTrue(d.getState().isAllIn());
		
		// only 4 have to check for next deal to complete
		assertValidAction("B", PlayerActionType.CHECK, 0, 8f);
		assertValidAction("C", PlayerActionType.CHECK, 0, 8f);
		assertValidAction("E", PlayerActionType.CHECK, 0, 8f);
		assertValidAction("A", PlayerActionType.CHECK, 0, 8f);
		assertEquals(TexasGameState.TURN, tg.getGameState());
		assertEquals(4 * 2f + 0.5f, tg.getCurrentPot(), 0.001);
	}
	
	@Test
	public void testDealContinuesWhenTwoPlayersAllInAsCantMatchBet() {
		addPlayers(5, tg);
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.8f);
		
		// Set D raises
		assertValidAction("D", PlayerActionType.BET, 2f, 8f);
		
		// E calls it, but only has 0.5
		Player e = tg.getPlayers().getPlayerById("E");
		e.getCurrentStack().setStack(0.5f);
		assertValidAction("E", PlayerActionType.CALL, 0.5f, 0f);
		
		// A calls it, but only has 1.5
		Player a = tg.getPlayers().getPlayerById("A");
		a.getCurrentStack().setStack(1.5f);
		assertValidAction("A", PlayerActionType.CALL, 1.5f, 0f);
		
		// rest of players call the raise
		assertValidAction("B", PlayerActionType.CALL, 2f, 8f);
		assertValidAction("C", PlayerActionType.CALL, 0f, 8f);// deal completes
		
		// All players except equalized except A and E
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		assertEquals(3 * 2f + 0.5f + 1.5f, tg.getCurrentPot(), 0.001);
		
		// Only three players in FLOP
		assertEquals(3, tg.getPlayers().getPlayersInHand(true).size());
		
		// only 3 have to check for next deal to complete
		assertValidAction("B", PlayerActionType.CHECK, 0, 8f);
		assertValidAction("C", PlayerActionType.CHECK, 0, 8f);
		assertValidAction("D", PlayerActionType.CHECK, 0, 8f);
		
		assertEquals(TexasGameState.TURN, tg.getGameState());
		assertEquals(3 * 2f + 0.5f + 1.5f, tg.getCurrentPot(), 0.001);
		
		tg.getPlayers().forEach(p -> p.getCurrentStack().getGameStats().updateStats(false));
		// Check VPIP
		assertEquals(100, getStats("A").getVpip(), 0.1);// called
		assertEquals(100, getStats("B").getVpip(), 0.1);// big-blind - called
		assertEquals(100, getStats("C").getVpip(), 0.1);// called
		assertEquals(100, getStats("D").getVpip(), 0.1);// called
		assertEquals(100, getStats("E").getVpip(), 0.1);// called
		
	}
	
	@Test
	public void testPlayerCanBetWholeStackToGoAllIn() {
		addPlayers(2, tg);
		tg.addPlayerBet(mockAction("A", PlayerActionType.BET, 8f), tg.getPlayers().getPlayerById("A"));
		tg.setRequiredBet(8f);
		
		// Set B's stack lower and bet
		Player B = tg.getPlayers().getPlayerById("B");
		B.getCurrentStack().setStack(5.25f);
		B.getCurrentStack().addToTable(tg.getGameState(), 0.25f);
		
		// B bet's whole stack to go all in
		boolean result = tg.addPlayerBet(mockAction("B", PlayerActionType.BET, 5.25f), B);
		assertTrue(result);
		assertTrue(B.getState().isAllIn());
		assertEquals(0f, B.getCurrentStack().getStack(), 0.001);
		
		assertTrue(tg.hasPotEqualized());
	}
	
	@Test
	public void testPlayerCanBetWholeStackToGoAllInButNotEqualPot() {
		addPlayers(2, tg);
		Player A = tg.getPlayers().getPlayerById("A");
		Player B = tg.getPlayers().getPlayerById("B");
		
		tg.addPlayerBet(mockAction("A", PlayerActionType.BET, 2f), A);
		tg.setRequiredBet(2f);// as this happens through player action
		
		// Set B's stack lower and bet
		B.getCurrentStack().setStack(5.25f);
		B.getCurrentStack().addToTable(tg.getGameState(), 0.25f);
		
		// B bet's whole stack to go all in
		boolean result = tg.addPlayerBet(mockAction("B", PlayerActionType.BET, 5.25f), B);
		
		assertTrue(result);
		assertTrue(B.getState().isAllIn());
		assertEquals(0f, B.getCurrentStack().getStack(), 0.001);
		tg.setRequiredBet(5.25f);// as this happens through player action
		
		assertFalse(tg.hasPotEqualized());// A has still got to match it
		assertFalse(tg.tryCollectBetsAndReset());
		
		// Player A matches
		tg.addPlayerBet(mockAction("A", PlayerActionType.BET, 5.25f), A);

		assertTrue(tg.hasPotEqualized());
		assertTrue(tg.tryCollectBetsAndReset());
		assertEquals(10.5f, tg.getCurrentPot(), 0.001);
		assertEquals(0, tg.getRequiredBet(), 0.001);
		
	}
	////////////// Equalise tests ///////////////////////
	@Test
	public void testPotHasEqualisedIfAllChecked() {

		// All players have £10 to start.
		// Add players and set round
		addPlayers(9, tg);
		tg.setGameState(TexasGameState.FLOP);
		tg.setRequiredBet(0f);
		
		// Check all players by adding a zero bet
		tg.getPlayers().forEach(p -> p.getCurrentStack().addToTable(TexasGameState.FLOP, 0));

		// then
		assertTrue(tg.hasPotEqualized());
		assertTrue(tg.tryCollectBetsAndReset());
		
	}
	
	@Test
	public void testPotHasEqualisedIfAllCheckedOnRiver() {

		// All players have £10 to start.
		// Add players and set round
		addPlayers(9, tg);
		tg.setGameState(TexasGameState.RIVER);
		tg.setRequiredBet(0f);
		
		// Check all players by adding a zero bet
		tg.getPlayers().forEach(p -> p.getCurrentStack().addToTable(TexasGameState.RIVER, 0));

		// then
		assertTrue(tg.hasPotEqualized());
		assertTrue(tg.tryCollectBetsAndReset());
		
	}
	@Test
	public void testPotHasNotEqualisedIfAPlayerHasntBeen() {

		// All players have £10 to start.
		// Add players and set round
		final Player pA = TestUtils.makePlayer("A",0);
		final Player pB = TestUtils.makePlayer("B",1);
		final Player pC = TestUtils.makePlayer("C",2);
		tg.addPlayer(pA);tg.addPlayer(pB);tg.addPlayer(pC);
		tg.setGameState(TexasGameState.FLOP);
		
		tg.setRequiredBet(7.5f);
		
		// Add the bets
		pA.getCurrentStack().addToTable(TexasGameState.FLOP, 7.5f);
		pB.getCurrentStack().addToTable(TexasGameState.FLOP, 7.5f);//all-in
		//pC.getCurrentStack().addToTable(GameState.FLOP, 7.5f); player not bet

		assertFalse(tg.hasPotEqualized());
		assertFalse(tg.tryCollectBetsAndReset());
	}
	
	@Test
	public void testPotEqualiseAfterFoldPreFlop() {
		// All players have £10 to start.
		// Add players and set round
		final Player pA = TestUtils.makePlayer("A",0);
		final Player pB = TestUtils.makePlayer("B",1);
		final Player pC = TestUtils.makePlayer("C",2);
		final Player pD = TestUtils.makePlayer("D",3);
		final Player pE = TestUtils.makePlayer("E",4);
		tg.addPlayer(pA);tg.addPlayer(pB);tg.addPlayer(pC);tg.addPlayer(pD);tg.addPlayer(pE);
		
		tg.setGameState(TexasGameState.POST_DEAL);
		tg.setRequiredBet(0.4f);
		
		pA.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 0.2f);//will
		pB.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 0.4f);//mike
		pC.getState().setLastAction(PlayerActionType.FOLD);//ben
		pD.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 0.4f);//Euan
		pE.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 0.4f);//Fabes
		pA.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 0.2f);//will (Call BB)
		
		assertTrue(tg.hasPotEqualized());
	}

	
	@Test
	public void testPotHasEqualisedIfAPlayerIsAllIn() {
		
		// All players have £10 to start.
		// Add players and set round
		final Player pA = TestUtils.makePlayer("A",0);
		final Player pB = TestUtils.makePlayer("B",1);
		final Player pC = TestUtils.makePlayer("C",2);
		tg.addPlayer(pA);tg.addPlayer(pB);tg.addPlayer(pC);
		tg.setGameState(TexasGameState.FLOP);
		
		// Set B with £5
		pB.getCurrentStack().setStack(5);
		
		tg.setRequiredBet(7.5f);
		
		// Add the bets
		pA.getCurrentStack().addToTable(TexasGameState.FLOP, 7.5f);
		pB.getCurrentStack().addToTable(TexasGameState.FLOP, 5f);//all-in
		pC.getCurrentStack().addToTable(TexasGameState.FLOP, 7.5f);

		assertTrue(tg.hasPotEqualized());
	}
	
	@Test
	public void testPotNotEqualisedIfLastPlayerAllIn() {
		
		// All players have £10 to start.
		// Add players and set round
		final Player pA = TestUtils.makePlayer("A",0);
		final Player pB = TestUtils.makePlayer("B",1);
		final Player pC = TestUtils.makePlayer("C",2);
		tg.addPlayer(pA);tg.addPlayer(pB);tg.addPlayer(pC);
		tg.setGameState(TexasGameState.FLOP);
		
		// Set C with £5
		pC.getCurrentStack().setStack(5f);
		
		// Add the bets
		pA.getCurrentStack().addToTable(TexasGameState.FLOP, 0f);
		pB.getCurrentStack().addToTable(TexasGameState.FLOP, 0f);
		pC.getCurrentStack().addToTable(TexasGameState.FLOP, 5f);
		tg.setRequiredBet(5f);

		assertFalse(tg.hasPotEqualized());
	}
	
	//////////////////////////////////////////////////////////////////////////
	
	@Test
	public void testSplitPotLeftoverIsRefunded() {
		addPlayers(3, tg);
		Player A = tg.getPlayers().getPlayerById("A");
		Player B = tg.getPlayers().getPlayerById("B");
		Player C = tg.getPlayers().getPlayerById("C");
		// Set player C with lower stack
		A.getCurrentStack().setStack(3f);
		
		tg.setGameState(TexasGameState.TURN);
		tg.addPlayerBet(mockAction("A", PlayerActionType.BET, 2f), A);
		tg.setRequiredBet(2f);// as this happens through player action
		tg.addPlayerBet(mockAction("B", PlayerActionType.BET, 2f), B);
		tg.addPlayerBet(mockAction("C", PlayerActionType.BET, 2f), C);
		
		assertTrue(tg.tryCollectBetsAndReset());
		assertEquals(6f, tg.getCurrentPot(), 0.001);
		tg.setGameState(TexasGameState.FLOP);
		
		tg.addPlayerBet(mockAction("A", PlayerActionType.ALL_IN, 4f), A);
		tg.setRequiredBet(4f);// as this happens through player action
		tg.addPlayerBet(mockAction("B", PlayerActionType.BET, 4f), B);
		tg.addPlayerBet(mockAction("C", PlayerActionType.BET, 4f), C);
		
		// A is All-in now
		assertEquals(0, A.getCurrentStack().getStack(), 0.01);
		
		assertTrue(tg.tryCollectBetsAndReset());
		assertEquals(6f + 9f, tg.getCurrentPot(), 0.001); // Total pot is £15
		
		A.addCard(new Card("5H")); A.addCard(new Card("QD"));//full-house (1) 
		B.addCard(new Card("3H")); B.addCard(new Card("QC"));// Qs and 5s (2) 
		C.addCard(new Card("8H")); C.addCard(new Card("JC"));// 8s and 5s (3)
		
		List<Card> cardsOnTable = Card.makeCards("3D", "5S", "5D", "8C", "QS");
		
		// When
		new PokerHandEvaluator().calculatePotsAndWinners(tg.getPlayers(), cardsOnTable);

		// Total pot is £15
		// A all-in, but top hand, so takes the first round and £1 off each in the 2nd round
		assertEquals(3 - 3 + 6 + 3, A.getCurrentStack().getStack(), 0.01);// A only wins deal and flop
		// B started at £10, bet £6 and won side-pot of £6 (2 x £3)
		assertEquals(10 - 6 + 6, B.getCurrentStack().getStack(), 0.01);
		// C started at 10, bet 6 and lost
		assertEquals(10 - 6, C.getCurrentStack().getStack(), 0.01);
	}
	
	@Test
	public void testPlayerCantFoldOutOfTurn() {
		addPlayers(3, tg);
		tg.startNextRound(false);

		assertEquals(TexasGameState.PRE_DEAL, tg.getGameState());
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 9.90f);
		assertValidAction("C", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 9.8f);
		
		// Action on A, B can't fold
		assertInvalidAction("B", PlayerActionType.FOLD, 2f, false);
		assertInvalidAction("C", PlayerActionType.FOLD, 2f, false);
	}
	
	@Test
	public void testAutocompleteGameToEnd() {
		addPlayers(2, tg);
		tg.startNextRound(false);

		assertEquals(TexasGameState.PRE_DEAL, tg.getGameState());

		final Player pA = tg.getPlayers().getPlayerById("A");
		final Player pB = tg.getPlayers().getPlayerById("B");
		pA.getState().setActionOn(true);
		// Both players go all-in
		pA.getCurrentStack().addToTable(TexasGameState.PRE_DEAL, 10f);
		pB.getCurrentStack().addToTable(TexasGameState.PRE_DEAL, 10f);
		assertEquals(DealResult.AUTOCOMPLETING, tg.autoCompleteIfNoBets());
		
		//TODO: Verify a bunch of actions here
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());
	}
	/**
	 * 
	 * @param id Player ID
	 * @param action The action
	 * @param betVal The value being bet
	 * @param stackVal value in stack after action
	 */
	private void assertValidAction(final String id, PlayerActionType action, float betVal, float stackVal) {
		assertValidAction(id, action, betVal, stackVal, tg);
	}
	/**
	 * 
	 * @param id Player ID
	 * @param action The action
	 * @param betVal The value being bet
	 * @param stackVal value in stack after action
	 */
	private void assertValidAction(final String id, PlayerActionType action, float betVal, float stackVal, TexasHoldemGame tg) {
		GameUpdateMessage gum = tg.doGameUpdateAction(mockAction(id, action, betVal));
		
		assertFalse("Gameupdate null", gum == null);
		
		assertNotEquals(GameUpdateType.WAITING_ON_ACTION.name(), gum.getMessage());
		assertNotNull("Current game null, but shouldnt be", gum.getCurrentGame());
		assertEquals(MessageTypes.GAME_UPDATE, gum.getMessageType());
		PlayerStack stack = tg.getPlayers().getPlayerById(id).getCurrentStack();
		assertEquals(betVal, stack.getOnTable(), 0.001);
		if (stackVal!=-1) {
			assertEquals(stackVal, stack.getStack(), 0.001);
		}
	}

	/** Test an action fails
	 * 
	 * @param id
	 * @param action
	 * @param betVal
	 * @param checkPreDealAction
	 */
	private void assertInvalidAction(final String id, PlayerActionType action, float betVal, boolean checkPreDealAction) {
		final PlayerActionMessage doAction = mockAction(id, action, betVal);
		GameUpdateMessage gum = tg.doGameUpdateAction(doAction);
		
		assertNull(gum);
		
		assertFalse(doAction.isSuccessful());
		assertNotNull(doAction.getMessage());

		if(checkPreDealAction) {
			Player p = tg.getPlayers().getPlayerById(id);
			assertFalse(p.getState().isFolded());
			assertEquals(0, tg.getCardsOnTable().size());
			assertEquals(TexasGameState.PRE_DEAL, tg.getGameState());
			assertTrue(p.getState().isActionOnMe());
		}
	}
	
	@Test
	public void testFailToCollectBetsWhenAllTheSame() {
		
		addPlayers(5, tg);
		tg.getPlayers().forEach(p->p.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 5));
		
		// All players have the same, but states are null
		Assert.assertFalse(tg.tryCollectBetsAndReset());

		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.NONE));
		// All players have the same, but states are NONE
		Assert.assertFalse(tg.tryCollectBetsAndReset());
	}
	
	@Test /* Real-world case */
	public void testOneOfThreePlayersRevealOnRiverCalculatesCorrectly() {
		tg.setEvaluator(new PokerHandEvaluator());
		tg.getSettings().setAnte(0.8f);
		tg.addDealCompleteCallback((c,r)->{
			c.clear();
			c.addAll(Card.makeCards("4C","4D","KD", "6C", "8D"));
		});

		
		final Player pAl = TestUtils.makePlayer("A", 1);
		final Player pDe = TestUtils.makePlayer("D", 7);
		final Player pJa = TestUtils.makePlayer("J", 8);
		
		tg.addPlayer(pAl);tg.addPlayer(pJa);tg.addPlayer(pDe);

		pAl.getCurrentStack().setStack(21.8f);
		pDe.getCurrentStack().setStack(4.2f);
		pJa.getCurrentStack().setStack(54f);
		
		// Set dealer
		pJa.getState().setDealer(true);
		pAl.getState().setDealer(false);
		pDe.getState().setDealer(false);
		
		tg.startNextRound(false);

		assertValidAction("A", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 21);
		assertValidAction("D", PlayerActionType.POST_BLIND, tg.getSettings().getBigBlind(), 2.6f);
		assertValidAction("J", PlayerActionType.CALL, 1.6f, 52.4f);
		assertValidAction("A", PlayerActionType.BET, 3.2f, 18.6f);
		assertValidAction("D", PlayerActionType.CALL, 3.2f, 1f);
		assertValidAction("J", PlayerActionType.CALL, 0f, 50.8f);// 0 because bets collected
		
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Set the cards correctly
		pJa.setCards(Card.makeCards("3S","AS"));
		pAl.setCards(Card.makeCards("KH","KS"));
		pDe.setCards(Card.makeCards("9H","9S"));
		
		assertValidAction("A", PlayerActionType.BET, 8f, 10.6f);
		assertValidAction("D", PlayerActionType.ALL_IN, 1f, 0f);
		assertValidAction("J", PlayerActionType.FOLD, 0, 50.8f);
		
		assertEquals(TexasGameState.COMPLETE, tg.getGameState());

		// Check pots and winner
		final GamePots gp = tg.getFinalPots();
		assertEquals("A", gp.getWinners().get(0).getPlayerId());
		final SidePot sp = gp.getAllPots().get(GamePots.FIRST_POT_NAME);
		assertEquals("A", sp.getPotWinners().get(0).getPlayerId());
		assertEquals(11.6, sp.getPotTotal(), 0.01);
		
		// Check hands
		assertEquals(PokerHand.TWO_PAIR, pDe.getRankedHand().getRankName());
		assertEquals(PokerHand.FULL_HOUSE, pAl.getRankedHand().getRankName());
	}
	
	@Test
	public void testWhenAllBetsZero() {
		
		// All checked 0
		addPlayers(6, tg);
		tg.getPlayers().forEach(p->p.getCurrentStack().addToTable(tg.getGameState(), 0f));
		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.CHECK));
		
		Assert.assertTrue(tg.tryCollectBetsAndReset());
		Assert.assertEquals(0d, tg.getCurrentPot(), 0.001);
		
		// If someone checked, and someone called, it's fine as can call a zero bet
		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.CHECK));
		getPlayerSeatedAt(tg, 1).getState().setLastAction(PlayerActionType.CALL);
		Assert.assertTrue(tg.tryCollectBetsAndReset());
		Assert.assertEquals(0d, tg.getCurrentPot(), 0.001);
		
		// Check VPIP
		tg.getPlayers().forEach(p -> p.getCurrentStack().getGameStats().updateStats(false));
		assertEquals(0, getStats("A").getVpip(), 0.1);// checked
		assertEquals(0, getStats("B").getVpip(), 0.1);// checked
		assertEquals(0, getStats("C").getVpip(), 0.1);// checked
		assertEquals(0, getStats("D").getVpip(), 0.1);// checked
		assertEquals(0, getStats("E").getVpip(), 0.1);// checked
	}
	
	@Test
	public void testCollectWhenSomeoneFolded() {
		
		// All called 5
		addPlayers(6, tg);
		tg.getPlayers().forEach(p->p.getCurrentStack().addToTable(tg.getGameState(), 5));
		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.CALL));
		tg.setRequiredBet(5);
		
		// Set one folded player differently
		Player player = getPlayerSeatedAt(tg, 1);
		player.getState().setLastAction(PlayerActionType.FOLD);
		
		Assert.assertTrue(tg.tryCollectBetsAndReset());
		Assert.assertEquals(30d, tg.getCurrentPot(), 0.001);// 6 x 5
		
		// Same thing if sitting out
		tg.setRequiredBet(5);
		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.CALL));
		player.getState().setLastAction(PlayerActionType.RAISE);
		player.getState().toggleSittingOut(TexasGameState.POST_DEAL, true);
		
		Assert.assertTrue(tg.tryCollectBetsAndReset());
		Assert.assertEquals(30d, tg.getCurrentPot(), 0.001); // Same pot as last time
	}
	
	@Test
	public void testPlayersHadDifferentActionsButPotEqualized() {
		
		addPlayers(6, tg);
		
		tg.getPlayers().forEach(p->p.getCurrentStack().addToTable(tg.getGameState(), 5));
		tg.setRequiredBet(5);
		getPlayerSeatedAt(tg, 0).getState().setLastAction(PlayerActionType.RAISE);
		getPlayerSeatedAt(tg, 1).getState().setLastAction(PlayerActionType.CASH_OUT);//a fold
		getPlayerSeatedAt(tg, 2).getState().setLastAction(PlayerActionType.CALL);
		getPlayerSeatedAt(tg, 3).getState().setLastAction(PlayerActionType.REVEAL);//a fold
		getPlayerSeatedAt(tg, 4).getState().setLastAction(PlayerActionType.FOLD);
		getPlayerSeatedAt(tg, 5).getState().setLastAction(PlayerActionType.ALL_IN);
	
		Assert.assertTrue(tg.tryCollectBetsAndReset());
		Assert.assertEquals(30d, tg.getCurrentPot(), 0.001); // 6 x 5 (incl. fold)
		
	}
	
	@Test
	public void testFailToCollectAllBetsDifferent() {
		
		addPlayers(5, tg);
		// Set values different
		float d = 1;
		for (Player p : tg.getPlayers()) {
			p.getCurrentStack().addToTable(TexasGameState.POST_DEAL, 5 + d);
			d++;
		}
		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.CHECK));
		
		// All players have the same, but states are null
		Assert.assertFalse(tg.tryCollectBetsAndReset());
	}
	
	@Test
	public void testCollectAllBetsSame() {
		
		addPlayers(5, tg);
		tg.getPlayers().forEach(p->p.getCurrentStack().addToTable(tg.getGameState(), 5));
		tg.setRequiredBet(5);
		tg.getPlayers().forEach(p->p.getState().setLastAction(PlayerActionType.CHECK));
		
		Assert.assertTrue(tg.tryCollectBetsAndReset());
		Assert.assertEquals(25d, tg.getCurrentPot(), 0.001);
	}

	
	@Test
	public void testActionBlockedWhilstProcessingSamePlayer() {
		addPlayers(3, tg);
		final Player pA = tg.getPlayers().getPlayerById("A");
		
		final float betVal = tg.getSettings().getBigBlind();
		// Set all players with 2.00
		tg.getPlayers().forEach(p->p.getCurrentStack().setStack(2f));
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 1.9f);
		assertValidAction("C", PlayerActionType.POST_BLIND, betVal, 2-betVal);
		
		assertEquals(TexasGameState.POST_DEAL, tg.getGameState());
		assertTrue(pA.getState().isActionOnMe());
		
	    // CountDownLatch used to release all the threads at the same time
	    final CountDownLatch startSignal = new CountDownLatch(1);
	    final CountDownLatch doneSignal = new CountDownLatch(2);
		// Set D as all-in
		new Thread(()->{
			try {
				startSignal.await();
				assertValidAction("A", PlayerActionType.CALL, betVal, 2-betVal);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				doneSignal.countDown();
			}
			
		}).start();
		new Thread(()->{
			try {
				startSignal.await();
				GameUpdateMessage gum = tg.doGameUpdateAction(mockAction("A", PlayerActionType.FOLD, betVal));
				assertEquals(GameUpdateType.WAITING_ON_ACTION.name(), gum.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				doneSignal.countDown();
			}
		}).start();
		
	    // Release the threads
	    startSignal.countDown();
	    // Wait until all threads did their task
	    try {
			doneSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private PlayerActionMessage mockAction(final String id, final PlayerActionType type, final  float betVal) {
		PlayerActionMessage action = mock(PlayerActionMessage.class);
		when(action.getAction()).thenReturn(type);
		when(action.getSessionId()).thenReturn(id);
		when(action.setSuccessful(Mockito.anyBoolean())).thenCallRealMethod();
	//	when(action.setMessage(Mockito.anyString())).thenCallRealMethod();
		when(action.getMessage()).thenCallRealMethod();
		when(action.isSuccessful()).thenCallRealMethod();
		
		Mockito.lenient().when(action.getBetValue()).thenReturn(betVal);
		return action;
	}
	
	private void addPlayers(int num, TexasHoldemGame g) {
		for (int i = 0; i < num; i++) {
			g.addPlayer(TestUtils.makePlayer(ALPHABET[i] + "", i));
		}
	}
	
	private AccountStats getStats(final String pId) {
		return tg.getPlayers().getPlayerById(pId).getCurrentStack().getGameStats();
	}
	/** Start a new game with 5 players and place all bets, auto-completing the whole game
	 * by posting blinds and then checking from flop to complete
	 * 
	 * @param tg The game
	 */
	private void checkHandsThroughToCompletion(TexasHoldemGame tg) {
		addPlayers(5, tg);
		
		final float betVal = tg.getSettings().getBigBlind();
		// Set all players with 2.00
		tg.getPlayers().forEach(p->p.getCurrentStack().setStack(2f));
		
		tg.startNextRound(true);
		assertValidAction("B", PlayerActionType.POST_BLIND, tg.getSettings().getAnte(), 1.9f, tg);
		assertValidAction("C", PlayerActionType.POST_BLIND, betVal, 2-betVal, tg);
		
		// Set D as all-in
		assertValidAction("D", PlayerActionType.CALL, betVal, 2-betVal, tg);
		assertValidAction("E", PlayerActionType.CALL, betVal, 2-betVal, tg);
		assertValidAction("A", PlayerActionType.CALL, betVal, 2-betVal, tg);
		assertValidAction("B", PlayerActionType.CALL, betVal, 2-betVal, tg);
		assertValidAction("C", PlayerActionType.CHECK, 0f, 2-betVal, tg); // C was big, so check
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Check the remaining rounds to completion
		checkFlopToComplete("BCDEA", 2-betVal, tg);
		
		// check the game state
		assertEquals(1f, tg.getCurrentPot(), 0.001);
		Mockito.verify(sevenCardEvaluator).calculatePotsAndWinners(Mockito.any(), Mockito.any());
	}
	
	/** Check all hands on the provided string ids (i.e. BCDA) from the flop through to completion
	 * 
	 * @param ids
	 * @param prevBet
	 * @param tg
	 */
	private void checkFlopToComplete(final String ids, final float prevBet, TexasHoldemGame tg) {
		assertEquals(TexasGameState.FLOP, tg.getGameState());
		
		// Check the remaining rounds to completion
		char[] pls = ids.toCharArray();
		for (int h=0; h < 3; h++) {
			for (int i=0; i < ids.length(); i++) {
				assertValidAction(""+pls[i], PlayerActionType.CHECK, 0.0f, prevBet, tg);
			}
			if (h==0) {
				assertEquals(TexasGameState.TURN, tg.getGameState());
			} else if (h==1) {
				assertEquals(TexasGameState.RIVER, tg.getGameState());
			} else if (h==2) {
				assertEquals(TexasGameState.COMPLETE, tg.getGameState());
			} else {
				assertTrue("Game didn't complete", false);
			}
		}
	}
	
	private Player getPlayerSeatedAt(TexasHoldemGame tg, int pos) {
		return tg.getPlayers().stream()
			.filter(p->p.getSeatingPos()==pos)
			.findFirst().orElse(null);
	}


}
