package com.langleydata.homepoker.game.texasHoldem;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PokerHand {

	NONE("Invalid"),
	ROYAL_FLUSH("Royal Flush"),
	FOUR_KIND("Four of a kind"),
	STRAIGHT_FLUSH("Straight Flush"),
	FULL_HOUSE("Full House"),
	FLUSH("Flush"),
	STRAIGHT("Straight"),
	THREE_KIND("Three of a kind"),
	TWO_PAIR("Two Pair"),
	PAIR("Pair"),
	HIGH_CARD("High card");
	
	
	private final String friendlyName;
	
	private PokerHand(String friendlyName) {
		this.friendlyName = friendlyName;
	}
	
	@JsonValue
	public String getFriendlyName() {
		return friendlyName;
	}
}
