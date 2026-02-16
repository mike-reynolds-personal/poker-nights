package com.langleydata.homepoker.message;

/** A request by a player to change a game setting. This is a client to server message only,
 * with the response being a {@link SettingsUpdate} message
 * 
 *
 */
public class SettingChangeRequest {
	private String settingName;
	private Object settingValue;
	/**
	 * @return the settingName
	 */
	public String getSettingName() {
		return settingName;
	}
	/**
	 * @param settingName the settingName to set
	 */
	public void setSettingName(String settingName) {
		this.settingName = settingName;
	}
	/**
	 * @return the settingValue
	 */
	public Object getSettingValue() {
		return settingValue;
	}
	/**
	 * @param settingValue the settingValue to set
	 */
	public void setSettingValue(Object settingValue) {
		this.settingValue = settingValue;
	}
}
