package com.langleydata.homepoker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.DealCompleteCallback;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.Blinds;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemGame;
import com.langleydata.homepoker.game.texasHoldem.TexasHoldemSettings;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;
import com.langleydata.homepoker.message.GameUpdateMessage;
import com.langleydata.homepoker.message.Messaging;
import com.langleydata.homepoker.message.PlayerActionMessage;

/** A testing class used to replay player actions for a specific round, taken from the JSON
 * output of the Admin interface
 * 
 * @author Mike Reynolds
 *
 */
public class ReplayRound implements DealCompleteCallback {
	private final Logger logger = LoggerFactory.getLogger(ReplayRound.class);
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ReplayRound rr = new ReplayRound();
		rr.replayActions(new File("C:/temp/playerActions.json"));
	}

	/** Replay all actions defined as Json within a file
	 * 
	 * @param actionsFile
	 * @throws IOException
	 */
	public void replayActions(File actionsFile) throws IOException {
		
		TexasHoldemSettings settings = new TexasHoldemSettings();
		settings.setAnte(0.2f);
		TexasHoldemGame tg = new TexasHoldemGame(settings, new LogMessaging());
		tg.addDealCompleteCallback(this);
		
		// Copy the playerActions from the Admin console for a specific round

		final String inJson = FileUtils.readFileToString(actionsFile, "UTF-8");
		
		Gson gson = new Gson();
		JsonArray inRecs = gson.fromJson(inJson, JsonArray.class);
		List<PlayerAction> actions = new ArrayList<>();
		for (int i=0; i < inRecs.size(); i++) {
			actions.add(gson.fromJson(inRecs.get(i), PlayerActionMessage.class));
		}
		
		// Sort the actions in order and then get unique list of Player's
		actions.sort(Comparator.comparing(PlayerAction::getTimestamp, Comparator.naturalOrder()));
		final double maxBet = actions.stream().mapToDouble(a -> a.getBetValue()).sum();
		PAToPl map = new PAToPl();

		// Ideally want to build players from round history as well
		final List<Player> players = actions.stream()
				.map(map)
				.distinct()
				.collect(Collectors.toList());
		players.get(0).getState().setDealer(true);
		
		// Add the players to the game
		players.forEach(p -> tg.addPlayer(p));
		players.forEach(p -> p.getCurrentStack().setStack((float)maxBet) );
		tg.startNextRound(false);
		
		// Set all stacks here
		
		// Start on the first post blind, not the first action
		float totalBets = 0;
		for (int r=1; r < actions.size(); r++ ) {
			
			final PlayerActionMessage action = (PlayerActionMessage) actions.get(r);
			
			if (action.getAction()==PlayerActionType.RE_BUY || action.getAction()==PlayerActionType.SIT_OUT || action.getAction()==PlayerActionType.CASH_OUT) {
				continue;
			}
			
			// As session ids can change through a round, always update the player's 
			// session id to that of the action message
			final Player thisPlayer = tg.getPlayers().getPlayerById(action.getPlayerId());
			thisPlayer.setSessionId(action.getSessionId());
			if (thisPlayer.getState().getBlindsDue()==Blinds.BIG) {
				action.setBetValue(settings.getBigBlind());
			} else if (thisPlayer.getState().getBlindsDue()==Blinds.SMALL) {
				action.setBetValue(settings.getAnte());
			}
			
			if (action.getAction()==PlayerActionType.ALL_IN) {
				thisPlayer.getCurrentStack().setStack(action.getBetValue());
			}
			
			final String assertMsg = action.getPlayerHandle() + ": " + action.getAction() + ": " + action.getBetValue();
			System.out.println(assertMsg);
			// Check state before action has effect
			if (tg.getGameState() != action.getGameState()) {
				logger.info("Game state doesn't match expected state");
				return;
			}
			
			// What was it in real life?
			final boolean wasSuccess = action.isSuccessful();
			
			// Set-up the cards on the penaltimate action
			if (tg.getGameState()==TexasGameState.POST_DEAL) {
				setPlayerCards(tg);
			}
			
			GameUpdateMessage gum = tg.doGameUpdateAction(action);
			if (wasSuccess!=action.isSuccessful()) {
				logger.info("Successful doesn't match");
			}
			totalBets += action.getBetValue();
			
			if (gum!=null && gum.getMessageType()!=null) {
				if (gum.getMessageType() != MessageTypes.GAME_UPDATE) {
					logger.info("Game update doesn't match");
				}
			} else {
				logger.info("Update null or wrong: " + action.toString());
				
			}
		}
		
		System.out.println("-----------------");
		System.out.println(String.format("Total Bets: %s, Current Pot: %s", totalBets,tg.getCurrentPot()));
		System.out.println("-----------------");
		System.out.println("Name, R1, R2, R3, R4, Tot");
		for (Player p : tg.getPlayers()) {
			Map<TexasGameState, Float> m = p.getCurrentStack().getCommitedPerRound();
			String out = String.format("%s:, %s, %s, %s, %s, %s", p.getPlayerHandle(),
							m.getOrDefault(TexasGameState.PRE_DEAL, 0f) + m.getOrDefault(TexasGameState.POST_DEAL, 0f),
							m.getOrDefault(TexasGameState.FLOP, 0f),
							m.getOrDefault(TexasGameState.TURN, 0f),
							m.getOrDefault(TexasGameState.RIVER, 0f),
							p.getCurrentStack().getTotalBetInRound()
							);
			
			System.out.println(out);
		}


		System.out.println("-----------------");
		GamePots wins = tg.getFinalPots();
		for (SidePot sp : wins.getAllPots().values()) {
			String winners = sp.getPotWinners().stream().map(s->s.getPlayerHandle()).collect(Collectors.joining(", "));
			System.out.println("Winners = " + winners + ": " + sp.getPotTotal());
		}

		System.out.println("-----------------");
		for (Player p : players) {
			System.out.println("tg.getPlayers().getPlayerById(\"" + p.getPlayerId() +"\").setCards(Card.makeCards(\"\",\"\"));// " + p.getPlayerHandle());
		}
	}
	
	@Override
	public void dealCompleted(List<Card> tableCards, DealResult result) {
		tableCards.clear();
		tableCards.addAll(Card.makeCards("3D", "5S","5D", "6D","KC"));
	}
	
	/**
	 * 
	 * @param tg
	 */
	private void setPlayerCards(TexasHoldemGame tg) {
		tg.getPlayers().getPlayerById("102431723989847899748").setCards(Card.makeCards("7H","9C"));// Mike
		tg.getPlayers().getPlayerById("113768298782156590900").setCards(Card.makeCards("AH","AC"));// Benjamin
		tg.getPlayers().getPlayerById("106177318834976918346").setCards(Card.makeCards("4H","QS"));// Will
		tg.getPlayers().getPlayerById("107967180290224494922").setCards(Card.makeCards("2H","7D"));// euan
		tg.getPlayers().getPlayerById("103817767397532213309").setCards(Card.makeCards("TD","2S"));// Fabes
		tg.getPlayers().getPlayerById("10158234888009856").setCards(Card.makeCards("9S","4S"));// Paul
	}
	
	/** Used for creating player's from Action's
	 * 
	 */
	private class PAToPl implements Function<PlayerAction, Player> {
		@Override
		public Player apply(PlayerAction p) {
			
			Player pl = new Player(p.getPlayerId(), p.getPlayerHandle());
			pl.setSeatingPos(-1);
			pl.setSessionId(p.getPlayerId());
			pl.setEmail(p.getPlayerId());
			pl.getCurrentStack().initialise(10000, false);
			pl.getCurrentStack().reBuy(10000, 0);
			
			pl.setSessionId(p.getSessionId());
			return pl;
		}
		
	}

	private class LogMessaging implements Messaging {

		@Override
		public void sendPrivateMessage(String sessionId, Object payload) {
			
		}

		@Override
		public void sendPrivateMessage(String sessionId, String queue, Object payload, long delay) {
			
		}

		@Override
		public void sendBroadcastToTable(String gameId, Object message) {
			
		}

		@Override
		public void sendBroadcastToTable(String gameId, Object message, long delay) {
			
		}
		
	}


}
