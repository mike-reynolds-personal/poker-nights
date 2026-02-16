package com.langleydata.homepoker.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.TestUtils;
import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.CardGame.MoneyType;
import com.langleydata.homepoker.api.CardGame.ShuffleOption;
import com.langleydata.homepoker.controllers.TableController.ToEvict;
import com.langleydata.homepoker.exception.GameStartException;
import com.langleydata.homepoker.game.GameStats;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.players.SystemPlayer;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemGame;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.AllocateFundsMessage;
import com.langleydata.homepoker.message.AllocateFundsMessage.AllocateToPlayer;
import com.langleydata.homepoker.message.CashOutMessage;
import com.langleydata.homepoker.message.ChatMessage;
import com.langleydata.homepoker.message.CompleteGameMessage;
import com.langleydata.homepoker.message.EvictPlayerMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.LeaverMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.PlayerActionMessage;
import com.langleydata.homepoker.message.TransferFundsMessage;
import com.langleydata.homepoker.persistence.MessageHistoryProvider;
import com.langleydata.homepoker.persistence.RoundHistoryProvider;
import com.langleydata.homepoker.persistence.SettingsProvider;

@RunWith(MockitoJUnitRunner.class)
public class TableControllerTest {

	@Mock
	private MessageUtils msgUtils;
	@Mock
	private RoundHistoryProvider historyProvider;
	@Mock
	private MessageHistoryProvider messageProvider;
	@Mock
	private SettingsProvider settingStorage;
	@Mock
	private TexasHoldemGame thg;
	@Mock
	private TexasHoldemSettings gameSettings;
	@Mock
	private Players players;
	@Mock
	private AccountService accService;
	@Mock
	private Logger logger;
	
	@InjectMocks
	private TableController tc;
	
	private Gson gson = new Gson();
	
	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		when(thg.getPlayers()).thenReturn(players);
		tc.activeGames.put("ABC", thg);
		when(gameSettings.getGameId()).thenReturn("ABC");
		when(thg.getSettings()).thenReturn(gameSettings);
		when(gameSettings.getLocale()).thenReturn(new Locale("en", "GB"));
		when(gameSettings.getShuffleOption()).thenReturn(ShuffleOption.ALWAYS);
//		try {
//			when(accService.updateAccountStats(Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(true);
//		} catch (GameSchedulingException e) {
//			e.printStackTrace();
//		}
				
	}
	
	@Test
	public void testCompletingGameRemovesPlayers() {
		when(thg.getGameState()).thenReturn(TexasGameState.COMPLETE);
		
		Player pA = setPlayer("A", 0, true);
		Player pB = setPlayer("B", 0, false);
		Player pC = setPlayer("C", 0, false);
		pA.getCurrentStack().initialise(0, true);
		pB.getCurrentStack().initialise(0, true);
		pC.getCurrentStack().initialise(0, true);
		Players pls = new Players();
		pls.add(pA);pls.add(pB);pls.add(pC);
		when(thg.getPlayers()).thenReturn(pls);

		final Map<String, GameStats> stats = new HashMap<>();
		stats.put("A", Mockito.mock(GameStats.class));
		stats.put("B", Mockito.mock(GameStats.class));
		stats.put("C", Mockito.mock(GameStats.class));
		when(thg.getPlayerStats()).thenReturn(stats);
		
		pB.getState().setHost(true);
		when(thg.getPlayers()).thenReturn(pls);
		
		final CompleteGameMessage cgm = tc.completeGame("ABC", "B");
		
		assertNotNull(cgm);
		assertEquals(3, cgm.getStats().size());
		Mockito.verify(msgUtils, Mockito.never()).sendPrivateMessage(Mockito.anyString(), Mockito.any());
		
	}
	
	@Test
	public void testOnlyHostAllocateFunds() {

		Player pA = setPlayer("A", 0, true);
		Player pB = setPlayer("B", 0, false);
		pA.getCurrentStack().initialise(0, true);
		pB.getCurrentStack().initialise(0, true);
		
		final String sessionId = "B";// B isn't host
		AllocateFundsMessage afm = new AllocateFundsMessage(sessionId);
		afm.getAllocations().add( mkAllocate("A", 20f) );
		afm.getAllocations().add( mkAllocate("B", 20f) );

		
		// when
		final GameUpdateMessage gum = tc.allocateFunds("ABC", afm, sessionId);
		
		assertNull(gum.getCurrentGame());
		assertNotNull(gum.getMessage());
		
		assertEquals(0f, pA.getCurrentStack().getWallet(), 0.01);
		assertEquals(0f, pB.getCurrentStack().getWallet(), 0.01);
		assertEquals(0f, pA.getCurrentStack().getStack(), 0.01);
		assertEquals(0f, pB.getCurrentStack().getStack(), 0.01);
		
	}
	
	@Test
	public void testAllocateFundsSuccess() {
		
		when(thg.getGameState()).thenReturn(TexasGameState.COMPLETE);
		Player pA = setPlayer("A", 0, true);
		Player pB = setPlayer("B", 0, false);
		Player pC = setPlayer("C", 0, false);
		pA.getCurrentStack().initialise(0, true);
		pB.getCurrentStack().initialise(0, true);
		pC.getCurrentStack().initialise(0, true);
		
		// Usually done through adding player to game
		pA.getState().toggleSittingOut(TexasGameState.COMPLETE, true);
		pB.getState().toggleSittingOut(TexasGameState.COMPLETE, true);
		pC.getState().toggleSittingOut(TexasGameState.COMPLETE, true);
		
		final String sessionId = "A";
		AllocateFundsMessage afm = new AllocateFundsMessage(sessionId);
		afm.getAllocations().add( mkAllocate("A", 20f) );
		afm.getAllocations().add( mkAllocate("B", 20f) );
		
		when(gameSettings.getBuyInAmount()).thenReturn(10);
		when(gameSettings.getMoneyType()).thenReturn(MoneyType.VIRTUAL);
		when(gameSettings.isHostControlledWallet()).thenReturn(true);
		
		// when
		final GameUpdateMessage gum = tc.allocateFunds("ABC", afm, sessionId);
		
		// then
		final Player A = gum.getCurrentGame().getPlayers().getPlayerById("A");
		final Player B = gum.getCurrentGame().getPlayers().getPlayerById("B");
		final Player C = gum.getCurrentGame().getPlayers().getPlayerById("C");
		
		assertEquals(10f, A.getCurrentStack().getWallet(), 0.01);
		assertEquals(10f, A.getCurrentStack().getStack(), 0.01);
		assertEquals(10f, B.getCurrentStack().getWallet(), 0.01);
		assertEquals(10f, B.getCurrentStack().getStack(), 0.01);
		
		assertEquals(0f, C.getCurrentStack().getWallet(), 0.01);
		assertEquals(0f, C.getCurrentStack().getStack(), 0.01);
		
		assertFalse(pA.getState().isSittingOut());
		assertFalse(pB.getState().isSittingOut());
		assertTrue(pC.getState().isSittingOut());
		
		Mockito.verify(msgUtils).sendPrivateMessage(eq("B"), Mockito.any(ChatMessage.class));
	}
	
	@Test 
	public void testCompleteGameHostOnly() {
		// TODO Cash-out test
	}
	
	@Test 
	public void testCompleteGame() {
		// TODO Cash-out test
	}
	
	private AllocateToPlayer mkAllocate(String pId, float value) {
		JsonObject jo = new JsonObject();
		jo.addProperty("playerId", pId);
		jo.addProperty("wallet", value);
		return gson.fromJson(jo.toString(), AllocateToPlayer.class);
	}
	
	@Test
	public void testStartNewGameSuccess() {
		final Player A = setPlayer("A", 10, true);
		final Player B = setPlayer("B", 10, false);
		final Player C = setPlayer("C", 10, false);
		
		// when
		when(players.getDealer()).thenReturn(A);
		when(thg.getRound()).thenReturn(1);
		final GameUpdateMessage result = tc.startNextRound("ABC", false);
		
		// then
		assertNotNull(result.getCurrentGame());
		assertTrue(result.isNewGame());
		assertEquals("shuffle", result.getActionSound());
		
		Mockito.verify(msgUtils).sendBroadcastToTable(eq("ABC"), Mockito.any(ChatMessage.class));
		Mockito.verify(messageProvider).addPlayerAction(Mockito.any(PlayerActionMessage.class));
	}

	
	@Test
	public void testStartNewGameFail() {
		final Player A = setPlayer("A", 10, true);
		final Player B = setPlayer("B", 10, false);
		final Player C = setPlayer("C", 10, false);
		
		// when
		when(players.getDealer()).thenReturn(A);
		Mockito.doThrow(GameStartException.class).when(thg).startNextRound(eq(false));
		final GameUpdateMessage result = tc.startNextRound("ABC", false);
		
		// then
		assertNull(result.getCurrentGame());
		assertNotNull(result.getMessage());

		Mockito.verify(messageProvider).addPlayerAction(Mockito.any(PlayerActionMessage.class));
	}
	
	@Test
	public void testMoneyNotTransferedIfNoStack() {
		// Given
		final TransferFundsMessage tfm = new TransferFundsMessage("xyz");
		tfm.setAmount(5);
		tfm.setFromId("from");
		tfm.setToId("to");
		
		final Player host = setPlayer("host", 10, true);
		final Player from = setPlayer("from", 10, false);
		final Player to = setPlayer("to", 10, false);
		
		// When
		from.getCurrentStack().setStack(1f);
		final GameUpdateMessage gum = tc.transferStack("ABC", tfm, "host");
		
		// Then
		assertNull(gum);
		ArgumentCaptor<TransferFundsMessage> captor = ArgumentCaptor.forClass(TransferFundsMessage.class);
		Mockito.verify(msgUtils).sendPrivateMessage(eq("xyz"), captor.capture());
		
		assertFalse(captor.getValue().isSuccessful());
		assertEquals( "'From' player has insufficient funds in their wallet", captor.getValue().getMessage());
		
	}
	
	@Test
	public void testMoneyNotTransferedIfNoToPlayerProvided() {
		// Given
		final TransferFundsMessage tfm = new TransferFundsMessage("xyz");
		tfm.setAmount(5)
			.setFromId("from")
			.setPlayerId("host")
			.setPlayerHandle("host");
		//tfm.setToId("to");
		final Player host = setPlayer("host", 10, true);
		final Player from = setPlayer("from", 10, false);
		final Player to = setPlayer("to", 10, false);
		
		// When
		final GameUpdateMessage gum = tc.transferStack("ABC", tfm, "host");
		
		// Then
		assertNull(gum);
		ArgumentCaptor<TransferFundsMessage> captor = ArgumentCaptor.forClass(TransferFundsMessage.class);
		Mockito.verify(msgUtils).sendPrivateMessage(eq("xyz"), captor.capture());
		
		assertFalse(captor.getValue().isSuccessful());
		assertEquals( "No From or To playerId provided", captor.getValue().getMessage());
		
	}
	
	@Test
	public void testMoneyNotTransferedAddedToSamePlayer() {
		// Given
		final TransferFundsMessage tfm = new TransferFundsMessage("xyz");
		tfm.setAmount(5)
			.setFromId("from")
			.setToId("from")
			.setPlayerId("host")
			.setPlayerHandle("host");
		//tfm.setToId("to");
		
		final Player host = setPlayer("host", 10, true);
		final Player from = setPlayer("from", 10, false);
		final Player to = setPlayer("to", 10, false);
		
		// When
		final GameUpdateMessage gum = tc.transferStack("ABC", tfm, "host");
		
		// Then
		assertNull(gum);
		ArgumentCaptor<TransferFundsMessage> captor = ArgumentCaptor.forClass(TransferFundsMessage.class);
		Mockito.verify(msgUtils).sendPrivateMessage(eq("xyz"), captor.capture());
		
		assertFalse(captor.getValue().isSuccessful());
		assertEquals( "To and from players must be different", captor.getValue().getMessage());
		assertEquals(10, from.getCurrentStack().getStack(), 0.1f);
		
	}
	
	@Test
	public void testMoneyNotTransferedIfNotHostInitiated() {
		// Given
		final TransferFundsMessage tfm = new TransferFundsMessage("xyz");
		tfm.setAmount(5)
			.setFromId("from")
			.setToId("to")
			.setPlayerId("host")
			.setPlayerHandle("host");
		
		final Player host = setPlayer("host", 10, false);
		final Player from = setPlayer("from", 10, false);
		final Player to = setPlayer("to", 10, false);
		
		Players players = Mockito.mock(Players.class);
		Mockito.when(thg.getPlayers()).thenReturn(players);
		
		// When
		final GameUpdateMessage gum = tc.transferStack("ABC", tfm, "from");
		
		// Then
		assertNull(gum);
		
		ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
		Mockito.verify(msgUtils).sendPrivateMessage(eq("from"), captor.capture());
		
		assertEquals(SystemPlayer.ID, captor.getValue().getSessionId());
		assertEquals( "Only the host can perform this action", captor.getValue().getMessage());
		assertEquals(10, from.getCurrentStack().getStack(), 0.1f);
		
	}
	
	@Test
	public void testMoneyIsTransferedCorrectly() {
		// Given
		final TransferFundsMessage tfm = new TransferFundsMessage("xyz");
		tfm.setAmount(5)
			.setFromId("from")
			.setToId("to");
		
		final Player host = setPlayer("host", 10, true);
		final Player from = setPlayer("from", 10, false);
		final Player to = setPlayer("to", 10, false);
		
		Players players = Mockito.mock(Players.class);
		Mockito.when(thg.getPlayers()).thenReturn(players);
		Mockito.when(players.getPlayerBySessionId("host")).thenReturn(host);
		Mockito.when(players.getPlayerById("from")).thenReturn(from);
		Mockito.when(players.getPlayerById("to")).thenReturn(to);
		//Mockito.when(players.getHost()).thenReturn(host);
		
		// When
		final GameUpdateMessage gum = tc.transferStack("ABC", tfm, "host");
		
		// Then
		assertNotNull(gum);
		ArgumentCaptor<TransferFundsMessage> captor = ArgumentCaptor.forClass(TransferFundsMessage.class);
		Mockito.verify(msgUtils).sendPrivateMessage(eq("xyz"), captor.capture());
		Mockito.verify(msgUtils).sendPrivateMessage(eq("from"), captor.capture());
		Mockito.verify(msgUtils).sendPrivateMessage(eq("to"), captor.capture());
		
		assertTrue(captor.getValue().isSuccessful());
		assertEquals( "The host transfered £5.00 from from to to", captor.getValue().getMessage());
		
		assertEquals(5, from.getCurrentStack().getStack(), 0.1f);
		assertEquals(15, to.getCurrentStack().getStack(), 0.1f);
		
		Mockito.verify(messageProvider).addPlayerAction(tfm);
		
	}
	
	private Player setupEviction() {
		final Player host = setPlayer("host", 10, true);
		final Player evict = setPlayer("toEvict", 10, false);
		final Player other = setPlayer("another", 10, false);
		
		final EvictPlayerMessage epmEvict = new EvictPlayerMessage();
		epmEvict.setGameId("ABC");
		epmEvict.setPlayerHandle("The Host");
		epmEvict.setToEvictId("toEvict");
		
		Players players = Mockito.mock(Players.class);
		Mockito.when(thg.getPlayers()).thenReturn(players);
		Mockito.when(players.getPlayerBySessionId("host")).thenReturn(host);
		Mockito.when(players.getPlayerBySessionId("toEvict")).thenReturn(evict);
		Mockito.when(players.getHost()).thenReturn(host);
		
		// When
		tc.evictPlayer("ABC", epmEvict, "host");
		assertNotNull(tc.evictStates.get("toEvict"));
		
		return evict;
	}
	@Test
	public void testEvictedPlayerIsNotEvictedIfRejected() {

		final Player evict = setupEviction();
		
		// Then
		final ToEvict state = tc.evictStates.get("toEvict");
		assertEquals(evict, state.toEject);
		assertEquals(state.ejectStart, System.currentTimeMillis(), 10);
		Mockito.verify(msgUtils).sendPrivateMessage(eq("toEvict"), Mockito.any(EvictPlayerMessage.class));
		
		// Now reject
		final EvictPlayerMessage epmReject = new EvictPlayerMessage();
		epmReject.setGameId("ABC");
		epmReject.setPlayerHandle("The Evicted");
		epmReject.setToEvictId("toEvict");
		epmReject.setIsReject(true);
		
		// when
		tc.evictPlayer("ABC", epmReject, "host");
		
		assertNull(tc.evictStates.get("toEvict"));
		
	}
	
	@Test
	public void testPlayerEvictedIfNotRejected() {

		final Player evict = setupEviction();
		
		GameUpdateMessage mkUpdate = Mockito.mock(GameUpdateMessage.class);
		when(thg.doGameUpdateAction(Mockito.any(PlayerActionMessage.class))).thenReturn(mkUpdate);
		when(thg.removePlayer(eq("toEvict"))).thenReturn(evict);
		
		// Then
		final ToEvict state = tc.evictStates.get("toEvict");
		state.ejectStart = System.currentTimeMillis() - 31000;
		
		// when
		tc.evictPlayersProcess();
		
		//then
		assertNull(tc.evictStates.get("toEvict"));
		Mockito.verify(msgUtils).sendBroadcastToTable(eq("ABC"), Mockito.any(LeaverMessage.class));
		Mockito.verify(msgUtils).sendBroadcastToTable(eq("ABC"), Mockito.any(GameUpdateMessage.class), eq(500L));
		Mockito.verify(msgUtils).sendPrivateMessage(eq(evict.getSessionId()), Mockito.any(CashOutMessage.class));
		Mockito.verify(messageProvider).addPlayerAction(Mockito.any(PlayerActionMessage.class));
		
	}
	
	@Test
	public void testPlayerNotEvictedIfLessThanTimeout() {
		setupEviction();
		
		// Then
		assertNotNull(tc.evictStates.get("toEvict"));
		
		// when
		tc.evictPlayersProcess();
		
		//then
		assertNotNull(tc.evictStates.get("toEvict"));
		Mockito.verify(msgUtils, Mockito.never()).sendBroadcastToTable(Mockito.anyString(), Mockito.any());
		Mockito.verify(messageProvider, Mockito.never()).addPlayerAction(Mockito.any(PlayerActionMessage.class));
	}
	/** Setup a player
	 * 
	 * @param nameId Player ID and handle
	 * @param wallet The amount in their wallet, with buy-in transfered to stack
	 * @param host Is this the host?
	 * @return
	 */
	private Player setPlayer(final String nameId, int wallet, boolean host) {
		final Player player = TestUtils.makePlayer(nameId, nameId, 0, wallet);
		when(players.getPlayerById(eq(nameId))).thenReturn(player);
		when(players.getPlayerBySessionId(eq(nameId))).thenReturn(player);
		
		player.getState().setHost(host);

		assertEquals(wallet, player.getCurrentStack().getStack(), 0.1f);
		return player;
	}
}
