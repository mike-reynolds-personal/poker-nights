package com.langleydata.homepoker;

import java.util.Arrays;

import org.mockito.Mockito;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.message.PlayerActionMessage;

public class TestUtils {

	/** Post a sit-out action on a player
	 * 
	 * @param p The player
	 * @param state The current game state
	 * @param toggle True for on, false for off
	 * @return Whether the action was successful
	 */
	public static boolean doSitOut(Player p, TexasGameState state, boolean toggle) {
		PlayerAction sitOut = new PlayerActionMessage(p.getSessionId());
		sitOut.setAction(PlayerActionType.SIT_OUT);
		sitOut.setBetValue(toggle ? 1 : 0);
		p.doPlayerAction(sitOut, state, Mockito.mock(GameSettings.class), 0);
		return sitOut.isSuccessful();
	}
	/** Make a player, transferring all of the wallet value to the stack
	 * 
	 * @param id
	 * @param handle
	 * @param pos
	 * @param inWallet
	 * @return
	 */
	public static Player makePlayer(final String id, final String handle, final int pos, int inWallet) {
		Player p = new Player(id, handle);
		p.setSeatingPos(pos);
		p.setSessionId(id);
		p.setEmail(id);
		p.getCurrentStack().initialise(inWallet, false);
		p.getCurrentStack().reBuy(inWallet, 0);

		return p;
	}
	/**
	 * 
	 * @param name
	 * @param rounds
	 * @param rounds
	 * @param cards
	 * @return
	 */
	public static Player makePlayer(String name, TexasGameState[] rounds, float[] valPerRound, Card...cards) {
		// Should really use a mock
		Player user2 = makePlayer(name, name, -1, 500);

		user2.setCards( Arrays.asList(cards));
		
		for (int i=0; i < rounds.length; i++) {
			user2.getCurrentStack().addToTable(rounds[i], valPerRound[i]);
			user2.getCurrentStack().collectBets();
		}
		
		return user2;
	}
	/**
	 * 
	 * @param name
	 * @param rounds
	 * @param dealStack
	 * @param rankValue
	 * @return
	 */
	public static Player makePlayer(String name, TexasGameState[] rounds, float[] valPerRound, int rankValue) {
		// Should really use a mock
		Player user2 = makePlayer(name, name, -1, 500);
		user2.setRankedHand(new HandRank(rankValue, PokerHand.NONE, null));
		
		for (int i=0; i < rounds.length; i++) {
			user2.getCurrentStack().addToTable(rounds[i], valPerRound[i]);
			user2.getCurrentStack().collectBets();
		}
		
		return user2;
	}
	/** Create a player with a stack of 20, ready for adding to a game.<p>
	 * The adding to a game initialises the player with a stack of 10 and wallet of 10
	 * 
	 * @param id
	 * @param pos
	 * @return
	 */
	public static Player makePlayer(final String id, final int pos) {
		Player p = new Player(id, id);
		p.setSessionId(id);
		p.setSeatingPos(pos);
		p.setSessionId(id);
		p.setEmail(id);
		p.getCurrentStack().setStack(20f);

		return p;
	}
}
