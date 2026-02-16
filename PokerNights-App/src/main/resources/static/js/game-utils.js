const ONE_HOUR = 60*60*1000;
/** Get a player from the currentGame players
 * 
 * @param playerId The playerId
 * @returns The matching player, or an empty object
 */
function getPlayerById(playerId) {
	if (!currentGame || !currentGame.players) {
		return {};
	}
	let retPlayer = {};
	$.each(currentGame.players, function(idx, player){
    	if (player.playerId === playerId) {
    		retPlayer = player;
    	}
	});
    return retPlayer;
}
/** Get the player who the action is on
 * 
 * @returns The player or null
 */
function getActionOn() {
	let retPlayer = null;
	$.each(currentGame.players, function(idx, player) {
		if (player.state.actionOnMe) {
			retPlayer = player;
		}
	});
    return retPlayer;
}
/** Get all players waiting on fund allocation 
 * 
 * @returns Array of players
 */
function getWOFA() {
	var wofas = [];
	$.each(currentGame.players, function(idx, player) {
		if (player.currentStack.wofa) {
			wofas.push(player);
		}
	});
	return wofas;
}

/** Use a regex to replace all urls within the text with <a> links
 * 
 * @param text The text to replace
 * @param colClass The text-[] colour for the link
 */
function linkify(text, colClass) {
	if (!colClass) {
		colClass = 'text-white';
	}
    return text.replace(URL_CHECK_REGEX, function(url) {
    	var fName = url.replace('https://', '').replace('http://', '');
        return '<a class="' + colClass + ' font-weight-bold" href="' + url + '" target="_blank">' + fName + '</a>';
    });
}

/** Format a number as currency
 * 
 * @param num Must be a number, not text
 * @param includePound Add an English pound sign? 
 * @returns formatted to two decimal places
 */
function currencyFormat(num, includePound) {
	if (num) {
	  return (includePound ? '&pound;'  : '') + num.toFixed(2).replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
	} else {
		return (includePound ? '&pound;'  : '') + '0.00';
	}
}

/** Check if the value is divisible by the current Ante
 * 
 * @param value The value to check
 * @returns True if it is.
 */
function isModOfAnte(value) {
	return (value * 10) % (gameSettings.ante * 10) == 0;
}
/** Round a value to 2 decimal places.
 * 
 * @param value The value to round
 */
function round(value) {
	return Math.round(value * 100) / 100;
}

/** Format a date/time for the message window */
function getDateTime(time) {
	var dt = new Date();
	if (time) {
		dt = time;
	}
	var h = '' + dt.getHours();
	var m = '' + dt.getMinutes();
	var s = '' + dt.getSeconds();
	
	return (h.length == 1 ? '0' + h : h) + ':' + (m.length == 1 ? '0' + m : m) + ':' + (s.length == 1 ? '0' + s : s);
}
/** Get a formatted date/time string as the difference between d1 and d2 */
function getElapsed(d1, d2) {
	return getDateTime(new Date(d1 - d2 - ONE_HOUR ));//TODO This doesn't seem right for all timezones, to deduct an hour
}

function toUTC(d1) {
	return Date.UTC(d1.getFullYear(), d1.getMonth(), d1.getDate(), d1.getHours(), d1.getMinutes(), d1.getSeconds(), d1.getMilliseconds());
}