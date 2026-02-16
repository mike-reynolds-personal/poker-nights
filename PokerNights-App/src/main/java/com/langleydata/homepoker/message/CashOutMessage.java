package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.PlayerStack;

/** This is a private message that tells the user what their balance is at
 * the end of a game.
 * 
 * @author Mike Reynolds
 *
 */
public class CashOutMessage extends PokerMessage {
	
	/** Create a new Cash-out message
	 * 
	 * @param player
	 * @param gameSettings
	 */
	public CashOutMessage(final Player player, final GameSettings gameSettings) {
		super(MessageTypes.CASH_OUT, player.getSessionId());
		final PlayerStack stack = player.getCurrentStack();
		final String wallet = MessageUtils.formatMoney(stack.getWallet(), gameSettings);
		this.message = String.format("You have been cashed out of the game. Your final wallet was %s. (%s)", wallet, player.formatBalance(gameSettings));
	}
}
