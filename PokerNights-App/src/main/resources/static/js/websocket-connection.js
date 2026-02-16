/** This file manages the websocket connectivity with the server; Connection, Disconnect and re-connection
 * if there is a refresh of the page or temporary network outage
 * 
 * Copyright Langley Data Solutions Ltd 2020 - Present
 * 
 */
var socket = null;
var stompClient = null;
var ssState = {isConnected: false, attemptReconnect: false, eveningStarted: false};

const maxRetry = 25;// 25 * 2 = 50 seconds
const tableReconnectTimeout = 15 * 60 * 1000;// re-connect to table lasts 15 mins

/** Add event listeners to detect when offline/ online.
 * also establishes the user identity, gameId and reloads session */
$(function () {
    window.addEventListener('offline', function () {
    	socket.close();
    });
    window.addEventListener('online', function () {
    	if (!ssState.attemptReconnect) {
    		socket.close();
    		reConnectWebSocket();
    	}
    });
    
    var gameId = getGameId();
    ssState.eveningStarted = getCkJson('eveningStarted-' + gameId);
    if (!ssState.eveningStarted) {
    	ssState.eveningStarted = false;	
    }
    saveEveState();
    
    // Reload identity
    var cIdentity = getCkJson('identity-' + gameId);
    if (cIdentity) {
    	identity = jQuery.extend({}, cIdentity);
    	identity.sessionId = '';
    } else {
    	identity.gameId = gameId;
    }

    // Get session and token information and then connect...
    getSession(function() {
    	connectWebsocket();
    });
});

function getCkJson(key) {
	var v = Cookies.get(key, '');
	if (v) {
		return JSON.parse(v); 
	}
	return v;
}
/** Get the game's id from the address */
function getGameId() {
    var pathArray = window.location.pathname;
    return pathArray.substring(pathArray.lastIndexOf('/') + 1).replace('#','');
}

/** Get the websocket identity headers including the client authentication for 
 * securing the websocket.
 * If the accessToken is about to expire (or has) then a refresh is requested 
 * from the server after-which the callback is run.
 * 
 * @param callback The function to run after the headers have been set.
 */
function getSocketHeaders(callback) {
    var hdr = jQuery.extend({}, identity);
    
	// If our token is going to expire, request a refresh via the
	// session call to the server
    if (accessToken.expiresAt < (Date.now() - 30000)) {
        $.ajax({
            url: '/session',
            type: 'GET',
            success: function (data) {
        		if (data.tokens) {
        			accessToken = data.tokens.accessToken;
        			hdr['X-Authorization'] = 'Bearer ' + accessToken.token;
        			if (callback) {
        				callback(hdr);
        			}
        		} else {
        			accessToken = {accessToken: '', expiresAt: 0};
        			console.log('Websocket headers: No access token returned');
        		}
            }
        });
        
    } else {
    
	    hdr['X-Authorization'] = 'Bearer ' + accessToken.token;
	    if (callback) {
	    	callback(hdr);
	    }
    }
}

/** This is the first function called on loading the table page
 * which connects to the websocket and the broadcast chat for the specific
 * game.
 */
function connectWebsocket() {
	
	// Both of these have to be re-created in order for the connect() call
	// to actually work after a disconnect
	socket = new SockJS('/home-poker-websocket');
	stompClient = Stomp.over(this.socket);
	
    stompClient.onWebSocketClose = () => { stompClientOnclose(); reConnectWebSocket(); }
    stompClient.debug = () => {};// disable debug logging
    getSocketHeaders( function( headers ) {
        delete headers.sessionId;// Remove on initial connection as provided by server
        
        // Do the actual websocket connection
        stompClient.connect(headers, function (frame) {
        	ssState.isConnected = true;
        	ssState.attemptReconnect = false;
        	
            // Store this unique session id
        	if (socket._transport) {
	            identity.sessionId = /\/([^\/]+)\/websocket/.exec(socket._transport.url)[1];
	            saveIdentity();
	
	            // The user's private queue
	            subscribeToPrivateQueue();
        	} else {
        		console.log('No socket transport');
        	}
        }, function(error) {
        	if (!(error instanceof Object) && error.includes('Whoops! Lost connection')) {
        		reConnectWebSocket();
        	}
        });
    });


}

/** Perform a recursive re-connect to the server, pausing 2 seconds between each
 * attempt. Considered a failure after 30 seconds and requires a refresh */
async function reConnectWebSocket() {
	if (ssState.attemptReconnect) {
		return;
	}
	
	// Reset status to ensure reconnection to the correct topics
	// if a connection can be re-established
	ssState.attemptReconnect = true;
	ssState.isConnected = false;
	
	disablePlayerControls();
	var retry = 0;

	while (!ssState.isConnected && retry < maxRetry) {
		console.log('Attempting reconnect... (' + (retry+1) + ')');
		connectWebsocket();
		await sleep(2000);
		retry++;
	}
	
	if (!ssState.isConnected && retry >= maxRetry) {
		showTableConfirm('reconnect', 'Connection timeout',
				'Sorry, we failed to re-establish your session automatically. Would you like to reconnect to the game?',
				'Reconnect',
				function() {
					location.reload();
				});
	}
}

/** Pause for x milliseconds */
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/** complete disconnect from the game */
function disconnect() {
    if (stompClient !== null) {
    	stompClient.disconnect(function() {
    		ssState.isConnected = false;
    		ssState.eveningStarted = false;    		
    	}, identity);
    }
}

/** Save connection status to cookie */
function saveEveState() {
	var nRelogon = new Date(new Date().getTime() + tableReconnectTimeout);
	Cookies.set('eveningStarted-' + getGameId(), ssState.eveningStarted, { path: '', expires: nRelogon });
}