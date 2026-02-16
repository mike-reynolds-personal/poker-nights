package com.langleydata.homepoker.exception;

public class GameSchedulingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -528335230593440560L;

	public GameSchedulingException(String message) {
		super(message);
	}
	public GameSchedulingException(String message, Exception e) {
		super(message, e);
	}
	public GameSchedulingException(Exception e) {
		super(e);
	}
}
