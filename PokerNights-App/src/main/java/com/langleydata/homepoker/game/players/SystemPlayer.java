package com.langleydata.homepoker.game.players;

import org.springframework.stereotype.Component;

import com.langleydata.homepoker.player.PlayerInfo;

/** A player that represents the application. system. Used for sending messages
 * 
 */
@Component
public class SystemPlayer extends PlayerInfo {
	public static final String ID = "SYSTEM";
	public static final String HANDLE = "PokerRoomsNow.com";
	public static final String PICTURE = "/images/logo.png";
	
	/** Create a new player as the system user
	 * 
	 */
	public SystemPlayer() {
		super(ID, HANDLE);
		setPicture(PICTURE);
	}

}
