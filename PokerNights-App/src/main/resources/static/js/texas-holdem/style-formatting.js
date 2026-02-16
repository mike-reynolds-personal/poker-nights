 /** Copyright Langley Data Solutions Ltd 2020 - Present */
const SITTING_OUT_COLOUR = '#dc3545';
const WILL_SIT_OUT_COLOUR = '#6c757d';
const WINNER_CLASS = 'bg-success';
const WINNER_BORDER = 'darkgreen';
const ACTION_ON_CLASS = 'bg-info';
const ACTION_ON_BORDER = '#076c8a';
const STD_BORDER = 'slategrey';
const URL_CHECK_REGEX = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
const IMG_PATH = '/images/';

var lastFlashControlState;

$(function () {
	cacheAllPlayingCards();
});

function cacheAllPlayingCards() {
	
	let path = '/images/';
	let suits = ['S', 'C', 'H', 'D'];
	let pics = ['T', 'J', 'Q', 'K', 'A'];
	var cache = $("#cachedCards");
	
	for (var c = 2; c < 10; c++) {
		$(suits).each(function() {
			cache.append( mkCacheImage( c + this) );
		});
	}
	$(pics).each(function() {
		var s = this;
		$(suits).each(function() {
			cache.append( mkCacheImage( s + this) );
		});
	});
	
    console.log('Cached ' + $("#cachedCards > img").length + ' cards');
}
function mkCacheImage(code) {
	return $('<img />')
		.attr('src', IMG_PATH + code + '.svg')
		.attr('title', code)
		.on('error', function() {
			receiveStatusMessage({result: 'GENERIC_ERROR', message: 'Error caching card ' + code});
		});
}
function mkCommCard(code) {
	return $('<img />')
		.attr('src', IMG_PATH + code + '.svg')
		.attr('title', code)
		.addClass('community-card')
		.show()
		.on('error', function() {
			receiveChatMessage('Error showing card ' + table[i].code, true);
		});
}

/** Copy an element and all children to the clipboard as an image
 * 
 * @param elementId
 */
function copyToImage(elementId) {
	html2canvas(document.querySelector("#"+elementId), { 'removeContainer': true, 'useCORS': true })
		.then(canvas => canvas.toBlob( blob => navigator.clipboard.write([new ClipboardItem({'image/png': blob})])  ));
}

/** Style a player's information bubble
 * 
 * @param player
 */
function stylePlayerStackAndChips(player) {
	var pRows = $("#player-info-" + player.seatingPos +" tr");
	
	if (!pRows || !player.playerHandle || pRows.length==0) {
		return;
	}
	
	pRows[1].style.display = 'none';
	pRows[0].cells[1].innerHTML = player.playerHandle;
	var chips = $(".player-chips-" + player.seatingPos);
	
	if (player.currentStack.onTable > 0) {
		chips.find(".col").show();
		chips.find(".betValue").html(currencyFormat(player.currentStack.onTable, true));
	} else {
		chips.find(".col").hide();
	}
	pRows[2].cells[1].innerHTML = currencyFormat(player.currentStack.stack, true);
}

/** Style a player's sit-out indicator
 * 
 * @param pContain
 * @param player
 * @param isMyView
 */
function styleSitOutIndicator(pContain, player, isMyView) {
	var sitOutInd = pContain.find(".sit-out-indicator");
	var show = false;
	var colour = '';
	
	if (currentGame.gameState == 'COMPLETE' && player.state.sonr) {
		// we want to show the status immediately on completion of a round
		colour = SITTING_OUT_COLOUR;
		show = true;
	} else {
		if (isMyView) {
			// my indicator - Grey if next round, red if actually sat out
			if (player.state.sittingOut || player.state.sonr) {
				colour = player.state.sittingOut ? SITTING_OUT_COLOUR : WILL_SIT_OUT_COLOUR;
				show = true;
			}
		} else {
			if (player.state.sittingOut) {
				colour = SITTING_OUT_COLOUR;
				show = true;
			}
		}
	}
	if (show) {
		sitOutInd.css({'color': colour});
		sitOutInd.parent().show();
	} else {
		sitOutInd.parent().hide();
	}
}

/** Style a specific player's info container
 * 
 * @param container The specific container
 * @param style The style to apply, cancelling all others. One of 'NORMAL', 'WINNER', 'ACTION_ON'
 * @returns
 */
function styleInfoContainer(container, style, player) {
	if (!container) {
		return;
	}
	
	var header = container.find("th");
	var picHolder = container.find(".player-avatar");
	var border = STD_BORDER;
	
	container.removeClass(ACTION_ON_CLASS).removeClass(WINNER_CLASS);
	
	if (style != 'REMOVED' && style != 'HIDE') {
		container.removeClass('hide-info');
	}
	
	if (style == 'HIDE') {
		container.addClass('hide-info');
	} else if (style == 'NORMAL') {
		container.css("border-color", STD_BORDER);
		header.removeClass('text-white');
	} else if (style == 'WINNER') {
		container.addClass(WINNER_CLASS).css('border-color', WINNER_BORDER);
		header.addClass('text-white');
		border = WINNER_BORDER;
	} else if (style == 'ACTION_ON') {
		container.addClass(ACTION_ON_CLASS).css('border-color', ACTION_ON_BORDER);
		header.addClass('text-white');
		border = ACTION_ON_BORDER;
	} else if (style == 'REMOVED') {
		if (controlState.hideEmptySeats) {
			container.addClass('hide-info');
		}
		container.css("border-color", STD_BORDER);
		header.removeClass('text-white');
		container.find(".sit-out-indicator").parent().hide();
		container.find(".dealer-indicator").parent().hide();
		container.find(".private-message").hide().off('click');
		container.find(".virtual-nudge").hide().off('click');
		container.find(".player-avatar").hide();
	}
	
	if (player && style != 'REMOVED' && picHolder && player.picture) {
		picHolder.attr('src', player.picture).css("border-color", border).show();
	}
	
}

/** Animate the player control background */
function flashPlayerControls() {
	if (lastFlashControlState != currentGame.gameState) {
	    $("#playerControlBar").animate({'background-color': '#17a2b8'}, 1000, function() {
	    	$("#playerControlBar").animate({'background-color': '#292b2c'}, 1000);
	    });
	}
	lastFlashControlState = currentGame.gameState;
}