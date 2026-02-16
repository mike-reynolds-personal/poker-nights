package com.langleydata.homepoker.game.texasHoldem;

import org.springframework.data.annotation.PersistenceConstructor;

import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.player.PlayerInfo;

/** A 'Winner' is a player who has won one or more pots in a game and is a 
 * simplified view of a player with their winning hand. This class is generally 
 * used for providing the simplified view to the UI
 * 
 * @author Mike Reynolds
 *
 */
public class Winner extends PlayerInfo {
	private final String winRank;
	private final String rankedCards;
	private final int seatingPos;
	
	@PersistenceConstructor
	private Winner() {
		super(null, null);
		winRank=null;
		rankedCards=null;
		seatingPos = 0;
	}
	/**
	 * 
	 * @param player
	 */
	public Winner(Player player) {
		super(player.getPlayerId(), player.getPlayerHandle());
		if (player.getRankedHand()!=null) {
			winRank = player.getRankedHand().getRankName().getFriendlyName();
			
			StringBuffer sb = new StringBuffer();
			for (Card c : player.getRankedHand().getCards()) {
				sb.append(c.getCode()).append(", ");
			}
			rankedCards = sb.length() > 2 ?  sb.substring(0, sb.length()-2) : "";
		} else {
			winRank = null;
			rankedCards = null;
		}
		seatingPos = player.getSeatingPos();
	}

	/**
	 * @return the winRank
	 */
	public String getWinRank() {
		return winRank;
	}
	/**
	 * @return the rankedCards
	 */
	public String getRankedCards() {
		return rankedCards;
	}
	
	public int getSeatingPos() {
		return seatingPos;
	}
}