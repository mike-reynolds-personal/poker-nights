package com.langleydata.homepoker.deck;

public enum CardNumber {
	TWO(2, "2"),
	THREE(3, "3"),
	FOUR(4, "4"),
	FIVE(5, "5"),
	SIX(6, "6"),
	SEVEN(7, "7"),
	EIGHT(8, "8"),
	NINE(9, "9"),
	TEN(10, "T"),
	JACK(11, "J"),
	QUEEN(12, "Q"),
	KING(13, "K"),
	ACE(14, "A");
	
	private final String face;
	private final int value;
	
	/** 
	 * 
	 * @param value The value of the card (2-14)
	 * @param face The face value code of the card (2-9, T, J, Q, K, A)
	 */
	private CardNumber(final int value, final String face) {
		this.face = face;
		this.value = value;
	}
	
	/** Create a number from the face value code (2-9, T, J, Q, K, A)
	 * 
	 * @param face
	 * @return
	 */
	public static CardNumber fromFace(final String face) {
		switch (face.toUpperCase()) {
		case "A":
			return ACE;
		case "K":
			return KING;
		case "Q":
			return QUEEN;
		case "J":
			return JACK;
		case "T":
			return TEN;
		default:
			return fromIndex(Integer.parseInt(face));
		}
		
	}
	
	/** Construct a card number from the index value
	 * 
	 * @param index (2-14)
	 * @return
	 */
	public static CardNumber fromIndex(final int index) {
		switch (index) {
		case 2:
			return TWO;
		case 3:
			return THREE;
		case 4:
			return FOUR;
		case 5:
			return FIVE;
		case 6:
			return SIX;
		case 7:
			return SEVEN;
		case 8:
			return EIGHT;
		case 9:
			return NINE;
		case 10:
			return TEN;
		case 11:
			return JACK;
		case 12:
			return QUEEN;
		case 13:
			return KING;
		case 14:
			return ACE;
		}
		
		return null;
	}
	/** Get the numeric value of the card (2-14)
	 * 
	 * @return
	 */
	public int getValue() {
		return this.value; 
	}
	
	/** Get the face value code
	 * 
	 * @return (2-9, T, J, Q, K, A)
	 */
	public String getFace() {
		return this.face;
	}
}
