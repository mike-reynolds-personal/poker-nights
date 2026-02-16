package com.langleydata.homepoker.message;

import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.MessageTypes;

public class SettingsUpdate extends PokerMessage {
	final GameSettings settings;
	
	public SettingsUpdate(GameSettings settings) {
		super(MessageTypes.SETTINGS_UPDATE, null);
		this.settings = settings;
	}

	public SettingsUpdate(final String error) {
		super(MessageTypes.SETTINGS_UPDATE, null);
		setMessage(error);
		settings = null;
	}
	
	public GameSettings getSettings() {
		return this.settings;
	}
}
