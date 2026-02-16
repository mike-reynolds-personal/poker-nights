package com.langleydata.homepoker.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.langleydata.homepoker.api.ActiveGame;
import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.exception.NoKnownServerException;
import com.langleydata.homepoker.persistence.es.ESGenericSettingsProvider;

@RunWith(MockitoJUnitRunner.class)
public class AbstractServiceDiscoveryTest {

	@Mock
	private ESGenericSettingsProvider settingsProvider;
	@Mock
	private HttpClientUtils httpClientUtils;
	@InjectMocks
	MockDiscovery underTest;
	private Map<String, Object> document = new HashMap<>();
	
	private String localUri ="https://localhost:8443";
	private Gson gson = new Gson();
	
	@Before
	public void setup() throws ClientProtocolException, IOException {
		MockitoAnnotations.initMocks(this);
		setHttpClientResp(new GameServer(localUri, GameType.TEXAS_HOLDEM));
	}
	
	private void setHttpClientResp(GameServer gs) throws ClientProtocolException, IOException {
		when(httpClientUtils.getForString(Mockito.any())).thenReturn(gson.toJson(gs));
	}

	@Test
	public void testLeastLoadedLookupWithNoActiveGames() throws ClientProtocolException, IOException {

		setHttpClientResp(new GameServer(localUri, GameType.TEXAS_HOLDEM));
		
		GameServer ret = underTest.getLeastLoaded(GameType.TEXAS_HOLDEM, true);
		
		assertEquals(0, ret.getGameCount());
		assertEquals(localUri, ret.getUri());
		assertEquals(GameType.TEXAS_HOLDEM, ret.getGameType());
		Assert.assertNotNull( underTest.knownServers.get(localUri) );// Check cached
	}
	
	@Test
	public void testLeastLoadedLookupGetLeast() throws UnsupportedOperationException, IOException {
		final GameServer sResp1 = new GameServer(localUri, GameType.TEXAS_HOLDEM);
		sResp1.setActiveGames(List.of(new ActiveGame("test"), new ActiveGame("test2"), new ActiveGame("test3"), new ActiveGame("test4")));
		final GameServer sResp2 = new GameServer(localUri, GameType.TEXAS_HOLDEM);
		sResp2.setActiveGames(List.of( new ActiveGame("test2"), new ActiveGame("test3"), new ActiveGame("test4")));
		final GameServer sResp3 = new GameServer(localUri, GameType.TEXAS_HOLDEM);
		sResp3.setActiveGames(List.of( new ActiveGame("test3"), new ActiveGame("test4")));
		final GameServer sResp4 = new GameServer(localUri, GameType.TEXAS_HOLDEM);
		sResp4.setActiveGames(List.of( new ActiveGame("test2")));
		underTest.setMulti();
		
		when(httpClientUtils.getForString(Mockito.any())).thenReturn(
				gson.toJson(sResp1), 
				gson.toJson(sResp2),
				gson.toJson(sResp3),
				gson.toJson(sResp4)
				);
		
		GameServer ret = underTest.getLeastLoaded(GameType.TEXAS_HOLDEM, true);
		
		assertEquals(1, ret.getGameCount());
		assertEquals("test2", ret.getActiveGames().get(0).getGameId());
		assertEquals("https://localhost:8110", ret.getUri().toString());
		assertEquals(GameType.TEXAS_HOLDEM, ret.getGameType());
		
		// Check the server is cached for subsequent calls
		Assert.assertNotNull( underTest.knownServers.get("https://localhost:8110") );
	}
	
	@Test
	public void testLeastLoadedLookupFail() throws ClientProtocolException, IOException {

		setHttpClientResp(null);
		
		GameServer ret = underTest.getLeastLoaded(GameType.TEXAS_HOLDEM, false);
		
		assertEquals(Integer.MAX_VALUE, ret.getGameCount());
		assertEquals("localhost://unknown", ret.getUri().toString());
		assertEquals(GameType.TEXAS_HOLDEM, ret.getGameType());
	}
	
	@Test
	public void testGetAssignedServerWhenItsDead() throws IOException, NoKnownServerException {
		when(settingsProvider.getSettingsById("test")).thenReturn(document);
		document.put("assignedServer", "https://serverDied");
		
		GameServer ret = underTest.getByGameId("test", GameType.TEXAS_HOLDEM);
		assertEquals(localUri, ret.getUri());//Not the server that was assigned
		
		Mockito.verify(settingsProvider).storeSettings("test", document);// stored as doesn't match original
	}
	
	@Test
	public void testGetById() throws NoKnownServerException, IOException {

		document.put("gameId", "test");
		when(settingsProvider.getSettingsById("test")).thenReturn(document);
		
		// when
		GameServer ret = underTest.getByGameId("test", GameType.TEXAS_HOLDEM);
		
		assertEquals(1, ret.getGameCount());// the game is added
		assertEquals("test", ret.getActiveGames().get(0).getGameId());
		assertEquals(localUri, ret.getUri());
		assertEquals(GameType.TEXAS_HOLDEM, ret.getGameType());
		
		// now get again and it should be cached
		ret = underTest.getByGameId("test", GameType.TEXAS_HOLDEM);
		
		assertEquals(1, ret.getGameCount());// the game is added
		assertEquals("test", ret.getActiveGames().get(0).getGameId());
		
		Mockito.verify(settingsProvider).getSettingsById("test");// only called once
		Mockito.verify(settingsProvider).storeSettings("test", document);// stored as first retrieval
	}
	
	@Test
	public void testGetByIdWithServerAssigned() throws NoKnownServerException, IOException {

		when(settingsProvider.getSettingsById("test")).thenReturn(document);
		document.put("assignedServer", localUri.toString());
		
		GameServer ret = underTest.getByGameId("test", GameType.TEXAS_HOLDEM);
		
		assertEquals(1, ret.getGameCount());// the game is added
		assertEquals("test", ret.getActiveGames().get(0).getGameId());
		assertEquals(localUri, ret.getUri());
		assertEquals(GameType.TEXAS_HOLDEM, ret.getGameType());
		
		// Test its in the cache
		ret = underTest.getByGameId("test", GameType.TEXAS_HOLDEM);
		assertEquals("test", ret.getActiveGames().get(0).getGameId());
		assertEquals(localUri, ret.getUri());
		assertEquals(GameType.TEXAS_HOLDEM, ret.getGameType());
		
		Mockito.verify(settingsProvider).getSettingsById("test");// only called once
		Mockito.verify(settingsProvider, Mockito.never()).storeSettings("test", document);// not updated
	}

	static class MockDiscovery extends AbstractGameServiceDiscovery {
		boolean doMulti = false;
		void setMulti() {
			doMulti = true;
		}
		@Override
		public List<GameServer> lookupServices(GameType gameType) {
			if (doMulti) {
				List<GameServer> se = new ArrayList<>();
				se.add(new GameServer("https://localhost:8080", GameType.TEXAS_HOLDEM));
				se.add(new GameServer("https://localhost:8090", GameType.TEXAS_HOLDEM));
				se.add(new GameServer("https://localhost:8100", GameType.TEXAS_HOLDEM));
				se.add(new GameServer("https://localhost:8110", GameType.TEXAS_HOLDEM));
				return se;
			} else {
				return Collections.singletonList(new GameServer("https://localhost:8443", GameType.TEXAS_HOLDEM));
			}
		}
	}
}
