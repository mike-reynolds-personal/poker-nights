package com.langleydata.homepoker.message;

import java.util.ArrayList;
import java.util.List;

import com.langleydata.homepoker.api.MessageTypes;

/** A one-way message for the host to allocate funds/ wallet values to players
 * 
 */
public class AllocateFundsMessage extends PokerMessage {

	private List<AllocateToPlayer> allocations = new ArrayList<>();
	
	/** Json constructor */
	AllocateFundsMessage() {
		super(MessageTypes.ALLOCATE_FUNDS, null);
	}
	
	/**
	 * 
	 * @param sessionId
	 */
	public AllocateFundsMessage(String sessionId) {
		super(MessageTypes.ALLOCATE_FUNDS, sessionId);
	}

	/**
	 * @return the allocations
	 */
	public List<AllocateToPlayer> getAllocations() {
		return allocations;
	}

	/**
	 * 
	 */
	public static class AllocateToPlayer {
		String playerId;
		float wallet;
		/**
		 * @return the playerId
		 */
		public String getPlayerId() {
			return playerId;
		}
		/**
		 * @return the wallet
		 */
		public float getWallet() {
			return wallet;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("AllocateToPlayer [playerId=");
			builder.append(playerId);
			builder.append(", wallet=");
			builder.append(wallet);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
}
