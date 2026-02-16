/** Copyright Langley Data Solutions Ltd 2020 - Present */
const IDENTITY_TIMEOUT = 30 * 24 * 60 * 60 * 1000;// Identity lasts 30 days
const COOKIE_SCRIPT = 'https://cdn.jsdelivr.net/npm/js-cookie@rc/dist/js.cookie.min.js';
const SIGN_IN_PATH = '/signin';
const tableCookie = 'table-signin';
const WELCOME_MSG = 'Welcome to Poker Rooms Now';
// TODO: This regex needs to be more dynamic for the game type in the path...
const CHECK_TABLE_REGEX = /\/texas\/([A-z]|[0-9])*/;
const GET_AVATAR_URL = 'https://eu.ui-avatars.com/api/?background=random&uppercase=false&name=';

const AUTH_CONFIG = {
		issuer: '',
		clientId: '',
		redirectUri: '',
		tokenManager: {
			storage: 'sessionStorage',
			autoRenew: true
		},
		cookies: { secure: true }
};

var identity = {gameId: '', sessionId: '', playerId: '', playerHandle: '', playerEmail: '', fullName: '', accLevel: ''};
var accessToken = {accessToken: '', expiresAt: 0};
var profilePic;

$(function () {
	$.ajaxSetup({ cache: true});
	
	// Add default headers
	if (!isOnTable()) {
		$.get('./views/main/head.html', function(header) {
			$("head").append(header);
		 });
	}
	
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
	
    $("#signin-button").click(function() {
    	location.replace(SIGN_IN_PATH);
	});
    
    $("#logout-button").click(function() {
    	logout(true);
    });
    
    $.getScript(COOKIE_SCRIPT, function() {
    	
       	// Do a request to simply retrieve any user information.
       	// The table page calls getSession() itself
       	if (!isOnTable()) {
	       	getSession( function() {
	       		// if we have an id and handle, the user has a session
	       		if (identity.playerId && identity.playerHandle ) {
		        	$("#signin-button").hide();
		        	$("#logout-button").show();
		        	$("#greeting").text("Welcome back " + identity.playerHandle);
	       		} else {
	       			$("#greeting").text(WELCOME_MSG);
	       		}
	       	});
       	}
       	
    });

});

/** Are we on the game table page?
 * 
 * @returns True if we are
 */
function isOnTable() {
	return location.href.match(CHECK_TABLE_REGEX) != null && !location.href.includes('newGame');
}
/** Retrieve current session information from the server
 * 
 * @param connectToWebsocket If True, calls the connectWebsocket() function 
 * instead of enabling standard buttons
 */
function getSession(callback) {
/*
	var cName = 'identity';
	if (identity.gameId && identity.gameId != '') {
		cName = 'identity-' + identity.gameId;
	}
	if (Cookies.get(cName)) {
		var prevCookie = JSON.parse(Cookies.get(cName));
		if (prevCookie && prevCookie.playerHandle !='') {
			saveIdentity(prevCookie, function() {callback();} );
			return;
		}
	}
*/
	$.get( '/session', function(data) {
		if (!data) {
			console.log('No data returned by the session manager');
			return;
		}
		
		if (!data.tokens && isOnTable()) {
    		
			console.log('Access token not provided by server: Attempting session refresh...');
    		Cookies.set(tableCookie, location.href, { path: '/', expires: 1, sameSite: 'strict' });
    		location.replace(SIGN_IN_PATH);
    		return;
		} else if (data.tokens) {
			
			accessToken = data.tokens.accessToken;
			
			// If we've been redirected back to the home page to log-in, 
			// now direct back to the original table address 
			var prevTable = Cookies.get(tableCookie);
			if (!isOnTable() && prevTable) {
				Cookies.remove(tableCookie);
				location.replace(prevTable);
			}
			
		} else {
			accessToken = {accessToken: '', expiresAt: 0};
			Cookies.remove(tableCookie);
		}
		
		saveIdentity(data, function() {callback(data.roles);} );
	});
}


/** Logout of the current session
 * 
 * @param removeCookie Remove the identity cookie?
 */
function logout(removeCookie) {
	$("#signin-button").show();
	$("#logout-button").hide();
	
	if (removeCookie) {
		Cookies.remove('identity', {path:''});
		$.get('/signout');// Triggers the logout process and clearing JSESSION
		$("#greeting").text("You have now been logged out!");
	}
}

/** Save identity to the identity cookie
 * 
 * @param newDdata Any new identity data to save
 * @param callback A function to run when complete
 */
function saveIdentity(newData, callback) {
	if (newData && newData.playerId) {
		if (!identity.playerHandle) {
			identity.playerHandle = newData.playerHandle;
		}
		identity.playerId = newData.playerId;
		if (newData.email) {
			identity.playerEmail = newData.email;
		} else if (newData.playerEmail) {
			identity.playerEmail = newData.playerEmail;
		}
		identity.accLevel = newData.accLevel;
		identity.fullName = newData.fullName;
		if (newData.picture) {
			profilePic = newData.picture;
			identity.picture = profilePic;//so its available on the cached call
		} else {
			profilePic = GET_AVATAR_URL + identity.playerHandle;
		}
	}
	
	var nIdentity = new Date(new Date().getTime() + IDENTITY_TIMEOUT);
	var cName = 'identity';
	var path = '/';
	if (identity.gameId && identity.gameId != '') {
		cName = 'identity-' + identity.gameId;
		path = '';
	}
	// Cover-off a refresh of the page - Expires after tableLogonTimeout
	Cookies.set(cName, JSON.stringify(identity), { path: path, expires: nIdentity, sameSite: 'strict' });
	
	if (callback) {
		callback();
	}
}

function addHead() {
	$.get('./views/main/head.html', function(header) {
		$("head").append(header);
	 });
}