/** This file manages the Texas Hold'em table view (Players, Cards and Table)
 * 
 * Copyright Langley Data Solutions Ltd 2020 - Present
 * 
 */
var dealer = false;
var showDeck = false;
var lastCommCardIdx = 0;
var currentGame;
var nudgeTimer = {'playerId': '', 'timeout': null, 'delay': 60};

const PLAYER_CARD_IMG = '<img class="player-table-card" width="55px" src="/images/{cardImg}"/>';
const CARD_POPOVER_HTML = '<img class="player-card mr-2" id="popoverCard2" {code1Holder} /> <img class="player-card" id="popoverCard1" {code2Holder} />';
const POT_ROW_START = '<tr style="display:inherit;text-align:left;">';
const STATS_HEADER = '<tr><th style="text-align: left;padding-left:0px">Player</th><th>Played</th><th>Won</th><th>Lost</th><th>VPIP</th><th style="min-width:5rem;">Re-buys</th><th style="min-width: 5.8rem;">Balance</th></tr>';

// Set-up dynamic sizing of community cards
$(document).ready( function () {
	setCommCardWidth();
	window.addEventListener('resize', function(event) {
		setCommCardWidth();
	});
});

/** Populate the game information on screen
 * 
 * @param currentGame
 * @param isNewGame
 * @param isReconnect
 */
async function updateGameStatus(currentGame, isNewGame, isReconnect) {
	this.currentGame = currentGame;
	gameSettings = currentGame.settings;
	disablePlayerControls();
	
	// Hide or remove chips and stack first
    for (var i = 0; i < currentGame.players.length; i++) {
    	stylePlayerStackAndChips( currentGame.players[i] );
    }
	
	/* Updating the table first so the player controls are disabled
	 * whilst the cards are being laid */
    await updateTableView(isNewGame, isReconnect);
    await updatePlayerDisplay(currentGame.players, isNewGame, isReconnect);
}


/** Update all information related to the table
 * 
 * @param clearDown
 * @param isReconnect
 */
async function updateTableView(clearDown, isReconnect) {
	
	// Update the pot first
	updateCurrentPot(currentGame.currentPot);

	// If reconnecting, clear the table view first
	if (isReconnect) {
		clearCommunityCards();
		$("#pots-container").hide();
		$("#potWinners tr").remove();
	}
	
	// Lay cards on the table
	var table = currentGame.cardsOnTable;
	var newCards = 1;
	if (table && table.length > 0) {
		newCards = table.length - lastCommCardIdx;
	}
	
	if (clearDown) {
		clearCommunityCards();
		hideCurrentPot();
		controlState.potWasHidden = false;
	} else if (newCards > 0) {
		
	    for (var i = lastCommCardIdx; i < table.length; i++) {
	    	$("#commCard" + (i+1)).html( mkCommCard(table[i].code) );
	    	
	    	// Delay the cards being laid - Don't delay after last card
	    	// or if we're reconnecting (that would be annoying!)
	    	if (newCards > 1 && !isReconnect && i < 4) {
	    		await sleep(900);
	    	}
	    }
	    lastCommCardIdx = table.length;
	}
    
	// Show pot win information when provided in the game info, otherwise hide it
	if (currentGame.finalPots.pots && currentGame.finalPots.pots.length > 0 && $("#potWinners tr").length == 0) {
		showWinnerDisplay(currentGame.finalPots.pots);
	} else if (clearDown) {
		$("#pots-container").hide();
		$("#potWinners tr").remove();
	}
	
}

/** Show and hide the current pot and pot-stack based on the value passed */
function updateCurrentPot( potVal ) {

	if (potVal && potVal > 0 && !controlState.potWasHidden) {
		$("#currentPot").html('&pound;' + currencyFormat(potVal)).show();
		$(".chips1").show();
		var potInc = ((gameSettings.format === 'TOURNAMENT' ? gameSettings.openingStack : gameSettings.buyInAmount) * currentGame.players.length) / 4;
		
		if (potVal > potInc) {
			$(".chips2").show();
		}
		if (potVal > (potInc*2)) {
			$(".chips3").show();
		}
		if (potVal > (potInc*3)) {
			$(".chips4").show();
		}
	}
	if (currentGame.gameState == 'COMPLETE') {
		controlState.potWasHidden = true;
		setTimeout(function() { hideCurrentPot(); }, 5000);
	}
}

function hideCurrentPot() {
	$(".chips1").hide();
	$(".chips2").hide();
	$(".chips3").hide();
	$(".chips4").hide();
	$("#currentPot").hide();
}


/** Show the pot winner listing and highlight the winner bubble
 * 
 * @param pots The game pots
 */
function showWinnerDisplay(pots) {
	$("#potWinners").append('<tr><th colspan="5">Pot Winners</th></tr>');
	
	for (var j=0; j < pots.length; j++) {
	
		var pw = pots[j].winners;
		var potPerPlayer = currencyFormat( pots[j].potTotal / pw.length, true );
		var lastName;
		
		for (var i=0; i < pw.length; i++) {
			var dispName = '';
			var player = pw[i];
			if (pots[j].name != lastName) {
				dispName = pots[j].name + ':';
				lastName = pots[j].name;
			}
			var winRank = player.winRank;
			var cards = player.rankedCards;
			if (!winRank) {
				winRank = '--';
			}
			if (!cards) {
				cards = '--';
			}
			
			$("#potWinners").append(POT_ROW_START + '<td><b>' + dispName + '</b></td><td>' + player.playerHandle + '</td>' + 
					'<td>' + winRank + '</td><td>' + cards + '</td><td>' + potPerPlayer + '</td></tr>');
			
			/* If the winner leaves, don't style the container */
			var pRows = $("#player-info-" + player.seatingPos +" tr");
			if (pRows && pRows[0].cells[1].innerText.length > 0) {
				styleInfoContainer( $("#pi-container-" + player.seatingPos), 'WINNER', player);
			}
		}
	}

	$("#pots-container").show();
}

/** Update the view of each players at the table.
 * This covers the state and information that all players can see.
 */
function updatePlayerDisplay(players, isNewGame, isReconnect) {
    
    /* For each player in the current game, update their display on my screen...*/
	var actionOn = {player: null, container: null};
    for (var i = 0; i < players.length; i++) {
    	
    	var player = players[i];
    	var pId = player.playerId;
    	
		// Update my status and controls first
		if ( pId == identity.playerId ) { 
			updateAllMyControls(player, isNewGame, isReconnect);
		}
		
		// The main player information display
		// This player's info container
		var pContain = $("#pi-container-" + player.seatingPos);

		if (player.state.actionOnMe) {
			actionOn.player = player;
			actionOn.container = pContain;
		}
		
		// The styling of the info bubble
		if (isNewGame || !pContain.hasClass('bg-success')) {
			styleInfoContainer(pContain, player.state.actionOnMe ? 'ACTION_ON' : 'NORMAL', player);
		}
		
		// Player sitting out indicator
		styleSitOutIndicator(pContain, player, pId == identity.playerId);
		configurePlayerActions(player);
		
		// Show as dealer?
		var dealParent = pContain.find(".dealer-indicator").parent();
		if (player.state.dealer) {
			dealParent.show();
		} else {
			dealParent.hide();
		}
		
		// clear or show backs of cards
		if (player.state.folded || player.state.sittingOut) {
			clearPlayerTableCards(player.seatingPos, isNewGame);
		} else {
			showPlayerTableCards(player.seatingPos, '', '', false, isReconnect);
		}
    }

	clearTimeout(nudgeTimer.timeout);
	var oldPlayer = getPlayerById(nudgeTimer.playerId);
	$("#pi-container-" + oldPlayer.seatingPos).find(".virtual-nudge").hide();
	
	// The nudge button...
	if ( actionOn.player && controlState.amSatDown) {
		nudgeTimer.playerId = actionOn.player.playerId;
		if (actionOn.player.playerId != identity.playerId) {
			var newNudge = actionOn.container.find(".virtual-nudge");
			nudgeTimer.timeout = setTimeout( function() { newNudge.show(); }, nudgeTimer.delay * 1000);
		}
	}
}

/** Clear the cards from a specific position
 * 
 */
function clearPlayerTableCards(pPos, isNewGame) {
	var pCards = $("#player-cards-" + pPos + " tr");
	const pHolder = PLAYER_CARD_IMG.replace('{cardImg}', 'card_placeholder.png');
	
	if (pCards[0].cells[0].innerHTML.includes('card_back') == false ) {
		// If its not the card back, they have revealed, therefore delay the
		// clearing of the cards. If complete, leave them out
		if (currentGame.gameState != 'COMPLETE') {
			var delay = (isNewGame ? 0 : 10000);
			setTimeout( function() { 
				pCards[0].cells[0].innerHTML = pHolder;
				pCards[0].cells[1].innerHTML = pHolder;
				$("#player-cards-" + pPos ).popover('dispose');
			}, delay);
		}
		return;
	}
	
	// Simple fold
	pCards[0].cells[0].innerHTML = pHolder;
	pCards[0].cells[1].innerHTML = pHolder;
}

/** Clear down a players information */
function removePlayer(seatingPos) {
	
	var pContain = $("#pi-container-" + seatingPos);
	styleInfoContainer(pContain, 'REMOVED');
	
	var pRows = $("#player-info-" + seatingPos +" tr");
	pRows[0].cells[1].innerHTML = '';//handle
	pRows[2].cells[1].innerHTML = '';//table
	pRows[2].cells[2].innerHTML = '';//stack
	
	// Remove chips display
	$(".player-chips-" + seatingPos).find(".col").hide();

	// Remove the player's cards from the table
	var pCards = $("#player-cards-" + seatingPos + " tr");
	const pHolder = PLAYER_CARD_IMG.replace('{cardImg}', 'card_placeholder.png');
	pCards[0].cells[0].innerHTML = pHolder;
	pCards[0].cells[1].innerHTML = pHolder;
	// Destroy pop-up of cards
	$("#player-cards-" + pPos).popover('dispose')
}

/** Create the card-display (zoom) pop-over for a specific position. Should only be called for a reveal
 * 
 * @param pPos
 * @param cardCode1
 * @param cardCode2
 * @returns
 */
function configurePlayerCardPopover(pPos, cardCode1, cardCode2) {
	var hover = $("#player-cards-" + pPos);
	hover.popover('dispose');

	var toApply = CARD_POPOVER_HTML.replace('{code1Holder}', 'src="../images/' + cardCode1 + '.svg"')
					.replace('{code2Holder}', 'src="../images/' + cardCode2 + '.svg"');
	
	var place = 'auto';
	var offset = '0 0';
	if (pPos == 3 || pPos == 4 || pPos == 5) {
		place = 'left';
		//offset = '-100 -150';
	}
	hover.popover({
	      html: true,
	      trigger: 'hover',
	      container: 'body',
	      offset: offset,
	      placement: place,
	      content: function () { return toApply; }
	    });
}

/** Show a pair of cards on the table at a specific position
 * 
 * @param pPos The seating position
 * @param cardCode1 card 1 or null when showing card backs
 * @param cardCode2 card 2
 * @param reveal If true, show the cards supplied face-up
 * @param isReconnect Is the player reconnecting?
 * @param isWinningHand Is this the winning hand show?
 */
function showPlayerTableCards(pPos, cardCode1, cardCode2, reveal, isReconnect) {
	var pCards = $("#player-cards-"+pPos + " tr");
	
	if (reveal) {
		// Show the cards and configure zoom of them
		pCards[0].cells[0].innerHTML = PLAYER_CARD_IMG.replace('{cardImg}', cardCode1 + '.svg');
		pCards[0].cells[1].innerHTML = PLAYER_CARD_IMG.replace('{cardImg}', cardCode2 + '.svg');
		configurePlayerCardPopover(pPos, cardCode1, cardCode2);
		return;
	}
	
	var inGame = isReconnect && currentGame.gameState!='COMPLETE' && currentGame.gameState!='PRE_DEAL';
	//Display card backs if it's not us
	if (currentGame.gameState == 'POST_DEAL' || inGame) {
		if (pCards[0].cells[0].innerHTML.includes('card_back') == false ) {
			pCards[0].cells[0].innerHTML = PLAYER_CARD_IMG.replace('{cardImg}', 'card_back.svg');
			pCards[0].cells[1].innerHTML = PLAYER_CARD_IMG.replace('{cardImg}', 'card_back.svg');
		}
	} else if (currentGame.gameState == 'PRE_DEAL') {
		// Clear down as new game
		pCards[0].cells[0].innerHTML = PLAYER_CARD_IMG.replace('{cardImg}', 'card_placeholder.png');
		pCards[0].cells[1].innerHTML =PLAYER_CARD_IMG.replace('{cardImg}', 'card_placeholder.png');
		$("#player-cards-" + pPos ).popover('dispose');
	}
}

/** Populate and display the player stats pop-up
 * 
 * @param stats The stats to show
 * @param endOfGame Is it the final stats dialog?
 */
function showPlayerStats(stats, endOfGame) {
	if (!stats || stats.length == 0) {
		return;
	}
	
	const UP = '<i class="fas fa-angle-up pl-1" style="color:green;"></i>';
	const DOWN = '<i class="pl-1 fas fa-angle-down" style="color:red;"></i>';
	
	$("#modalStats").modal('show');

	var statsArea = $("#statsArea");
	$("#statsArea tr").remove();
	
	var toUse = [];
	var nameLen = 0;
	$.each(stats, function(idx, value) {
		value['playerHandle'] = idx;
		toUse.push(value);
		if (idx.length > nameLen) {
			nameLen = idx.length;
		}
	});
	
	var goWide = nameLen > 9;// make the modal wide?
	toUse.sort((a, b) => b.balance - a.balance);
	
	if (endOfGame) {
		$("#statsModalLabel").html('End of ' + $("#statsModalLabel").html());
	}
	
	// The header and fields to display...
	var showTransfer = endOfGame && gameSettings.hostControlledWallet && getPlayerById(identity.playerId).state.host;
	var showRank = endOfGame && gameSettings.format === 'TOURNAMENT';
	var head = STATS_HEADER;
	if (showRank) {
		head = head.replace('Re-buys','Rank');
	}
	if (showTransfer) {
		head = STATS_HEADER.replace('</tr>', '<th>Transfer</th></tr>');
		goWide = true;
	}
	statsArea.append(head);
	
	if (goWide) {
		$("#modalStats").find(".modal-dialog").addClass('modal-lg');
	}
	
	for (var j=0; j < toUse.length; j++) {
		var stat = toUse[j];
		var bI = stat.balance > 0 ? UP : DOWN;
		var trStart = '<tr><td style="text-align: left;padding-left:0px;">';
		if (stat.playerHandle === identity.playerHandle) {
			trStart = '<tr style="font-weight:bold;"><td style="text-align: left;padding-left:0px;">';
		}
		var line = trStart + stat.playerHandle + '</td>' +
				'<td>' + stat.handsPlayed + '</td>' +
				'<td>' + stat.won + '</td>' + 
				'<td>' + stat.lost + '</td>' +
				'<td>' + stat.vpip + '%</td>' +
				'<td>' + (showRank ? stat.rank : stat.rebuys) + '</td>' +
				'<td>' + currencyFormat(stat.balance, true) + bI + '</td>' +
				(showTransfer ? '<td>' + currencyFormat(stat.transfer, true) + '</td>' : '') +
				'</tr>';
						
		statsArea.append(line);
	}
}

/** Set community card size */
function setCommCardWidth() {
	const minWidth = 50;
	const totCards = 5;
	const aw = $(".comm-card-area").width();
	var conWidth = parseInt(aw / totCards);
	$(".comm-card-container").width(conWidth < minWidth ? minWidth : conWidth);
}

/** Clear the community cards */
function clearCommunityCards() {
	for (var i = 1; i < 6; i++) {
		$("#commCard" + i).html('');
	}
	lastCommCardIdx = 0;
}