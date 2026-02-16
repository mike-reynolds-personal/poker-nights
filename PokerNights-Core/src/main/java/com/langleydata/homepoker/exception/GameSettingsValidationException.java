package com.langleydata.homepoker.exception;

public class GameSettingsValidationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -528335230593440560L;

	public GameSettingsValidationException(String message) {
		super(message);
	}
	public GameSettingsValidationException(String message, Exception e) {
		super(message, e);
	}
	public GameSettingsValidationException(Exception e) {
		super(e);
	}
}
