/** This file manages the Texas Hold'em table client-server messaging, including
 * websocket subscriptions (Lobby and Private topics) including status updates to the
 * player and re-connection to subscriptions after network outages 
 * 
 * Copyright Langley Data Solutions Ltd 2020 - Present
 * 
 */
const USER_QUEUE = '/user/queue/private';
const TABLE_TOPIC = '/topic/texas/';
const SEND_TABLE = '/app/texas/';

const chatHead = '<img src="{chatHeadImg}" class="chat-head">';
const startRecMsg = '<div class="message-holder"><div class="bg-primary message-text-bg"><p class="message-receive-text">';
const endRecMsg = '</p></div>{chatHeadHolder}</div><p class="message-time">TTTT</p>';
const startSendMsg = '<div class="message-holder"><div class="ml-auto"><div class="bg-light message-text-bg"><p class="message-send-text">';
const endSendMsg = '</p></div><p class="message-time">TTTT</p></div>';
const DISCONNECT_MSG = 'You are currently disconnected from the server - Attempting to reconnect...';

/** Subscribe to the table topic for game updates.
 * This happens when using takes a seat at the table, or if re-connecting */
function subscribeToGameTableUpdates() {

	getSocketHeaders( function( sendIdent ) {
		sendIdent.to = 'table';
	    console.log('Subscribing to table...');
		stompClient.subscribe(TABLE_TOPIC + identity.gameId, function (tableMsg) {
	    	tableMessageRec(JSON.parse(tableMsg.body));
	    	saveIdentity();
	    }, sendIdent);
	});

}

/** Subscribe to a private queue for receiving direct messages from the 
 * server or other users. This happens on initial connection/ re-connection
 */
function subscribeToPrivateQueue() {

	getSocketHeaders( function( sendIdent ) {
	    sendIdent.to = 'private';
	    console.log('Subscribing to private messages...');
	    stompClient.subscribe(USER_QUEUE, function (privateMsg) {
	    	var jsonMsg = JSON.parse(privateMsg.body);
	    	privateMessageRec(jsonMsg);
	    	saveIdentity();
	    	
	    	// The subscribe message is only sent once, so use this
	    	// to connect to the table updates
	    	if ( jsonMsg.messageType == 'SUBSCRIBE') {
				subscribeToGameTableUpdates();
	    	}
	    	
	    }, sendIdent);
	});
}

/** The main function that all others go through to actually send data
 * to the server via the websocket on the table topic
 * 
 *  action
 *  toSend
 */
function sendToTable(action, toSend) {
	
	var path = SEND_TABLE + identity.gameId;
	if ( action.startsWith('/')) {
		path += action;
	} else {
		path += '/' + action;
	}
	
	if (!ssState.isConnected) {
		receiveChatMessage(DISCONNECT_MSG, true);
		return;
	}

	getSocketHeaders( function( headers ) {
		stompClient.send(path, headers, JSON.stringify(toSend) );
		saveIdentity();// Refresh our timeout cookies
	});
}

/** Send this player's action to the server
 * 
 *  action The name of the action
 *  betValue The bet or status value
 */
function postPlayerAction(action, betValue) {
	if (!action) {
		return;
	}
	if (!ssState.isConnected) {
		receiveChatMessage(DISCONNECT_MSG, true);
		return;
	}
	// Check a bet has been entered
	if ((action == 'RAISE' || action == 'BET') && betValue < 0) {
		receiveChatMessage('No bet has been provided!', true);
		return;
	}
	var json = 
	    {
	    	'playerId':  identity.playerId,
	    	'sessionId': identity.sessionId,
	    	'gameId': identity.gameId,
	    	'picture': profilePic,
	    	'playerHandle': identity.playerHandle, 
	    	'betValue': !betValue ? 0 : betValue,
	    	'action': action
	    };
	
	sendToTable('/player/action', json);
	saveIdentity();// Refresh cookie timeouts
}

/** Transfer funds from one player's stack to another's
 * 
 * transObj The transfer details
 */
function postStackTransfer(transObj) {
	if (!transObj) {
		return;
	}
	if (!ssState.isConnected) {
		receiveChatMessage(DISCONNECT_MSG, true);
		return;
	}
	
	if (!transObj.fromId || !transObj.toId) {
		console.log('Either fromId or toId not provided');
		return;
	}
	
	if (transObj.amount == 0) {
		return;
	}
	
	// add default values and send
	transObj['gameId'] = identity.gameId;
	transObj['playerId'] = identity.playerId;
	transObj['playerHandle'] = identity.playerHandle; 
	transObj['sessionId'] = identity.sessionId;
	transObj['action'] = 'TRANSFER_FUNDS';
	transObj['picture'] =  profilePic;
	sendToTable('/player/stackTransfer', transObj);
}

/** The host to allocate wallet values to the players
 * allocations An array of players
 */
function postFundAllocation(allocations) {
	if (!allocations) {
		return;
	}
	if (!ssState.isConnected) {
		receiveChatMessage(DISCONNECT_MSG, true);
		return;
	}
	
	var json = {
			'allocations': allocations,
	    	'picture': profilePic,
	    	'sessionId': identity.sessionId
			};
	
	sendToTable('/allocate', json);
}
/** Request a change to the game settings
 * 
 * setting The setting name - must match the variable in the settings class
 * value the value of the setting - must be of the correct data type for the setting
 */
function postSettingsUpdate(setting, value) {
	if (!setting || value === undefined) {
		return;
	}
	if (!ssState.isConnected) {
		receiveChatMessage(DISCONNECT_MSG, true);
		return;
	}
	
	var json = {
		'requestorId': identity.sessionId,
		'settingName': setting,
		'settingValue': value,
    	'picture': profilePic
		};
	sendToTable('/changeSetting', json);
}
/** Forcibly remove a player from the game
 * 
 * playerId The host playerId
 * isReject Is the player rejecting?
 */
function evictPlayer(player, isReject) {
	if (!player) {
		return;
	}
	if (!ssState.isConnected) {
		receiveChatMessage(DISCONNECT_MSG, true);
		return;
	}

	var json = 
    {
    	'toEvictId': player.sessionId,
    	'gameId': identity.gameId,
    	'action': 'EVICT_PLAYER',
    	'picture': profilePic,
    	'isReject': isReject
    };

	sendToTable('/player/eject', json);
}

/** add a new player to the table */
function addMeToTable(seatId, playerHandle, wallet) {
	if (!ssState.isConnected) {
		return;
	}
	
	// Player may have embelished their name
	identity.playerHandle = playerHandle;
	saveIdentity();
	
	var joinInfo = {
        	'playerId':  identity.playerId,
        	'sessionId': identity.sessionId,
        	'seatingPos': seatId,
        	'picture': profilePic,
        	'email': identity.playerEmail,
        	'playerHandle': identity.playerHandle, 
        	'currentStack': {
        		'stack': parseFloat(wallet)
        		}
        	};

	getSocketHeaders( function( headers ) {
		stompClient.send(SEND_TABLE + identity.gameId + '/addplayer', headers, JSON.stringify(joinInfo));	
	});
}


/** Received a private message, which happens when user joins the game or the player receives their cards
 * In the intial update the joinedUser maybe this user's ID which enables confirms we can subscribe
 * to the broadcast /table/topic.
 * <p>
 * The dealer will also receive private messages based on how the deal went.
 * 
 * privateMsg The message from the server
 * Async function
 */
async function privateMessageRec(privateMsg) {
	var showAsSystemMessage = false;
	var message = privateMsg.message;
	
	switch (privateMsg.messageType) {
	case 'EVICT_PLAYER':
		if (privateMsg.toEvictId === identity.sessionId) {
			showTableConfirm('evictReceived', 'You are being evicted', 
					'The host is attempting to evict you from the game!<p>' +
					' You have 30 seconds to reject this action.</p>', 
					'Stay in game',
					function() {
						evictPlayer(getPlayerById(identity.playerId), true);
		    	    	$("#confirmModal").modal('hide');
					});
		}
		break;
	case 'STATUS_MSG':
		if (privateMsg.hostTransfer) {
			$("#hostOptions").show();
		} else {
			$("#hostOptions").hide();
		}
		break;
	case 'CASH_OUT':
		showTableConfirm('cashout', 'Cashed Out',
				privateMsg.message,
				'OK',
				function() {location.reload();}
			);
		disablePlayerControls();
		toggleDealerControls(false, false);
		return;
	case 'SUBSCRIBE':
		// Update with the logged-in user's details and save
   		identity.playerId = privateMsg.playerId;
   		identity.playerHandle = privateMsg.playerHandle;
   		// Update the username display in case they've changed names
   		$("#userName").html(identity.playerHandle);
   		saveIdentity();// Save to include handle
    	break;
	case 'ANTE':
		// Blinds on me?
		if (controlState.autoBlind) {
			// don't bother showing the message
			return;
		}
		if (privateMsg.anteType == 'BIG') {
			message = 'Please post the Big blind';
		} else if (privateMsg.anteType == 'SMALL') {
			message = 'Please post the Small blind';
		}
		break;
	case 'PLAYER_ACTION':
		if (privateMsg.successful) {
			// Add any additional actions here (as required)
		} else {
			if (privateMsg.action == 'BET') {
				message = 'Invalid bet value';
			}
			showAsSystemMessage = true;
		}
		break;
	case 'STATS':
		showPlayerStats(privateMsg.stats);
		break;
	case 'CHAT':
		// A private chat message - drops through to message window update
		if (privateMsg.receiptSound) {
			playSound(privateMsg.receiptSound);
		}
		break;
	case 'GAME_UPDATE':
		showAsSystemMessage = true;
		if (privateMsg.currentGame) {
			updateGameStatus(privateMsg.currentGame, false, false);
		}
		break;
	case 'JOINER_PRIVATE':// User has joined a game table
		showAsSystemMessage = true;
		// This is triggered by the user 'taking a seat' being sent to the 
		// server and then the success response being returned
		if (!privateMsg.currentGame) {
			break;
		}
		if (privateMsg.successful) {
			controlState.amSatDown = true;
			updateViewOnJoin(privateMsg.player);
			
		    /* Update our initial game state. We do an immediate
			 * update here to refresh the display */
		    if (privateMsg.currentGame) {
		    	updateGameStatus(privateMsg.currentGame, false, privateMsg.reconnect);
		    }

		}
		break;
		
	case 'RECEIVE_CARDS':
		if (privateMsg.hand) {
			playSound('dealt_2_cards');
			await showMyCards(privateMsg.hand[0].code, privateMsg.hand[1].code);
		}
		
		break;
	default:
		// Any other message types?
		break;
	}
	
	// Always update our chat window if we have a message
	if (message) {
		receiveChatMessage(message, showAsSystemMessage, privateMsg.picture);
	}
}


/** Called when game related information is updated via a Broadcast message on the <b>/texas/topic</b>
 * One only receives messages here if you've joined the game
 * 
 * gameMsg:  A GameMessage which encapsulates the game and type of update.
 */
async function tableMessageRec(gameMsg) {
    if (!gameMsg) {
    	return;
    }
    
   	playSound(gameMsg.actionSound);
   	
    switch(gameMsg.messageType) {
		case 'JOINER': // A new player has joined
			if (gameMsg.player.playerId != identity.playerId) {
				var player = gameMsg.player;
				
				// Update our currentGame with the player so the data is immediately available
				if (jQuery.isEmptyObject(getPlayerById(player.playerId))) {
					currentGame.players.push(player);
				}
				stylePlayerStackAndChips(player);
				styleInfoContainer(
						$("#pi-container-" + player.seatingPos), 
						player.state.actionOnMe ? 'ACTION_ON' : 'NORMAL',
						player);
			}
			break
		case 'LEAVER':
			removePlayer(gameMsg.seatingPos);
			break;
			
		case 'CHAT':
			// If it didn't originate from us, update the chat window
			if (gameMsg.sessionId != identity.sessionId) {
				receiveChatMessage(gameMsg.message, gameMsg.sessionId == 'SYSTEM', gameMsg.picture);
			}
			return; // don't send to status window
    	case 'SHOW_CARDS':
    		// A player has shown their cards
    		if (gameMsg.cards.length == 2 ) {
	    		await showPlayerTableCards(gameMsg.seatingPos, gameMsg.cards[0].code,  gameMsg.cards[1].code, true, false);
    		}
    		break;
	    case 'COMPLETE_GAME':
	    	showPlayerStats(gameMsg.stats, true);
	    	resetSeating();
	    	break;
	    case 'SETTINGS_UPDATE':
	    	if (gameMsg.settings) {
	    		gameSettings = gameMsg.settings;
	        	nudgeTimer.delay = gameSettings.nudgeTime;
	    	}
	    	break;
	    case 'GAME_UPDATE':

	    	if (!gameMsg.currentGame) {
	    		// do we want to do something with the error message?
	    		if (dealer) {
	    			receiveChatMessage(gameMsg.message, true);
	    		}
	    		console.log('Game detail was missing: ' + gameMsg.message);
	    		return;
	    	}
	    	
	    	// Set our initial global status
	    	if (gameMsg.newGame) {
	    		ssState.eveningStarted = true;
	    	}
	    	
	    	updateGameStatus(gameMsg.currentGame, gameMsg.newGame, false);
	    	
		    if (currentGame.gameState == 'COMPLETE') {
		    	$("#dealCards").html("New&nbsp;Game");
		    	// If user joined late, and we've got here, then the evening has started
		    	ssState.eveningStarted = true;
		   		
				// An override to ensure the remaining player can recover the game
				if (currentGame.players.length == 1) {
					toggleDealerControls(true, true);
				}
		    } else {
		    	if (ssState.eveningStarted) {
		    		$("#dealCards").html("Deal");
		    	}
		    }
		    saveEveState();
		    break;
	}
    
	receiveStatusMessage(gameMsg);
}

/** Send a messsge to the server and display in Chat */
function sendChatMessage(theMsg, toSessionId) {
	if (!theMsg) {
		return;
	}
	var toSession = identity.sessionId;
	var msgQ = SEND_TABLE + identity.gameId + '/chat';
	
	if (toSessionId) {
		toSession = toSessionId;
		msgQ = SEND_TABLE + identity.gameId + '/privatechat';
	}
	
	var json = JSON.stringify(
	    	{
	    	'playerId':  identity.playerId,
	    	'sessionId': toSession,
	    	'playerHandle': identity.playerHandle, 
	    	'picture': profilePic,
	    	'message': theMsg
	    	});
	
	getSocketHeaders( function( headers ) {
		stompClient.send(msgQ, headers, json);	
	});
	
	if (theMsg == 'NUDGE') {
		return;
	}

	updateChatWindow( formatChatHead(startSendMsg, profilePic) + linkify(theMsg, 'text-dark') + endSendMsg.replace('TTTT', getDateTime()) );
	$("#chatMsg").val('');
}

function formatChatHead(msgPart, imgSrc) {
	var chatPic = chatHead.replace('{chatHeadImg}', imgSrc);
	return msgPart.replace('{chatHeadHolder}', imgSrc ? chatPic : '');
}
/** Add a received message to the chat window
 * 
 */
function receiveChatMessage(theMsg, asSystem, profilePic) {
	if (!theMsg) {
		return;
	}
	if (asSystem) {
		profilePic = '../../images/logo.png';
	}
	var end = formatChatHead(endRecMsg.replace('TTTT', getDateTime()), asSystem ? '../../images/logo.png' : profilePic);
	var start = asSystem ? startRecMsg.replace('bg-primary', 'bg-info') : startRecMsg;
	updateChatWindow( start + linkify(theMsg) + end );
}

/** A status update message */
function receiveStatusMessage(theMsg) {
	if (!theMsg.message || theMsg.messageType === 'CHAT') {
		return;
	}
	var message = theMsg.message;
	
	if (theMsg.playerAction) {
		// format the message nicer
		if (theMsg.playerAction.betValue) {
			message += ' (' + currencyFormat(theMsg.playerAction.betValue) + ')'
		}
	} else if (theMsg.dealResult && theMsg.dealResult.result === 'SUCCESS') {
		return;
	}
	
	var rStart = rStart = '<tr><td>';
	// change colour based on status
	if (theMsg.result && theMsg.result == 'GENERIC_ERROR') {
		rStart = '<tr><td style="color:red;">';
	} else if (message.match(/Round [0-9]+ complete/) != null) {
		rStart = '<tr><td style="color:white;">';
	}
	
	// Add the message
	$("#sysMessageWindow").append(rStart + getDateTime() + ' ' + message + '</td></tr>');
	
	 // Scroll to bottom
	var div = document.getElementById('sysMessgeArea');
	$("#sysMessgeArea").animate({
	      scrollTop: div.scrollHeight - div.clientHeight
	   }, 300);
}

/** Update the chat window with a new element (composed html) and scroll */
function updateChatWindow(newFullMsg) {
   var div = document.getElementById('chatWindow');
   var chat = $('#chatWindow');
   
   chat.append(newFullMsg);
   chat.animate({
      scrollTop: div.scrollHeight - div.clientHeight
   }, 500);
}
