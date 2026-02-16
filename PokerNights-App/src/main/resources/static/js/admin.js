/** Copyright Langley Data Solutions Ltd 2020 - Present */
const hisPath = '/admin/game/history/';
const setPath = '/admin/settings/';
const accPath = '/account/';
const feedPath = '/admin/feedback/';
const activePath = '/admin/activegames/';

const trStyle = 'style="display:inherit;white-space: nowrap;"';
const trStart = '<tr ' + trStyle + '>';
var paging = {
		settings: [0,0],
		feedback: [0,0],
		accounts: [0,0]
	};

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });

	addHead();
   	
   	// Stop race condition by loading the Cookies file here as well
    $.getScript(COOKIE_SCRIPT, function() {
		populateGames();
		populateAccounts();
		setupNavigation();
		populateActiveGames();
		populateFeedback();
		
    });

    setupTabs();
    $("#clearById").click(function() {
		clearGameTables();
    	populateGames();
    });
    $("#searchById").click(function() {
    	let gameId = $("#txtGameId").val();
    	if (gameId == '') {
    		return;
    	}
		clearGameTables();
    	$.get(setPath + '/' + gameId, function(data) {
    		displayGameSettings([data]);
    	});
		populateRoundHistory(gameId);
    	
    });
});
function clearGameTables() {
	$("#gameHistory tr").remove();
	$("#storedGames tr").remove();
	$("#actionHistory tr").remove();
}

function populateAccounts() {
	
	// The number of accounts
	$.get(accPath + '/count', function(data) {
		$("#accountsTab").html('Accounts (' + data.count + ')');
	});
	
	// Get the page of account detail
	$.get(accPath + '?page=' + paging.accounts[0] + '&pageSize=100', function(data) {
		if (!data) {
			return;
		}
		
		var table = $("#accountsTable");
		$("#accountsTable tr").remove();
		
		table.append(trStart + '<th>Player ID</th><th>Full Name</th><th>Email</th><th>Account</th><th>Roles</th><th></th>' + 
								'<th>Last Played</th><th>Played</th><th>Organised</th><th>Played (m)</th><th>Balance</th><th></th></tr>');
		
		data.sort((a, b) => (a.playerHandle > b.playerHandle) ? 1 : ((b.playerHandle > a.playerHandle) ? -1 : 0));
		paging.accounts[1] = data.length;
		
		// for each account...
		for (var j=0; j < data.length; j++) {
			var acc = data[j];
			var stat = acc.accStats;
			var roles = '';
			if (acc.roles) {
				roles = acc.roles.sort().join();
			}

			var row = trStart + 
				'<td>' + acc.playerId + '</td>' +
				'<td>' + acc.fullName + '</td>' +
				'<td class="small"><a href="mailto:"' + acc.email + '>' + acc.email + '</a></td>' +
				'<td>' + mkUpgradeAcc(acc) + '</td>' +
				'<td>' + roles + '</td>' +
				'<td>' + mkAddAdmin(acc) + '</td>' +
				'<td>' + formatDate(stat.lastPlayed) + '</td>' +
				'<td>' + (stat.gamesPlayedIn ? stat.gamesPlayedIn.length : 0) + '</td>' +
				'<td>' +  Object.keys(stat.organised).length + '</td>' +
				'<td>' +  new Date(stat.timePlayed * 1000).toISOString().substr(11, 8) + '</td>' +
				'<td>' +  formatMoney(stat.balance, true) + '</td>' +
				'<td style="display:none">' + mkDelete( acc.playerId, 'deleteAccountLink') + '</td>' +
				'<td>' + mkBlock( acc.playerId, acc.isBlocked ) + '</td>' +
				'</tr>';
			table.append(row);
		}
		
		addAccountsClickEvents();
	});
}

function populateFeedback() {
	
	$.get(feedPath + '/count', function(data) {
		$("#feedbackTab").html('Feedback (' + data.count + ')');
	});
	
	$.get(feedPath + '?page=' + paging.feedback[0] + '&pageSize=50', function(data) {
		if (!data) {
			return;
		}
		var table = $("#feedbackTable");
		$("#feedbackTable tr").remove();
		table.append(trStart + '<th>Sent</th><th>Player</th><th>Game</th><th>Subject</th><th>Round</th><th>Screenshot</th><th>Message</th><th></th></tr>');
		
		paging.feedback[1] = data.length;
		// for each account...
		for (var j=0; j < data.length; j++) {
			var feed = data[j];
			
			var row = trStart + 
				'<td class="small">' + formatDate(feed.created) + '</td>' +
				'<td class="small"><a href="mailto:"' + feed.playerEmail + '>' + feed.playerEmail + '</a></td>' +
				'<td class="small">' + feed.gameId + '</td>' +
				'<td class="small">' + feed.subject + '</td>' +
				'<td>' + feed.gameRound + '</td>' +
				'<td class="small">' + (feed.hasScreenshot ? feed.messageId : '') + '</td>' +
				'<td class="small" style="white-space: normal;text-align: left;">' + feed.body + '</td>' +
				'<td>' + mkDelete( feed.messageId, 'deleteFeedLink') + '</td>' +
				'</tr>';
			table.append(row);
		}

		addFeedbackClickEvents();
		
	});
}

function addFeedbackClickEvents() {
	$(".deleteFeedLink").off('click');
	$(".deleteFeedLink").click(function() {
		 var feedId = $(this).attr('id');
		 $.ajax({
			    url: feedPath + feedId,
			    type: 'DELETE',
			    success: function(result) {
			    	setTimeout( function() {populateFeedback();}, 1000);
			    }
		});
   });
}

function populateActiveGames() {
	
	// Get active game info
	$.get(activePath, function(data) {
		if (!data) {
			return;
		}
		var activeCount = 0;
		var table = $("#activeGamesTable");
		$("#activeGamesTable tr").remove();
		table.append(trStart + '<th>Server</th><th>Game Type</th><th>Num active</th><th>Active</th><th>URI</th></tr>');
		// for each account...
		for (var j=0; j < data.length; j++) {
			var gs = data[j];
			activeCount += gs.activeGames.length;
			var active = '<table class="table table-sm">';
			for (var p=0; p < gs.activeGames.length; p++) {
				var a = gs.activeGames[p];
				active += '<tr><td>' + a.gameFormat + '</td><td>' + a.players + '</td><td>' + a.gameId + '</td></tr>';
			}
			active += '</table>';
			
			var row = trStart + 
				'<td>' + gs.serverName + '</td>' +
				'<td class="small">' + gs.gameType + '</td>' +
				'<td>' + gs.activeGames.length + '</td>' +
				'<td class="small">' + active + '</td>' +
				'<td>' + gs.uri.replace('http://', '').replace(':8080','') + '</td>' +
				'</tr>';
			table.append(row);
		}

		$("#activeGamesTab").html('Active Games (' + activeCount + ')');
	});
}
function mkAddAdmin(acc) {
	return '<a href="#" class="changeToAdmin pl-1" id="' + acc.playerId + '">' +
	'<i class="fas fa-unlock"></i></a>';
}
function mkUpgradeAcc(acc) {
	return acc.accLevel.replace('_', ' ') +
	'<a href="#" class="changeAccLevelUp pl-1" id="' + acc.playerId + '" data-level="' + acc.accLevel + '">' +
	'<i class="fas fa-arrow-circle-up"></i></a>' +
	'<a href="#" class="changeAccLevelDown pl-1" id="' + acc.playerId + '" data-level="' + acc.accLevel + '">' +
	'<i class="fas fa-arrow-circle-down"></i></a>';
	
}
/** Populate the list of game settings */
function populateGames() {

	// Get the total count
	$.get(setPath + '/count', function(data) {
		$("#gamesTab").html('Stored Games (' + data.count + ')');
	});
	
	// Get current settings
	$.get(setPath + 'all?page=' + paging.settings[0] + '&pageSize=10', function(data) {
		if (!data) {
			return;
		}
		
		data.sort((a, b) => b.scheduledTime - a.scheduledTime);
		displayGameSettings(data);
	});
}

function displayGameSettings(data) {
	var stored = $("#storedGames");
	
	$("#storedGames tr").remove();
	
	stored.append(trStart + '<th></th><th></th><th></th><th>Scheduled</th><th>ID</th><th>Format</th><th>Organiser</th><th>Created</th><th>Ante</th><th>Money</th><th>Length (m)</th><th>Server</th><th>Archived</th><th></th></tr>');
	paging.settings[1] = data.length;
	
	// for each game...
	for (var j=0; j < data.length; j++) {
		var d = data[j];
	
		var row = trStart + 
			'<td>' + mkOpenGame(d.gameId) + '</td>' +
			'<td>' + mkSettings(d.gameId) + '</td>' +
			'<td>' + mkHistory(d.gameId, d.wasPlayed) + '</td>' +
			'<td>' + formatDate(d.scheduledTime) + '</td>' +
			'<td><a target="_blank" href="' + hisPath + d.gameId + '">History (raw)</a></td>' +
			'<td>' + d.format + '</td>' + 
			'<td class="small"><a href="mailto:"' + d.organiserEmail + '>' + d.organiserEmail + '</a></td>' +
			'<td>' + formatDate(d.createdTime) + '</td>' +
			'<td>' + formatMoney(d.ante) + '</td>' +
			'<td>' + d.moneyType + '</td>' +
			'<td>' + new Date(d.gameLength * 1000).toISOString().substr(11, 8) + '</td>' +
			'<td>' + d.serverName + '</td>' +
			'<td>' + d.isArchived + '</td>' +
			'<td>' + mkDelete(d.gameId, 'deleteGameLink') + '</td>' +
			'</tr>';
		stored.append(row);
	}
	
	// Do after everything populated
	addSettingsClickEvents();
	
}
/** When link clicked, populate the specific game's history
 * 
 * @param gameId
 */
function populateRoundHistory(gameId) {
	 $.get(hisPath + gameId, function(data){
		if (!data) {
			return;
		}
		$("#rhLabel").html('Round History (' + gameId + ') #' + data.length);
		var history = $("#gameHistory");
		
		$("#gameHistory tr").remove();
		history.append(trStart + '<th>#</th><th></th><th></th><th>State</th><th>Completed</th><th>Table Cards</th><th>Seed</th><th>Pots</th><th>Players</th></tr>');

		data.sort((a, b) => a.completeTime - b.completeTime);
		
		// for each game...
		for (var j=0; j < data.length; j++) {
			var d = data[j];
			var row = trStart +
				'<td><b>' + d.roundNum + '</b></td>' +
				'<td>' + mkRoundDetailFull(gameId, d.roundNum) + '</td>' + 
				'<td>' + mkActionsShow(gameId, d.roundNum) + '</td>' + 
				'<td>' + d.gameState + '</td>' + 
				'<td>' + formatDate(d.completeTime) + '</td>' + 
				'<td>' + d.tableCards + '</td>' +
				'<td class="small">' + d.shuffleSeed + '</td>' +
				'<td>' + formatGamePots(d.roundNum, d.gamePots.allPots) + '</td>' + 
				'<td>' + formatPlayers(gameId, d.roundNum, d.players) + '</td>' +
				'</tr>';
			history.append(row);
		}

		// Do after history has been populated
		 addRoundHistoryClickEvents();
	 });
}

function populateActionsHistory(gameId, round, player) {
	var path = hisPath + 'actions/' + gameId + '/';
	var title = 'Actions: ';
	var showRound = false;
	
	 if (player) {
		path += 'player/' + player;// not 100% good, but otherwise ambiguos method
		title += 'Player ' + player;
		showRound = true;
	 } else if (round) {
		path += round;
		title += '<a href="' + path + '" target="_blank">Round ' + round + "</a>";
	} else {
		$("#actionHistory tr").remove();
		return;
	}
	
	 $.get(path, function(data){
			if (!data) {
				return;
			}
			$("#paLabel").html(title);
			var history = $("#actionHistory");
			$("#actionHistory tr").remove();
			
			data.sort((a, b) => a.timestamp - b.timestamp);
			var header = trStart + '<th>Timestamp</th><th>Handle</th><th>State</th><th>Action</th><th>Value</th><th>Success</th><th>Message</th>';
			if (showRound) {
				header += '<th>Round</th>';
			}
			history.append(header + '</tr>');
			
			// for each action...
			for (var j=0; j < data.length; j++) {
				var d = data[j];
				
				var useStart = trStart;
				if (d.action == 'DEAL') {
					useStart = '<tr style="display:inherit;white-space: nowrap;font-weight: bold;">';
				}

				var row = useStart + 
					'<td>' + formatDate(d.timestamp, false, true) + '</td>' +
					'<td>' + d.playerHandle + '</td>' +
					'<td>' + d.gameState + '</td>' +
					'<td>' + d.action + '</td>' +
					'<td>' + d.betValue + '</td>' +
					'<td>' + d.successful + '</td>' +
					'<td>' + (d.message ? d.message : '') + '</td>';
				
				if (showRound) {
					row += '<td>' + d.round + '</td>';
				}
				history.append(row + '</tr>');
			}
	 });

}

/** Format the players table
 * 
 * @param gameId
 * @param round
 * @param players
 * @return The table
 */
function formatPlayers(gameId, round, players) {
	var pls = '<table class="inner-table"><tbody id="players">';
	pls += '<tr><th>Name</th><th>Stack</th><th>Commit</th><th>Cards</th><th></th></tr>';
	
	for (var j=0; j < players.length; j++) {
		var player = players[j];
		var cs = player.currentStack;
		var link = hisPath + gameId + '/' + round + '/' + player.playerId;
		
		// Total up the player commitment
		var committed = 0;
		var cpr = cs.commitedPerRound;
		if (cpr) {
			for (var rnd in cpr) {
				committed += cpr[rnd];
			}
		}
		
		pls += '<tr id="player-' + player.playerId + '">' + 
			'<td><a href="' + link +'" target="_blank">' + player.playerHandle + '</a></td>' + 
			'<td>' + formatMoney(cs.stack, false) + '</td>' +
			'<td>' + formatMoney(committed, false) + '</td>' +
			'<td>' + player.pastCards + '</td>' +
			'<td style="padding-left:8px">' + mkActionsShow(gameId, round, player.playerId) + '</td>' +
			'</tr>';
	}
	pls += '</tbody></table>';
	return pls;
}

/** Format the winners table
 * 
 * @param w
 * @return the table
 */
function formatWinners(w) {
	if (!w) {
		return '';
	}
	var wins = '<table class="inner-table"><tbody id="winners">';
	wins += '<tr><th>Win Rank</th><th>Name</th><th>Ranked Cards</th></tr>';
	
	for (var j=0; j < w.length; j++) {
		wins += '<tr id="winner-' + w[j].playerId + '">' + 
			'<td>' + w[j].winRank + '</td>' +
			'<td>' + w[j].playerHandle + '</td>' + 
			'<td>' + w[j].rankedCards + '</td>' +
			'</tr>';  
	}
	wins += '</tbody></table>';
	return wins;
}
/** Format the game pots table
 * 
 * @param round
 * @param allPots
 * @returns The table
 */
function formatGamePots(round, allPots) {
	var pots = '<table class="stats-table flex-fill"><tbody id="gamePots">';

	for(var key in allPots) {
		var v = allPots[key];
		var winners = '';
		for (var i=0; i < v.potWinners.length; i++) {
			winners += v.potWinners[i].playerHandle + ', ';
		}
		winners = winners.substring(0, winners.length-2);
		var rank = 'L-M-S';
		if (v.maxRank && v.maxRank.rankName) {
			rank = v.maxRank.rankName;
		}
		pots += '<tr id=' + round + '-' + key + '>' +
			'<td>' + key + '</td>' +
			'<td>' + formatMoney(v.potTotal) + '</td>' +
			'<td>' + rank + '</td>' +
			'<td>' + winners + '</td>' +
			'<tr>';
	}
	pots += '</tbody></table>';
	return pots;
}

function addAccountsClickEvents() {
	$(".changeAccLevelUp").off('click');
	$(".changeAccLevelDown").off('click');
	$(".changeToAdmin").off('click');
	$(".blockPlayer").off('click');
	
	$(".blockPlayer").click(function() {
		var userId = $(this).attr('id');
		//TODO Controller method to block
	});
	
	$(".changeAccLevelUp").click(function() {
		changeAccLevel($(this).attr('id'), $(this).attr('data-level'), true);
	});
	
	$(".changeAccLevelDown").click(function() {
		changeAccLevel($(this).attr('id'), $(this).attr('data-level'), false);
	});
	$(".changeToAdmin").click(function() {
		var userId = $(this).attr('id');
		 $.ajax({
			    url: accPath + userId + '/roles?newRoles=Player,Admin',
			    type: 'PUT',
			    success: function(result) {
			    	setTimeout( function() {populateAccounts();}, 1000);
			    }
		});
	});
	
	$(".deleteAccountLink").off('click');
	$(".deleteAccountLink").click(function() {
		 var accId = $(this).attr('id');
		 $.ajax({
			    url: accPath + accId,
			    type: 'DELETE',
			    success: function(result) {
			    	setTimeout( function() {populateAccounts();}, 1000);
			    }
		});
   });
}

/** Change a user's account level */
function changeAccLevel(userId, cLevel, up) {
	 var cTier = cLevel.match(/[0-9]/);
	 var nTier = parseInt(cTier) + (up ? 1 : -1);
	 var newLevel = cLevel.replace(cTier, nTier);
	 
	 $.ajax({
		    url: accPath + userId + '?newLevel=' + newLevel,
		    type: 'PUT',
		    success: function(result) {
		    	setTimeout( function() {populateAccounts();}, 1000);
		    }
	});
}

/** Replace the action click events
 * 
 */
function addSettingsClickEvents() {
	// delete settins links
	$(".deleteGameLink").off('click');
	$(".deleteGameLink").click(function() {
		 var gameId = $(this).attr('id');
		 $.ajax({
			    url: setPath + gameId,
			    type: 'DELETE',
			    success: function(result) {
			    	setTimeout( function() {populateGames();}, 1000);
			    }
		});
   });
	
	// history
	$(".historyLink").off('click');
	$(".historyLink").click(function() {
		$("#actionHistory tr").remove();
		var gameId = $(this).attr('id');
		populateRoundHistory(gameId);
	});

	//others
}
/** Add click events to the round history table  */
function addRoundHistoryClickEvents() {
	// player actions - round
	$(".roundActions").off('click');
	$(".roundActions").click(function() {
		var parts = $(this).attr('id').split("|");
		populateActionsHistory(parts[0], parts[1], parts[2]);
	});
}
function mkActionsShow(gameId, round, player) {
	var id = gameId + '|' + round;
	if (player) {
		id += '|' + player;
	}
	 return '<a id="' + id + '" href="#" class="roundActions" title="All player actions"><i class="fas fa-bolt"></i></a>';
}
function mkRoundDetailFull(gameId, round) {
	var link = hisPath + gameId + '/' + round;
	return '<a href="' + link + '" target="_blank" title="Round detail"><i class="fas fa-info-circle"></i></a>';
} 
function mkDelete(itemId, clazz) {
    return '<a id="' + itemId + '" href="#" class="' + clazz + '" title="Delete"><i class="fas fa-trash-alt"></i></a>';
}
function mkBlock(playerId, isBlocked) {
	return '<a id="' + playerId + '" href="#" class="blockPlayer" title="Delete"><i class="fas fa-ban"></i></a>';
}
function mkHistory(gameId, played) {
	var dis = played ? 'disabled' : '';
	return '<a id="' + gameId + '" href="#" class="historyLink ' + dis + '" title="Show round history"><i class="far fa-clock"></i></a>';
}
function mkSettings(gameId) {
	var link = setPath + gameId;
	return '<a href="' + link + '" target="_blank" title="Game settings"><i class="fas fa-cog"></i></a>';
}
function mkOpenGame(gameId) {
	return '<a href="/texas/' + gameId + '" target="_blank" title="Goto game"><i class="fas fa-external-link-square-alt"></i></a>';
	
}
function formatMoney(num, includePound) {
	if (num) {
	  return (includePound ? '&pound;'  : '') + num.toFixed(2).replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
	} else {
		return (includePound ? '&pound;'  : '') + '0.00';
	}
}
/** Format an epoch timestamp for display
 * 
 * @param dt The unix/ epoch timestamp
 * @param withSeconds Display the date and time with seconds?
 * @param timeOnly Only provide a time, including seconds
 * @returns A formatted date/time string
 */
function formatDate(dt, withSeconds, timeOnly) {
	if (!dt) {
		return '';
	}
	var myDate = new Date(dt);
	var formatted = '';
	
	if (timeOnly) {
		formatted = ('0' + myDate.getHours()).slice(-2) + ':' +
		('0' + myDate.getMinutes()).slice(-2)  + ':' + 
		('0' + myDate.getSeconds()).slice(-2);
	} else {
		formatted = myDate.getFullYear() + '-' +
			('0' + (myDate.getMonth() + 1)).slice(-2) + '-' +  
			('0' + myDate.getDate()).slice(-2) + ' ' +
			('0' + myDate.getHours()).slice(-2) + ':' +
			('0' + myDate.getMinutes()).slice(-2);
		if (withSeconds) {
			formatted + ':' + ('0' + myDate.getSeconds()).slice(-2);
		}
	}

	return formatted;
};

function gameCreated(game) {
	if (game.success) {
		$(".modal").modal('hide');
		setTimeout( function() {populateGames()}, 2500);
	}
}
function setupNavigation() {
	// should be reduce to parameter based setup for each
    $("#setPagePrev").click(function() {
    	if (paging.settings[0]==0) {
    		return;
    	}
    	paging.settings[0]--;
    	populateGames();
    });
    $("#setPageNext").click(function() {
    	if (paging.settings[1]==0) {
    		return;
    	}
    	paging.settings[0]++;
    	populateGames();
    });
    $("#accPagePrev").click(function() {
    	if (paging.accounts[0]==0) {
    		return;
    	}
    	paging.accounts[0]--;
    	populateAccounts();
    });
    $("#accPageNext").click(function() {
    	if (paging.accounts[1]==0) {
    		return;
    	}
    	paging.accounts[0]++;
    	populateAccounts();
    });
    $("#feedPagePrev").click(function() {
    	if (paging.feedback[0]==0) {
    		return;
    	}
    	paging.feedback[0]--;
    	populateFeedback();
    });
    $("#feedPageNext").click(function() {
    	if (paging.feedback[1]==0) {
    		return;
    	}
    	paging.feedback[0]++;
    	populateFeedback();
    });
}
function setupTabs() {
	// should be reduce to parameter based setup for each
	$("#accountsView").hide();
	$("#activeGamesView").hide();
	$("#feedbackView").hide();
	
	$("#gamesTab").click(function() {
		$("#gamesTab").addClass('active');
		$("#accountsTab").removeClass('active');
		$("#activeGamesTab").removeClass('active');
		$("#feedbackTab").removeClass('active');
		
		$("#gamesView").show();
		$("#accountsView").hide();
		$("#activeGamesView").hide();
		$("#feedbackView").hide();
	});
	$("#accountsTab").click(function() {
		$("#gamesTab").removeClass('active');
		$("#accountsTab").addClass('active');
		$("#activeGamesTab").removeClass('active');
		$("#feedbackTab").removeClass('active');
		
		$("#gamesView").hide();
		$("#accountsView").show();
		$("#activeGamesView").hide();
		$("#feedbackView").hide();
	});
	$("#activeGamesTab").click(function() {
		$("#gamesTab").removeClass('active');
		$("#accountsTab").removeClass('active');
		$("#activeGamesTab").addClass('active');
		$("#feedbackTab").removeClass('active');
		
		$("#gamesView").hide();
		$("#accountsView").hide();
		$("#activeGamesView").show();
		$("#feedbackView").hide();
		populateActiveGames();
	});
	$("#feedbackTab").click(function() {
		$("#gamesTab").removeClass('active');
		$("#accountsTab").removeClass('active');
		$("#activeGamesTab").removeClass('active');
		$("#feedbackTab").addClass('active');
		
		$("#gamesView").hide();
		$("#accountsView").hide();
		$("#activeGamesView").hide();
		$("#feedbackView").show();
		populateFeedback();
	});
}