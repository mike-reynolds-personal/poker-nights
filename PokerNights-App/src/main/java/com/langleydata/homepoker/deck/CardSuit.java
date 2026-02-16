package com.langleydata.homepoker.deck;

public enum CardSuit {
	Spades(0x1000),
	Hearts(0x2000),
	Diamonds(0x4000),
	Clubs(0x8000);

	private final int value;

	/** Enum constructor
	 * 
	 * @param value
	 */
	private CardSuit(final int value) {
		this.value = value;
	}

	/** Get the binary value of the suit
	 * 
	 * @return
	 */
	public int getValue() {
		return this.value;
	}

	/** Create a suit from the String code 
	 * 
	 * @param face S, C, D or H
	 * @return A new Suit
	 */
	public static CardSuit fromFace(final String face) {
		if (face.length() > 1) {
			return CardSuit.valueOf(face);
		} else if (face.length() == 1) {
			switch (face.toUpperCase()) {
			case "S":
				return Spades;
			case "C":
				return Clubs;
			case "D":
				return Diamonds;
			case "H":
				return Hearts;
			}
		}
		return null;
	}

	/** Create a suit from the binary value
	 * 
	 * @param value
	 * @return
	 */
	public static CardSuit fromIndex(int value) {
		switch (value) {
		case 0x2000:
			return Hearts;
		case 0x8000:
			return Clubs;
		case 0x1000:
			return Spades;
		case 0x4000:
			return Diamonds;
		}
		return null;
	}
}
