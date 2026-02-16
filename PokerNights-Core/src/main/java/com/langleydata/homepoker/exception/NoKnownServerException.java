package com.langleydata.homepoker.exception;

public class NoKnownServerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5286733551593440560L;

	public NoKnownServerException(String message) {
		super(message);
	}
	public NoKnownServerException(String message, Exception e) {
		super(message, e);
	}
}
