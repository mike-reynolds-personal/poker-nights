package com.langleydata.homepoker.message;

import org.apache.commons.lang3.StringUtils;

import com.langleydata.homepoker.api.CardGame.DealResult;
import com.langleydata.homepoker.api.MessageTypes;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.game.players.SystemPlayer;

/**
 * A one-way message that only the dealer receives
 * 
 * @author reynolds_mj
 *
 */
public class StatusMessage extends PokerMessage {
	private final DealResult dealResult;
	private PlayerAction playerAction;
	private boolean isHostTransfer = false;

	/** Create a StatusMessage from the system user
	 * 
	 * @param message The system message to send
	 */
	public StatusMessage(final String message) {
		super(MessageTypes.STATUS_MSG, SystemPlayer.ID);
		setPicture(SystemPlayer.PICTURE);
		setMessage(message);
		dealResult = null;
	}
	/** Construct a message to inform the dealer of something
	 * 
	 * @param sessionId
	 * @param message
	 */
	public StatusMessage(final String sessionId, final String message) {
		super(MessageTypes.STATUS_MSG, sessionId);
		dealResult = null;
		setMessage(message);
	}
	/**
	 * Create a new message to inform the dealer of the result of dealing
	 * 
	 * @param sessionId
	 * @param dealResult
	 */
	public StatusMessage(String sessionId, final DealResult dealResult) {
		super(MessageTypes.STATUS_MSG, sessionId);
		this.dealResult = dealResult;
		if (dealResult!=null) {
			setMessage(dealResult.name());
		}
	}

	/**
	 * Create a new message to inform the dealer that a player performed an action
	 * 
	 * @param sessionId
	 * @param playerAction
	 */
	public StatusMessage(String sessionId, final PlayerActionMessage playerAction) {
		super(MessageTypes.STATUS_MSG, sessionId);
		this.dealResult = null;
		this.playerAction = playerAction;
		
		if (playerAction!=null) {
			if (StringUtils.isNotBlank(playerAction.message) && playerAction.getAction() != PlayerActionType.RE_BUY) {
				setMessage(playerAction.getPlayerHandle() + ": " + playerAction.message);
			} else {
				setMessage(playerAction.getPlayerHandle() + ": " + playerAction.getAction().getFriendlyMessage());
			}
		}
	}

	/**
	 * @return the dealResult
	 */
	public DealResult getDealResult() {
		return dealResult;
	}

	public boolean isHostTransfer() {
		return isHostTransfer;
	}
	public void setHostTransfer(boolean isHostTransfer) {
		this.isHostTransfer = isHostTransfer;
	}
	/**
	 * @return the playerAction
	 */
	public PlayerAction getPlayerAction() {
		return playerAction;
	}

}
