package com.langleydata.homepoker.persistence;

public final class ElasticSettings {
	private ElasticSettings() {}
	public static final String TEXAS_SETTINGS_IDX = "pn-settings";
	public static final String PLAYER_ACTION_IDX = "playeraction-history";
	public static final String TEXAS_ROUNDS_IDX = "round-history";
	public static final String ACCOUNT_IDX = "user-accounts";
	public static final String FEEDBACK_IDX = "player-feedback";
	
	public static final String[] INDEXES = {
			TEXAS_SETTINGS_IDX,
			PLAYER_ACTION_IDX,
			TEXAS_ROUNDS_IDX,
			ACCOUNT_IDX,
			FEEDBACK_IDX
	};
}
