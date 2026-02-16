package com.langleydata.homepoker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.langleydata.homepoker.game.PokerMathUtils;
import com.langleydata.homepoker.game.players.PlayerStack;

/** A class used to read and output summary information for an entire game,
 * based on the JSON available in the Admin interface (the Game History)
 * 
 * @author Mike Reynolds
 *
 */
public class GameHistoryReader {

	static final int BUY_IN = 10;

	public static void main(String[] args) throws JsonIOException, JsonSyntaxException, FileNotFoundException {
		new GameHistoryReader().run("C:/temp/home-rounds.json");
	}

	/**
	 * 
	 * @throws JsonIOException
	 * @throws JsonSyntaxException
	 * @throws FileNotFoundException
	 */
	private void run(final String file) throws JsonIOException, JsonSyntaxException, FileNotFoundException {
		final JsonArray root = JsonParser.parseReader(new FileReader(new File(file))).getAsJsonArray();
		Gson gson = new Gson();
		
		for (int r = 0; r < root.size(); r++) {
			final JsonObject round = root.get(r).getAsJsonObject();
			final int actualRound = round.getAsJsonPrimitive("roundNum").getAsInt();
			final JsonArray players = round.getAsJsonArray("players");
			final JsonObject pots = round.getAsJsonObject("gamePots").getAsJsonObject("allPots");
			
			// Get the value of all pots in round
			double totalPot = pots.entrySet().stream().map(e -> e.getValue().getAsJsonObject())
				.mapToDouble(o->o.get("potTotal").getAsDouble())
				.sum();
			
//			final String winName = pots.getAsJsonObject("Main Pot").getAsJsonArray("potWinners").get(0).getAsJsonObject().get("playerHandle").getAsString();
			
			float roundBal = 0;
			double totalCommits = 0;
			float inPlay = 0;
			if (actualRound==8) {
				int m=0;
				m++;
			}
			// Cycle players
			for (int p=0; p < players.size(); p++) {
				final JsonObject player = players.get(p).getAsJsonObject();
				final PlayerStack ps = gson.fromJson(  player.get("currentStack"), PlayerStack.class);

				// The total amount commit by player(s)
				final double committed = ps.getCommitedPerRound().values().stream().mapToDouble(Float::doubleValue).sum();
				final float calcWallet = PokerMathUtils.rd( ps.getStack() + ps.getWallet() + committed );
//				if ( (calcWallet % 10) > 0) {
//					System.out.println(player.get("playerHandle").getAsString() + ": " + calcWallet);
//				}
				
				// all the money in-play based on buy-ins
				inPlay += calcWallet > 0 ? BUY_IN : 0;
				roundBal += ps.getStack();
				
//				if (player.get("playerHandle").getAsString().equals(winName)) {
//					float bal = (float) (ps.getStack() - totalPot + committed);
//					System.out.println("Winner Balance: " + (bal-ps.getStack() ));
//				}
				totalCommits += committed;
				
			}
			final String discrep = Math.abs(PokerMathUtils.rd(totalPot - totalCommits)) > 0 ? "--- Discrepency (" + PokerMathUtils.rd(totalPot - totalCommits) + ") ---" : "";
			final String out = String.format("%s: GameBal=%s; Pot=%s; Commits=%s  %s", 
					actualRound,
					PokerMathUtils.rd(roundBal - inPlay),
					PokerMathUtils.rd(totalPot),
					PokerMathUtils.rd(totalCommits),
					discrep
					);
			System.out.println(out);
		}
		
	}
}
