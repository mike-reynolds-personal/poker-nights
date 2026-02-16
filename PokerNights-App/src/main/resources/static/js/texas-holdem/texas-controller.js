/** This file manages all aspects of the Texas Hold'em table controls/ functionality
 * 
 * Copyright Langley Data Solutions Ltd 2020 - Present
 * 
 */
var controlState = {
		autoBlind: true, 
		maxBet: 0, 
		soundEffects: true, 
		countdownSound: true,
		amSatDown: false,
		potWasHidden: false,
		hideEmptySeats: true
		};
const viewPath = '../views/texas-holdem/';
var gameSettings;
var wasBigBlind = false;
var myCards = [];
let playerJoined;

/** Set-up of controls (on-load) */
$(function () {
	var gameId = getGameId();

	// Disable right-click
	$(document).on("contextmenu",function(e){
		e.preventDefault();
	});
	// Stop forms closing just by pressing the submit button
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
	
	
	
	// Reload control's state, if exists
	var cControlState = Cookies.get('controlState-' + gameId);		
    if (cControlState) {
    	var tControlState = JSON.parse(cControlState);
    	if (tControlState.hideEmptySeats == 'undefined') {
    		tControlState['hideEmptySeats'] = true;
    	}
    	controlState = tControlState;
    }
	
	// Immediately get this game's settings to help configure controls
	$.get("/texas/" + gameId + "/settings",  function (data, textStatus, jqXHR) {
		if (data) {
			gameSettings = data;
			
			loadAdditionalViews();
			configureActionControls();
		    configurePlayerMenu();

	    	nudgeTimer.delay = gameSettings.nudgeTime;
	        saveControlState();
	        
	        // Set-up the host options
	        configureHostControls();
	        $("#nudegeDelayMenu").val(nudgeTimer.delay);
		} else {
			alert("Game settings not loaded - Please refresh the page");
		}
	});

	setSliderVals(0, 10, 1, true);
	
	// When everything loaded, update any persisted state info and connect
	$(document).ready( function () {
		restoreMenuSettings();
	});
});


function toggleSeatingDisplay(show) {
	var seated = [];
	if (currentGame && currentGame.players) {
		$.each(currentGame.players, function(idx, ply) {
			seated.push(ply.seatingPos);
		});
	}
	$('[id^=pi-container-]').each(function() {
		var thisSeat = parseInt( $(this).attr('id').split('-')[2] );
		if ( $.inArray(thisSeat, seated)==-1) {
			if (show) {
				$(this).removeClass('hide-info');
			} else {
				$(this).addClass('hide-info');
			}
		}
	});

}

/** Display all player seats and 'Take Seat' button
 * 
 */
function resetSeating() {
	if (gameSettings.format === 'CASH') {
		$(".seatingButtons").show();
	}
	if (controlState.hideEmptySeats) {
		for (var i=0; i < 9; i++) {
			$("#pi-container-" + i).removeClass('hide-info');
		}
	}
}

/** The current player has joined the game */
function updateViewOnJoin(player) {
	
	$(".seatingButtons").hide();
	toggleSeatingDisplay(!controlState.hideEmptySeats);
	
	if (player.state.host) {
		$("#hostOptions").show();
	} else {
		$("#hostOptions").hide();
	}
    // Set initial state
    $("#cashOut").prop("disabled", false);
    $("#sittingOut").prop("disabled", false);
	$("#dealerControls").removeClass('invisible');
	$("#playerMenu").show();
	
	playerJoined = new Date(player.state.joinTime);
	
	// Timer displays
	$('#timeInGame').show();
	if (gameSettings.format === 'TOURNAMENT') {
		$("#timeBlindUp").show();
	}
	if (gameSettings.actionTimeout > 0) {
		let timer = $("#timeTakeAction");
		let tTop = gameSettings.format === 'TOURNAMENT' ? '165px' : '135px';
		timer.css({top: tTop})
		timer.show();
	} else {
		$("#timeTakeAction").hide();
	}
}

/** All in-game timers on a 1 second interval */
setInterval(function() {
	if (!playerJoined) {
		return;
	}
	// Incremental in-game  clock
    $('#timeInGame').html( 'In-game: ' + getElapsed(new Date(), playerJoined) );
    
    // Tournament Blinds timer
    if (gameSettings.format === 'TOURNAMENT') {
    	countdownTimer($("#timeBlindUp"), currentGame.blindIncreaseAt, 'Blinds increase: ');
    }
    
    // Take-action timer
    if (gameSettings.actionTimeout > 0) {
    	var actionOn = getActionOn();
    	var timer = $("#timeTakeAction");
    	if (actionOn && actionOn.playerId === identity.playerId) {
    		countdownTimer(timer, actionOn.state.nextAutoInaction, 'Take action by: ');
    	} else {
    		timer.html('Take action by: --:--');
    		timer.removeClass('timer-alarm');
    	}
    }
    
}, 1000);

/** Generic functions for countdown timers */
function countdownTimer(ctrl, time, prefix) {
	var disp = '--:--';
	if (time > Date.now()) {
		let diff = new Date(time - Date.now());
		disp = diff.toISOString().substr(14, 5);
		if (currentGame.gameState != 'COMPLETE') {
			let sec = parseInt(disp.substr(3,2));
			if (sec < 10) {
				ctrl.addClass('timer-alarm');
				if (sec == 5 && controlState.countdownSound) {
					playSound('5_sec_countdown');
				}
			} else {
				ctrl.removeClass('timer-alarm');
			}
		}
	}
	ctrl.html(prefix + disp );
}

/** Show a confirmation pop-up over the table
 * 
 * @param action A name for the action being performed
 * @param title The dialog title
 * @param body The dialog body text
 * @param btnText The confirmation button text
 * @param onConfirm A function to run when the confirm button is clicked
 * @param onHide Any callback when the dialog is hidden again
 */
function showTableConfirm(action, title, body, btnText, onConfirm, onHide) {
	var btn = $("#confirmModalButton");
	btn.data('action', action);
	btn.data('title', title);
	btn.data('body', body);
	btn.data('btnText', btnText);

	var cm =  $('#confirmModal');
    cm.on('show.bs.modal', function (event) {
    	var btn = $("#confirmModalButton");
    	$("#confirmModalTitle").html( btn.data('title') );
   		$("#confirmModalBody").html( btn.data('body') );
    	btn.html( btn.data('btnText') );
    	btn.off('click');
    	if (onConfirm) {
    		btn.click(onConfirm);
    	} else {
    		btn.click(function() {
    			$("#confirmModal").modal('hide');
    		});
    	}
    });
    
    cm.on('hidden.bs.modal', function (event) {
    	if (onHide) {
    		onHide();
    	}
    });

    $("#confirmModal").modal('show');
}

/** Save the control status */
function saveControlState() {
	var gameId = gameSettings.gameId;
	if (!gameId) {
		gameId = getGameId();
	}
	Cookies.set('controlState-' + gameId, JSON.stringify(controlState) );
}

function refreshMyCards() {
	if (myCards.length==0) {
		return;
	}
	var p1 = $("#playerCard1");
	var p2 = $("#playerCard2");
	p1.attr('src', '../images/' + myCards[0] + '.svg').attr('title', myCards[0]);
	p2.attr('src', '../images/' + myCards[1] + '.svg').attr('title', myCards[1]);
}

function showMyCards(card1, card2) {
	myCards = [card1, card2];
	var p1 = $("#playerCard1");
	var p2 = $("#playerCard2");
	var area = $("#userCardsTable");
	
	//p1.hide();//p2.hide();
	var src1 = '../images/' + card1 + '.svg';
	var src2 = '../images/' + card2 + '.svg';

	p1.off().on('error',function() {
		console.log('Error showing card' + card1 +', attempting refresh');
		$(this).attr('src', src1);
	}).attr('src', src1).attr('title', card1);
	
	p2.off().on('error', function() {
		console.log('Error showing card' + card2 +', attempting refresh');
		$(this).attr('src', src2);
	}).attr('src', src2).attr('title', card2);
	
	p2.on('load', function() {
		p1.css({opacity: 1});
		p2.css({opacity: 1});
		area.animate({height: 145}, 800);
		p1.show();p2.show();
	});
}

function clearMyCards(isNewGame) {
	myCards = [];
	if (isNewGame) {
		$("#userCardsTable").animate({height: 10}, function() {
			$("#playerCard1").off().hide().attr('src', '../images/card_back.svg');
			$("#playerCard2").off().hide().attr('src', '../images/card_back.svg');
		});
	} else {
		// folded
		var p1 = $("#playerCard1").fadeTo('slow', 0.4);
		var p2 = $("#playerCard2").fadeTo('slow', 0.4);
	}
}

/** Dealer controls */
function toggleDealerControls(enable, amDealer) {
	dealer = amDealer;
	
	$("#dealCards") .removeClass('disabled')
					.prop('disabled', !enable)
					.removeClass(amDealer ? 'btn-outline-primary' : 'btn-primary')
					.addClass(amDealer ? 'btn-primary' : 'btn-outline-primary');
	
	// Ensure the dealer can't leave!
	$("#cashOut").removeClass('disabled')
				 .prop("disabled", amDealer);
};

/** Disable all player controls*/
function disablePlayerControls() {
	$(".playerControl").prop('disabled', true);
	$("#betSlider").slider( 'option', 'disabled', true );
}


/** Updates all of the buttons, states and menus for the player within this
 * browser. Subsequently calls updateMyActionControls() which deals with the 
 * action buttons when the action is on this player
 * 
 * @param playerInfo
 * @param isNewGame
 * @param isReconnect
 */
function updateAllMyControls(playerInfo, isNewGame, isReconnect) {
	
	pPos = playerInfo.seatingPos;

	// If we're re-connecting and am the dealer, reset the in-round flag
	if (isReconnect) {
		if (playerInfo.state.dealer) {
			dealer = true;
			toggleDealerControls(currentGame.gameState=='COMPLETE', true);
		}
		if (playerInfo.state.sittingOut || playerInfo.state.sonr) {
			 $("#sittingOut").prop('checked', true);
		}
	}
	
	// we're saying !dealer here as we are the dealer throughout the game, but we don't 
	// want the control enabled, therefore only enable on a change of state. 
	if (!dealer && playerInfo.state.dealer) {
		// If i wasn't the dealer, but am now, enable my buttons
		toggleDealerControls(true, true); 
	} else if (currentGame.gameState=='COMPLETE' && !playerInfo.state.dealer) {
		// I'm not the dealer
		toggleDealerControls(false, false);
	} else {
		// do nothing as already set-up
	}
	
	/** These actions are regardless of whether the action is on me... */
	if (currentGame.gameState=='COMPLETE') {
		$("#buyIn").prop('disabled', round(playerInfo.currentStack.stack) > 0 );
		$("#cashOut").prop('disabled', false);
		setSliderVals(playerInfo.currentStack.stack, true); //reset ready for next game
	} else {
		$("#cashOut").prop('disabled', true);
		$("#buyIn").prop('disabled', !gameSettings.buyInDuringGameAllowed );
	}

	// Reset states for new game
	if (isNewGame) {
		toggleDealerControls(false, playerInfo.state.dealer);
		wasBigBlind = false;
	}
	if (isNewGame || playerInfo.state.folded) {
		clearMyCards( isNewGame );
	} else {
		refreshMyCards();
	}
	
	// Set the Call/ check button dependent on whether I was the big-blind
	var requiredBet = round(currentGame.requiredBet);
    if (requiredBet == 0 || (wasBigBlind && requiredBet == currentGame.settings.bigBlind) ) {
    	$("#callHand").html("Check");
    } else {
    	$("#callHand").html("Call");
    }
	if (playerInfo.state.blindsDue == 'BIG') {
		wasBigBlind = true;
	}

	/** Only happens if the action is on me */
	updateMyActionControls(playerInfo);
	
}

/** Enable and disable my action control buttons according to our state when it is 
 * our tern (actionOn == true)
 * 
 * @param playerInfo
 */
function updateMyActionControls(playerInfo) {

	// If the action is on someone else disable the player controls
	// and exit
	if (!playerInfo.state.actionOnMe) {
		disablePlayerControls();
		// allow Reveal at end of game
		var disable = toggleFoldReveal(playerInfo.state.folded);
		if (disable) {
			$("#foldHand").html('Fold');
		} else {
			$("#foldHand").html('Muck');
		}
		return;
	}
	
	var canBet = round(playerInfo.currentStack.stack) > 0;
	var blindsDue = playerInfo.state.blindsDue != 'NONE';
	var outOfHand = playerInfo.state.folded || playerInfo.state.allIn || playerInfo.state.sittingOut;
	
	if (!canBet) {
		disablePlayerControls();
	} else if (outOfHand) {
		disablePlayerControls();
		if (!playerInfo.state.sittingOut) {
			$("#revealHand").prop('disabled', false); // can still reveal
		}
	} else {
		if (blindsDue) {
			if ( $("#autoBlindCheck").hasClass('checked') ) {
				// Theoretically this delay allows the new game to start and
				// the game-update messages to come in the correct order
				setTimeout(function() {
					postPlayerAction('POST_BLIND');
				}, 800);
				return;
			} else {
				disablePlayerControls();
				$("#postBlind").prop('disabled', false);
			}
		} else if (currentGame.gameState == 'COMPLETE') {
			disablePlayerControls();
			toggleFoldReveal( playerInfo.state.folded);
			setSliderVals( playerInfo.currentStack.stack, true);
		} else {
			$("#postBlind").prop('disabled', true);
			$("#callHand").prop('disabled', false);
			$("#placeBet").prop('disabled', !canBet);
			$("#betAmountLabel").prop('disabled', !canBet);
			$("#foldHand").prop('disabled', false);
			$("#revealHand").prop('disabled', false);
			setSliderVals(playerInfo.currentStack.stack + playerInfo.currentStack.onTable, !canBet);
		}
	}
	
	// Ensure we toggle the flag off in case we go around again
	if (wasBigBlind && playerInfo.state.lastAction == "POST_BLIND") {
		wasBigBlind = false;
	}
	
	flashPlayerControls();

}

/** Enable or disable a player's fold and reveal buttons
 * 
 * @param actionOnMe
 * @param folded
 * @returns the state
 */
function toggleFoldReveal(folded) {
	var disable = currentGame.gameState != 'COMPLETE';
	
	if (folded || !ssState.eveningStarted) {
		disable = true;
		$("#revealHand").prop('disabled', disable);
		$("#foldHand").prop('disabled', disable);
		return disable;
	}

	$("#revealHand").prop('disabled', disable);
	$("#foldHand").prop('disabled', disable);
	return disable;
}

/** Set the betting slider values and increment
 * 
 * @param sMax
 * @param disable
 */
function setSliderVals(sMax, disable) {
	
	// The default is either the required bet, or what we have available, whichever is smaller
	var sMin = 0;
	var sStep = 0.2;
	
	// Default bet increments is the value of the big-blind
	if (gameSettings) {
		sStep = gameSettings.ante * 2;
		sMin = Math.min(currentGame.minRaise, sMax);
		
		if (gameSettings.potLimit == 'NO_LIMIT') {
			
		} else if (gameSettings.potLimit == 'POT_LIMIT') {
			var pot = currentGame.currentPot === undefined ? 0 : currentGame.currentPot;
			/* Maximum raise: The size of the pot, which is defined as the total of the active 
			 * pot plus all bets on the table plus the amount the active player must first call before raising.
			 */
			sMax = Math.min(pot + (round(currentGame.requiredBet) * 2), sMax);
			sStep = sMax;
		} else if (gameSettings.potLimit == 'LIMIT') {
			// LIMIT
			/* Pre-flop and on the flop, all bets and raises are of the same amount as the big blind.
			 * On the turn and the river, the size of all bets and raises doubles. */
			if (currentGame.gameState == 'TURN' || currentGame.gameState == 'RIVER') {
				sMax = Math.min(sMin + (sStep * 2), sMax);
			} else {
				// Set the maximum bet to the big-blind
				sMax = Math.min(sMin + sStep, sMax);
			}
		}
		
		controlState.maxBet = Math.round(sMax * 100) / 100;
		if (!Number.isInteger(sMax / sStep) && gameSettings.potLimit == 'NO_LIMIT') {
			// Ensure the slider covers everything in a user's stack
			sMax += sStep;
		}
	}
//TODO Revist slider and bets for non-no limit games
	
	$("#betAmountLabel").attr("step", sStep);
    $("#betSlider").slider({
    	disabled: disable,
        animate: true,
        value: Math.round(sMin * 100),
        min: Math.round(sMin * 100),
        max: Math.round(sMax * 100),
        step: Math.round(sStep * 100),
        slide: function(event, ui) {
        	updateBetValue(ui.value / 100, sMin, sMax); //changed
        }
    });
    updateBetValue(sMin, sMin, sMax);
}

function updateBetValue(val, min, max) {
   $( "#betAmountLabel" ).val( currencyFormat(val, false) ).attr('min', min).attr('max', max);
   
   let doAllin = false;
   if (gameSettings) {
	   doAllin = val >= controlState.maxBet && gameSettings.potLimit == 'NO_LIMIT';
   }
   
   $("#placeBet").html(doAllin ? 'All-In' : 'Bet');
}

function playSound(clip) {
	if (!clip || !controlState.soundEffects) {
		return;
	}
  let audio = new Audio('../audio/' + clip + '.mp3');
  var promise = audio.play();
  if (promise !== undefined) {
	  promise.then(_ => {
	  }).catch(error => {
		  // Just enough to catch it
	  });
	}
}

/** Configure the actions that can be performed on a player 
 * from their info-bubble
 * 
 * @param player
 */
function configurePlayerActions(player) {
	if (player.playerId == identity.playerId || !controlState.amSatDown) {
		return;
	}
	var pContain = $("#pi-container-" + player.seatingPos);
	var pm = pContain.find(".private-message");
	var vn = pContain.find(".virtual-nudge");
	
	pm.off("click");
	vn.off("click");
	
	// Private messsage
	pm.data('playerHandle', player.playerHandle).click(function() {
		var cm = $("#chatMsg");
		cm.val('@' +  $(this).data('playerHandle') + ' ');
		cm.focus();
	}).show();
	
	// Nudge click action
	vn.data('playerId', player.playerId).click(function() {
		var player = getPlayerById( $(this).data('playerId') );
		if (player) {
			sendChatMessage('NUDGE', player.sessionId);
		}
	});
}