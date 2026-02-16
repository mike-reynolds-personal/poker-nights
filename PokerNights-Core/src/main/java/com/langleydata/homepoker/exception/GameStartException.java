package com.langleydata.homepoker.exception;

public class GameStartException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;
	
	public GameStartException(String message) {
		super(message);
	}
}
