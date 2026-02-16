package com.langleydata.homepoker.exception;

public class InvalidPlayerException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	
	public InvalidPlayerException(final String message) {
		super(message);
	}
}
