package com.langleydata.homepoker.services;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.gson.Gson;
import com.langleydata.homepoker.api.ActiveGame;
import com.langleydata.homepoker.api.GameServer;
import com.langleydata.homepoker.api.GameType;
import com.langleydata.homepoker.exception.NoKnownServerException;
import com.langleydata.homepoker.persistence.GenericSettingsProvider;

/** An abstract service discovery class that can be extended to automatically find
 * one of many services that can serve a game or the default web content.<p>
 * Once a service has been looked-up then it is cached for faster response.
 * 
 * @author Mike Reynolds
 *
 */
public abstract class AbstractGameServiceDiscovery {
	protected final Logger logger = LoggerFactory.getLogger(AbstractGameServiceDiscovery.class);
	static final String ACTIVE_GAME_QUERY_PATH = "/info/active";
	static final int PURGE_SCHEDULE = 2 * 60 * 1000;// 2 mins
	protected final Map<String, GameServer> knownServers = Collections.synchronizedMap(new HashMap<>());
	
	@Autowired
	private GenericSettingsProvider settingsProvider;
	@Autowired
	HttpClientUtils httpClientUtils;
	
	private Gson gson = new Gson();

	
	/** Get details of a game server instance from the service itself, populating the required information
	 * 
	 * @param service The root URL of the service
	 * @return The GameServer object, or null if there was an error
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public GameServer getGameServerInfo(String service) throws NoKnownServerException, IOException {
		
		if (StringUtils.isBlank(service)) {
			throw new NoKnownServerException("Invalid service provided");
		}
		if (service.endsWith("/")) {
			service = service.substring(0, service.length()-1);
		}
		
		GameServer toRet = null;
		final String gsResponse = httpClientUtils.getForString(URI.create(service + ACTIVE_GAME_QUERY_PATH));

		if (gsResponse!=null && gsResponse.startsWith("{")) {
			toRet = gson.fromJson(gsResponse, GameServer.class);
			logger.trace("Received response: {}", toRet);
		} else {
			throw new NoKnownServerException(gsResponse == null ? "Requesting active games - response null" : gsResponse);
		}
		
		return toRet;
	}

	/** Get a server that can serve the provided game
	 * 
	 * @param gameId The gameId being requested
	 * @param gameType The type of game
	 * @return A GameServer which can, or already is, servicing the gameId
	 * 
	 * @throws NoKnownServerException If no game server can serve this game
	 */
	public GameServer getByGameId(final String gameId, final GameType gameType) throws NoKnownServerException {
		
		if (StringUtils.isBlank(gameId) || gameType == null) {
			throw new NoKnownServerException("Game parameters are null");
		}
		
		GameServer known = knownServers.get(gameId);
		
		if (known == null) {
			
			// See if the game has been assigned a server
			final Map<String, Object> document = getSettings(gameId);
			final String server = (String) document.get("assignedServer");
			final ActiveGame activeGame = new ActiveGame(gameId);
			activeGame.setGameFormat((String)document.get("format"));
			
			if (server == null) {
				// A server hasn't been assigned, so get one
				known = getLeastLoaded(gameType, false);
				if (known.getGameCount()==Integer.MAX_VALUE) {
					throw new NoKnownServerException("No service to serve game of type " + gameType);
				}
				
				// Only save back the allocated server if these are valid settings (i.e. it was created by a user)
				if (document.get("gameId") != null ) {
					known.addActiveGame(activeGame);
					document.put("assignedServer", known.getUri());
					try {
						settingsProvider.storeSettings(gameId, document);
					} catch (IOException e) {
						throw new NoKnownServerException("Could not update settings for " + known.getUri());
					}
					
				}
				
			} else {
				
				// The server is assigned, but this gateway doesn't know about it,
				// so create a new entry [so it does next time]

				// check the server is still available..
				final List<GameServer> running = lookupServices(gameType);
				GameServer toUse = running.stream()
									.filter(gs -> gs.getUri().equalsIgnoreCase(server))
									.findFirst()
									.orElse(getLeastLoaded(gameType, false, running));
				
				known = new GameServer(toUse.getUri(), gameType);
				known.addActiveGame(activeGame);
				
				if (toUse.getUri().equalsIgnoreCase(server)==false) {
					try {
						settingsProvider.storeSettings(gameId, document);
					} catch (IOException e) {
						throw new NoKnownServerException("Could not update settings for " + known.getUri());
					}
				}
			}
			// Store in the cache
			knownServers.put(gameId, known);
		}
		
		return known;
	}

	/** Get game settings for the provided ID, that isn't archived
	 * 
	 * @param gameId
	 * @return
	 * @throws NoKnownServerException
	 */
	private Map<String, Object> getSettings(final String gameId) throws NoKnownServerException {
		try {
			final Map<String, Object> settings = settingsProvider.getSettingsById(gameId);
			final boolean archived = (boolean) settings.getOrDefault("isArchived", false);
			if (archived) {
				throw new NoKnownServerException("No settings for game " + gameId);
			}
			return settings;
		} catch (IOException e) {
			throw new NoKnownServerException("Could not contact server", e);
		}
	}
	
	/** Get a list of raw information from the underlying service discovery provided.<p>
	 * This does not contact individual services.
	 * 
	 * @param gameType
	 * @return A list of raw GameServer information (aka the URL)
	 */
	public abstract List<GameServer> lookupServices(final GameType gameType);
	
	/** Get a failed GameServer response
	 * 
	 * @param gameType
	 * @return
	 */
	private GameServer getFailure(GameType gameType) {
		final GameServer fail = new GameServer("localhost://unknown", gameType);
		fail.setActiveGames(null);
		return fail;
	}
	
	/** Get the server with the least amount of active games
	 * 
	 * @param servers
	 * @param gameType
	 * @return
	 */
	private Optional<GameServer> getMin(Collection<GameServer> servers, GameType gameType) {
		return servers.stream()
				.filter(gs -> gs.getGameType()==gameType)
				.sorted(Comparator.comparing(GameServer::getGameCount, Comparator.naturalOrder()))
				.findFirst();
	}
	/** Get a server which can serve this game type, and is currently serving the 
	 * least amount of games.
	 *  
	 * @param gameType
	 * @param knownFirst Search the known servers first
	 * @return Always a valid GameServer, but a failure will have {@link GameServer#getGameCount()}
	 * set to Integer.MAX_VALUE
	 */
	public GameServer getLeastLoaded(GameType gameType, final boolean knownFirst) {
		return getLeastLoaded(gameType, knownFirst, lookupServices(gameType));
	}
	/** Get a server which can serve this game type, and is currently serving the 
	 * least amount of games.
	 *  
	 * @param gameType
	 * @param knownFirst Search the known servers first
	 * @param running a list of server instances to use for lookup
	 * @return Always a valid GameServer, but a failure will have {@link GameServer#getGameCount()}
	 * set to Integer.MAX_VALUE
	 */
	public GameServer getLeastLoaded(GameType gameType, final boolean knownFirst, List<GameServer> running) {
		
		final List<GameServer> loaded = new ArrayList<>();
		
		if (knownFirst) {
			final Optional<GameServer> known = getMin(knownServers.values(), gameType);
			if (known.isPresent()) {
				return known.get();
			}
		}

		for (GameServer gs : running) {
			try {
				final GameServer aG = getGameServerInfo(gs.getUri());
				if (aG != null) {
					aG.setUri(gs.getUri());
					loaded.add(aG);
					
					// Cache the server for faster response 
					aG.getActiveGames().forEach(active -> knownServers.put(active.getGameId(), aG));
					knownServers.put(aG.getUri(), aG);
				}
			} catch (IOException | NoKnownServerException e) {
				logger.error(e.getMessage() + ": " + gs.getUri());
			}
		}
		
		// Get the server with the least number of active games
		Optional<GameServer> least = getMin(loaded, gameType);
		return least.isPresent() ? least.get() : getFailure(gameType);
	}
	
	@Scheduled(fixedDelay = PURGE_SCHEDULE)
	public void purgeCache() {
		logger.trace("Purging {} cached servers from memory", knownServers.size());
		
		synchronized(knownServers) {
			knownServers.clear();
		}
	}
}
