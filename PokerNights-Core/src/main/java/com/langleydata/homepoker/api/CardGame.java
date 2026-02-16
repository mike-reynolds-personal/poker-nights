package com.langleydata.homepoker.api;

public interface CardGame {

	/**
	 */
	public enum GameUpdateType {
		NO_ACTION,
		UNRECOGNISED,
		UNSUCCESSFUL,
		SUCCESS,
		WAITING_ON_ACTION
	}
	
	/** The result of a deal */
	public enum DealResult {
		SUCCESS,
		GENERIC_ERROR,
		NOT_ENOUGH_PLAYERS,
		BLINDS_DUE,
		WAITING_ON_BETS,
		WAITING_BB_CHECK,
		AUTOCOMPLETING,
		NO_DEAL
	}
	
	/** Which game settings can be changed in game by the host? */
	public enum PermisibleSettingChangeFields {
		nudgeTime,
		ante
	}
	
	/** The format (game-style) of the game */
	public enum GameFormat {
		CASH,
		TOURNAMENT
	}
	
	/** The type of money being played/ bet */
	public enum MoneyType {
		VIRTUAL,
		REAL
	}
	
	/** The frequency of shuffles */
	public enum ShuffleOption {
		ALWAYS,
		NEVER
	}
}
