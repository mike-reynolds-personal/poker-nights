package com.langleydata.homepoker.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.util.ReflectionUtils;

import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.controllers.WebsocketSessionController.Disconnected;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.exception.ReconnectException;
import com.langleydata.homepoker.game.AbstractCardGame;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.PlayerState;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.players.SystemPlayer;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemGame;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.message.DealUserMessage;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.MessageUtils;
import com.langleydata.homepoker.message.PrivateJoinerMessage;
import com.langleydata.homepoker.message.SubscribeMessage;

@RunWith(MockitoJUnitRunner.class)
public class WebscoketSessionControllerTest {

	@Mock
	private MessageUtils msgUtils;
	@Mock
	private TexasHoldemGame txGame;
	@Mock
	private Players mkPlayers;
	@Mock
	private TexasHoldemSettings txSettings;
	@Mock
	Player plFred;
	@Mock
	Player plBob;
	@Mock
	Player plJim;
	@Mock
	TableController tableController;
	@InjectMocks
	private WebsocketSessionController sc;
	
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(txGame.getPlayers()).thenReturn(mkPlayers);
		when(txGame.getSettings()).thenReturn(txSettings);
		when(txSettings.getGameId()).thenReturn("game-id0");
		tableController.addActiveGame(txGame);
		Map<String, AbstractCardGame<?>> list = new HashMap<>();
		list.put("game-id0", txGame);
		Mockito.when(tableController.getActiveGames()).thenReturn(list);
		setupPlayers();
		Mockito.doReturn(txGame).when(tableController).getActiveGame("game-id0");
		Mockito.when(mkPlayers.getPlayerById(plBob.getPlayerId())).thenReturn(plBob);
	}
	
	@Test
	public void testSubscribeToPrivateFirstTime() {
		final String sId = "sessionId";
		final String handle = "bob";
		
		final SimpMessageHeaderAccessor mkHeaders = mkHeaders(sId, "private", handle, false);
		sc.onSubscribe(mkHeaders, plBob.getPlayerId());
		
		ArgumentCaptor<SubscribeMessage> captor = ArgumentCaptor.forClass(SubscribeMessage.class);
		
		verify(msgUtils).sendPrivateMessage(eq(sId), captor.capture());
		
		assertEquals(plBob.getPlayerId(), captor.getValue().getPlayerId());
		assertEquals(sId, captor.getValue().getSessionId());
		assertEquals(handle, captor.getValue().getPlayerHandle());

	}
	
	@Test
	public void testSubscribeToTableFirstTime() {
		Mockito.when(mkPlayers.getPlayerById(plBob.getPlayerId())).thenReturn(null);
		final String trackId = plBob.getPlayerId() + "|" + txSettings.getGameId();
		final String sId = plBob.getSessionId();
		final String handle = "bob";
		
		final SimpMessageHeaderAccessor mkHeaders = mkHeaders(sId, "table", handle, false);
		sc.onSubscribe(mkHeaders, plBob.getPlayerId());
		
		// Gets an initial table update
		ArgumentCaptor<GameUpdateMessage> captor = ArgumentCaptor.forClass(GameUpdateMessage.class);
		
		verify(msgUtils).sendPrivateMessage(eq(plBob.getSessionId()), captor.capture());
		Assert.assertNotNull(captor.getValue().getCurrentGame());
		assertEquals(MessageTypes.GAME_UPDATE, captor.getValue().getMessageType());
		assertEquals(SystemPlayer.ID, captor.getValue().getSessionId());
		assertTrue(captor.getValue().getMessage().contains("Please 'Take a Seat' to join the game"));
		
		Assert.assertNull(sc.disConnStates.get(trackId));

	}
	
	@Test
	public void testSubscribeToTableSecondTimeSendsInformation() throws ReconnectException {
		final String sId = "bob-new";
		final String prevId = plBob.getSessionId();
		final String handle = "bob";
		final String trackId = plBob.getPlayerId() + "|" + txSettings.getGameId();
		
		Mockito.when(mkPlayers.getPlayerById(plBob.getPlayerId())).thenReturn(plBob);
		
		// Do disconnect with 'existing' id
		sc.updatePlayerConnectState(prevId);
		Assert.assertEquals("game-id0", sc.disConnStates.get(trackId).gameId);
		
		// Set-up mock player for second connect
		//Mockito.when(plBob.getSessionId()).thenReturn(sId);
		List<Card> cards = new ArrayList<>();
		cards.add(mock(Card.class));
		Mockito.when(plBob.getCards()).thenReturn(cards);

		// Re-connect
		final SimpMessageHeaderAccessor mkHeaders = mkHeaders(sId, "table", handle, true);
		sc.onSubscribe(mkHeaders, plBob.getPlayerId());

		// Check the player had the new session id set and they are no longer tracked for disconnection
		verify(plBob).setSessionId(sId);
		
		ArgumentCaptor<PrivateJoinerMessage> captor = ArgumentCaptor.forClass(PrivateJoinerMessage.class);

		// Verify the joiner message
		verify(msgUtils).sendPrivateMessage(eq(sId), captor.capture());
		Assert.assertNotNull(captor.getValue().getCurrentGame());
		assertEquals(handle, captor.getValue().getPlayer().getPlayerHandle());
		assertEquals(handle + ", you're back in the game!", captor.getValue().getMessage());
		assertTrue(captor.getValue().isSuccessful());
		assertTrue(captor.getValue().isReconnect());
		
		// Verify the card update
		verify(msgUtils).sendPrivateMessage(eq(sId), eq(MessageUtils.PRIVATE_QUEUE), Mockito.any(DealUserMessage.class), eq(100L));
		

		Assert.assertNull(sc.disConnStates.get(trackId));
	}
	
	@Test
	public void testReconnectedUserGetsDisconnected() throws ReconnectException {
		final String sess1 = plBob.getSessionId();
		final String sess2 = "bob-new";
		final String handle = "bob";
		final String trackId = plBob.getPlayerId() + "|" + txSettings.getGameId();
		
		// Initial connect
		SimpMessageHeaderAccessor mkHeaders = mkHeaders(sess1, "table", handle, false);
		sc.onSubscribe(mkHeaders, plBob.getPlayerId());
		
		Assert.assertNull(sc.disConnStates.get(trackId));
		
		// Disconnect
		sc.updatePlayerConnectState(sess1);
		Assert.assertEquals("game-id0", sc.disConnStates.get(trackId).gameId);
		
		// Re-connect with new session id
		mkHeaders = mkHeaders(sess2, "table", handle, true);
		sc.onSubscribe(mkHeaders, plBob.getPlayerId());
		Assert.assertNull(sc.disConnStates.get(trackId));
		
		// Final disconnect
		Mockito.when(plBob.getSessionId()).thenReturn(sess2);
		sc.updatePlayerConnectState(sess2);
		Assert.assertNotNull(sc.disConnStates.get(trackId));
	}
	
	@Test
	public void testUserTrackingOnDisconnect() throws ReconnectException {
		final String trackId = plBob.getPlayerId() + "|" + txSettings.getGameId();
		
		// Disconnect
		sc.updatePlayerConnectState(plBob.getSessionId());
		
		final Disconnected disCon = sc.disConnStates.get(trackId);
		Assert.assertEquals("game-id0", disCon.gameId);
		Assert.assertTrue(disCon.disConStart <=System.currentTimeMillis());
		Assert.assertTrue(disCon.disConStart >=System.currentTimeMillis()-1000);
		
		// reconnect
		sc.updatePlayerConnectState("bob-new", plBob.getPlayerId(), txGame, false);
		Assert.assertNull(sc.disConnStates.get(trackId));
		
	}

	@Test
	public void testConnectingUserINotsTracked() throws ReconnectException {
		final String trackId = plBob.getPlayerId() + "|" + txSettings.getGameId();
		
		sc.updatePlayerConnectState(plBob.getSessionId(), plBob.getPlayerId(), txGame, false);
		Assert.assertNull(sc.disConnStates.get(trackId));
	}
	
	@Test
	public void testDisconnectPlayerIsNotActioned() throws InterruptedException, ReconnectException {
		final String trackId = plBob.getPlayerId() + "|" + txSettings.getGameId();
		
		sc.updatePlayerConnectState(plBob.getSessionId());
		
		Assert.assertEquals(txSettings.getGameId(), sc.disConnStates.get(trackId).gameId);
		
		Thread.sleep(200);
		sc.updatePlayerState();
		Mockito.verify(txGame, Mockito.never()).pausePlayer(plBob.getPlayerId(), plBob.getSessionId());
		Mockito.verify(txGame, Mockito.never()).removePlayer(plBob.getPlayerId());
		
	}
	
	@Test
	public void testDisconnectPlayerIsPaused() {
		final String trackId = plJim.getPlayerId() + "|" + txSettings.getGameId();
		Disconnected cs = sc.new Disconnected(plJim, txSettings.getGameId());
		Field f = ReflectionUtils.findField(Disconnected.class, "disConStart");
		f.setAccessible(true);
		ReflectionUtils.setField(f, cs, System.currentTimeMillis() - WebsocketSessionController.SIT_OUT_DELAY - 10);
		sc.disConnStates.put(trackId, cs);
		
		sc.updatePlayerState();
		Mockito.verify(txGame).pausePlayer(plJim.getPlayerId(), plJim.getSessionId());
		Mockito.verify(txGame, Mockito.never()).removePlayer(plJim.getPlayerId());
		
	}
	
	@Test
	public void testDisconnectPlayerIsRemoved() {
		final String trackId = plJim.getPlayerId() + "|" + txSettings.getGameId();
		Disconnected cs = sc.new Disconnected(plJim, txSettings.getGameId());
		Field f = ReflectionUtils.findField(Disconnected.class, "disConStart");
		f.setAccessible(true);
		ReflectionUtils.setField(f, cs, System.currentTimeMillis() - WebsocketSessionController.REMOVE_DELAY - 10);
		sc.disConnStates.put(trackId, cs);
		
		sc.updatePlayerState();
		
		Mockito.verify(txGame, Mockito.never()).pausePlayer(plJim.getPlayerId(), plJim.getSessionId());
		Mockito.verify(txGame).removePlayer(plJim.getPlayerId());
		
		Assert.assertNull(sc.disConnStates.get("jim"));
		
	}
	
	/**
	 * 
	 * @param sessionId
	 * @param queue
	 * @param pHandle
	 * @param reconnect
	 * @return
	 */
	private SimpMessageHeaderAccessor mkHeaders(
			String sessionId, 
			String queue, 
			String pHandle, 
			boolean reconnect) {
		SimpMessageHeaderAccessor mkHdr = mock(SimpMessageHeaderAccessor.class);
		when(mkHdr.getSessionId()).thenReturn(sessionId);
		
		// Headers
		mkHeader(mkHdr, "to", queue);
		mkHeader(mkHdr, "gameId", "game-id0");
		mkHeader(mkHdr, "playerHandle", pHandle);
		mkHeader(mkHdr, "isReconnect", String.valueOf(reconnect));
		
		return mkHdr;
	}
	
	/**
	 * 
	 * @param mkHdr
	 * @param name
	 * @param value
	 */
	private void mkHeader(final SimpMessageHeaderAccessor mkHdr, String name, String value) {
		List<String> h = new ArrayList<>();
		h.add(value);
		when(mkHdr.getNativeHeader(eq(name))).thenReturn(h);
	}
	
	/**
	 * 
	 */
	private void setupPlayers() {
		lenient().when(plFred.getPlayerId()).thenReturn("fred-p");
		lenient().when(plBob.getPlayerId()).thenReturn("bob-p");
		lenient().when(plJim.getPlayerId()).thenReturn("jim-p");
		
		lenient().when(plFred.getSessionId()).thenReturn("fred");
		lenient().when(plBob.getSessionId()).thenReturn("bob");
		lenient().when(plJim.getSessionId()).thenReturn("jim");
		
		lenient().when(plFred.getState()).thenReturn(mock(PlayerState.class));
		lenient().when(plBob.getState()).thenReturn(mock(PlayerState.class));
		lenient().when(plJim.getState()).thenReturn(mock(PlayerState.class));
		
		lenient().when(plFred.getPlayerHandle()).thenReturn("fred");
		lenient().when(plBob.getPlayerHandle()).thenReturn("bob");
		lenient().when(plJim.getPlayerHandle()).thenReturn("jim");
		
		lenient().when(mkPlayers.stream()).then(i -> Stream.of(plFred, plBob, plJim));

	}
}
