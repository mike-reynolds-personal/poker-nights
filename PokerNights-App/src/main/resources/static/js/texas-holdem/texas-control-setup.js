/** This file sets up the player's control buttons
 * 
 * Copyright Langley Data Solutions Ltd 2020 - Present
 * 
 */
function configureActionControls() {

    // Top menu controls
	if (gameSettings.format === 'TOURNAMENT') {
		$("#cashOptions").hide();
		$("#revealHand").hide();
		$("#postBlind").hide();
		$("#dealCards").removeClass('mr-sm-2');
		$("#foldHand").removeClass('mr-sm-2');
	} else {
		setupCashGameControls();
	}

	// Common control buttons
    $("#dealCards").click(function() { 
		sendToTable('startNextRound', false);
    });

    // Player actions...
    $( "#checkHand" ).click(function() { 
    	postPlayerAction('CHECK');
    });
    $( "#callHand" ).click(function() { 
    	postPlayerAction( this.innerText == 'Check' ? 'CHECK' : 'CALL');
    }); 
    $( "#placeBet" ).click(function() { 
    	var entered = $("#betAmountLabel").val();
    	if ( !isModOfAnte(entered)) {
    		return;
    	}
    	postPlayerAction( (this.innerText == 'All-In' ? 'ALL_IN' : 'BET'), entered );
    });
    $("#foldHand").click(function() { 
    	postPlayerAction('FOLD');
    }); 
	

    // Betting...
	$("#betAmountLabel").change(function() {
		var v = parseFloat( $(this).val() ).toFixed(2);
		$(this).val(v);
		$("#betSlider").slider({value: Math.round(v * 100)} );
	});
	
	// Other controls
    $("#sendChatMsg").click(function() {
    	sendChatMessage( $("#chatMsg").val() );
    });
    $("#sittingOut").click(function() { 
    	var isChecked = $("#sittingOut").is(":checked");
    	postPlayerAction('SIT_OUT', isChecked ? 1 : 0);
    });
}

/** Define the controls only used in cash games */
function setupCashGameControls() {
    $("#cashOut").click(function() {
		showTableConfirm('cashOut', 'Cash Out', 
				'Are you sure you want to cash out? This will transfer your balance and remove you from the table.', 
				'I\'m off',
				function() {
					$("#confirmModal").modal('hide');
					setTimeout(function() {
						postPlayerAction('CASH_OUT');
					}, 800);
				});
    });
    
    $("#buyIn").click(function() { 
    	postPlayerAction('RE_BUY');
    });
    
    // Reveal button
    $("#revealHand").click(function() { 
    	if (currentGame.gameState != 'COMPLETE') {
    		showTableConfirm('reveal', 'Reveal cards', 
    				'Revealing your cards now will fold your hand. Are you sure you want to continue?', 
    				'Do it!',
    				function() {
		    	    	postPlayerAction('REVEAL');
		    	    	$("#confirmModal").modal('hide');
    				});
    	} else {
	    	postPlayerAction('REVEAL');
    	}
    });
    
    // blinds
    $("#postBlind").click(function() { 
    	postPlayerAction('POST_BLIND');
    });
}
function restoreMenuSettings() {
	restoreMenuCheck('autoBlindCheck', controlState.autoBlind);
	restoreMenuCheck('soundEffectsCheck', controlState.soundEffects);
	restoreMenuCheck('emptySeatsCheck', controlState.hideEmptySeats);
	restoreMenuCheck('countdownSoundCheck', controlState.countdownSound);
}
function configurePlayerMenu() {

	$("#accInfo").click(function() {
		showAccInfo(true);
	});
	
	if (gameSettings.format === 'TOURNAMENT') {
		controlState.autoBlind = true;
		restoreMenuCheck('autoBlindCheck', true);
		$("#autoBlindMenu").addClass('disabled');
	} else {
	    $("#autoBlindMenu").click(function() { 
	    	var tick = $("#autoBlindCheck");
	    	var ticked = tick.is(':visible');
	    	restoreMenuCheck('autoBlindCheck', !ticked);
	    	if (!ticked) {
	    		if ($("#postBlind").is(':disabled')==false) {
	    			postPlayerAction('POST_BLIND');
	    		}
	    	}
	    	controlState.autoBlind = tick.is(':visible');
	    	saveControlState();
	    	return false;
	    });
	}
	
    $("#soundEffectsMenu").click(function() {
    	var tick = $("#soundEffectsCheck");
    	restoreMenuCheck('soundEffectsCheck', !tick.is(':visible'));
    	controlState.soundEffects = tick.is(':visible');
    	saveControlState();
    	return false;
    });
    $("#countdownSoundMenu").click(function() {
    	var tick = $("#countdownSoundCheck");
    	restoreMenuCheck('countdownSoundCheck', !tick.is(':visible'));
    	controlState.countdownSound = tick.is(':visible');
    	saveControlState();
    	return false;
    });
    $("#emptySeatsMenu").click(function() {
    	var tick = $("#emptySeatsCheck");
    	restoreMenuCheck('emptySeatsCheck', !tick.is(':visible'));
    	controlState.hideEmptySeats = tick.is(':visible');
    	saveControlState();
    	toggleSeatingDisplay( !controlState.hideEmptySeats );
    	return false;
    });
    $("#statsMenu").click(function() { 
    	sendToTable('myStats', null);
    });
    $("#helpMenu").click(function() {
    	$("#modalInstructions").modal('show');
    });
    $("#ranksMenu").click(function() { 
    	$("#modalRanks").modal('show');
    });
    $("#screenshotMenu").click(function() {
    	copyToImage('tableScreenArea');
    });
    $("#feedbackMenu").click(function() {
		$("#feedbackSubmit").prop('disabled', false);
		$("#modalFeedbackForm").modal('show');
    });
}
/** Configure all the host controls
 * 
 */
function configureHostControls() {
    $("#hostOptions").click(function() {
    	// Alloc wallets option
    	var awm = $("#assignWalletsMenu");
    	if (!(gameSettings.hostControlledWallet && gameSettings.moneyType == 'VIRTUAL')) {
    		awm.attr('hidden', true);
    	} else {
    		awm.attr('hidden', false);
        	if (getWOFA().length > 0) {
        		awm.removeClass('disabled');
        	} else {
        		awm.addClass('disabled');
        	}
    	}

    	// Eviction control
    	var toEvict = getActionOn();
    	if (toEvict != null && toEvict.playerId != identity.playerId) {
    		$("#evictPlayerMenu").removeClass('disabled');
    	} else {
    		$("#evictPlayerMenu").addClass('disabled');
    	}
    	
    	if (gameSettings.format === 'TOURNAMENT') {
    		 $("#doubleBlindsMenu").addClass('disabled');
    		 $("#completeGameMenu").addClass('disabled');
    	}
    });
    
    $("#evictPlayerMenu").click(function() {
    	var toEvict = getActionOn();
        if (!toEvict) {
        	return;
        }
    	if (toEvict.playerId === identity.playerId) {
    		receiveChatMessage('You cannot evict yourself!', true);
    		return;
    	}
        if (toEvict) {
    		showTableConfirm('cashoutPlayer:' +toEvict.sessionId , '<p>Evict the current player', 
    				'Are you sure you want to remove the current player from the game?</p>' +
    				'<p>If you proceed, ' + toEvict.playerHandle + ' will have 30 seconds to reject this action.</p>', 
    				'Evict player',
    				function() {
    					evictPlayer(toEvict, false);
    	    	    	$("#confirmModal").modal('hide');
    				});
        } else {
        	receiveChatMessage('Unable to get actionOn player from the game info', true);
        }

    });
    
    $("#transTransfer").click(function() {
    	let ta = $("#transAmount").val();
    	let tf =  $("#transFromPlayer").val();
    	let tt = $("#transToPlayer").val();
    	if (!ta || !tf || !tt || ta == '' || tf == '' || tt == '' || ta < 0) {
    		return false;
    	}
    	var transMsg = {
	    	'amount': parseFloat(ta),
	    	'fromId' : tf,
	    	'toId' : tt
    	};
    	postStackTransfer(transMsg);
    	$("#modalStackTransfer").modal('hide');
    });
    
    $("#transferStackMenu").click(function() {
		$("#modalStackTransfer").modal('show');
    	$(this).parent().toggleClass('show');
		let from = $("#transFromPlayer");
		let to = $("#transToPlayer");
		from.empty();to.empty();
		
	    $.each(currentGame.players, function(idx, player) {
			from.append(new Option(player.playerHandle, player.playerId));
			to.append(new Option(player.playerHandle, player.playerId));
		});
	    to.val('');from.val('');
    	return false;
    });
    
    if (gameSettings.format === 'CASH') {
	    $("#doubleBlindsMenu").click(function() {
	    	$(this).parent().toggleClass('show');
	    	sendToTable('increaseAnte', null);
	    	return false;
	    });
	    $("#completeGameMenu").click(function() { 
			showTableConfirm('completeGame', 'Complete the current game', 
					'Are you sure you want to complete the game and cash-out all players?', 
					'Yes',
					function() {
						sendToTable('completeGame');
		    	    	$("#confirmModal").modal('hide');
					});
	    });
    }
    
    // Set-up the Nudge delay menu option (Host)
    $("#nudegeDelayMenu").focusout(function() {
    	postSettingsUpdate('nudgeTime', parseInt( $(this).val() ) );
    	nudgeTimer.delay = $(this).val();
    });
}


/** Load all additional views to their div holders */
function loadAdditionalViews() {
	$("#feedbackForm").load('../views/feedback-form.html');
	$("#accInfoHolder").load('../views/main/account-info.html');
	
	$(".tableCards").load(viewPath + 'table-cards.html');
	$("#instructions").load(viewPath + 'instructions.html');
	$("#playerStats").load(viewPath + 'player-stats.html');
	$("#handRanks").load(viewPath + 'poker-ranks.html');
	$("#stackTransferForm").load(viewPath + 'stack-transfer.html');
	$("#allocateFunds").load(viewPath + 'allocate-funds.html');
	$("#getSeated").load(viewPath + 'get-seated.html');
	$(".playerChips").load(viewPath + 'player-chips.html');
	$(".playerInfo").load(viewPath + 'player-info.html', function() {
		// label the seating buttons and assign the click action
		var detail = $(this);
		var seatId = detail.attr('id').split('-')[2];
		detail.find('.take-seat-button').click(function() {
			$("#modalTakeSeat").modal('show');
			$("#playerHandle").val(identity.playerHandle.replaceAll(' ',''));
			$("#getSeatedBtn").attr('data-val', seatId);
		});
	});
}
/** Set a menu tick mark */
function restoreMenuCheck(check, restore) {
	if (!check) {
		return;
	}
	var tick = $("#" + check);
	if (restore) {
		tick.show();
		tick.addClass('checked');
	} else {
		tick.hide();
		tick.removeClass('checked');
	}
}