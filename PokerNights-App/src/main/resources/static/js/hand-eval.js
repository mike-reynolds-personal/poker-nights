/** Copyright Langley Data Solutions Ltd 2020 - Present */
var p1Rank = 0;

$(function () {
	addHead();
    $("#evalButton").click(function() {
    	evalHands();
    });
});

function highlightPlayer(add, p) {
	if (p == -1) {
		highlightPlayer(add, 1);
		highlightPlayer(add, 2);
	} else {
		if (add) {
			$("#playerCard" + p).addClass('bg-success').addClass('text-white').removeClass('bg-light');
		} else {
			$("#playerCard" + p).addClass('bg-light').removeClass('bg-success').removeClass('text-white');
		}
	}
}
function evalHands() {
	highlightPlayer(false, -1);
	var tc = $("#tableCards").val();
	var p1 = $("#playerCards1").val();
	var p2 = $("#playerCards2").val();
	
	getRank(tc, p1, p2.length > 0);
}

function getRank(tc, pc, expectP2) {
	$.get('/handrank?tableCards=' + encodeURIComponent(tc) + '&playerCards=' + encodeURIComponent(pc), function(resp) {
		var ctrl = $("#winningRank");
		if (!resp) {
			ctrl.val('There was an error - sorry');
			return;
		}
		if (resp.message) {
			ctrl.html(resp.message);
			return;
		}
		
		// if we're expecting player 2's cards, store and request
		if (expectP2) {
			p1Rank = resp.handRank;
			getRank(tc,  $("#playerCards2").val(), false);
			return;
		}
		
		// Which hand is better?
		var useRank = p1Rank;
		var p = 0;
		if (resp.handRank.rankValue > p1Rank.rankValue) {
			useRank = resp.handRank;
			p = 2;
		} else if (p1Rank.rankValue > resp.handRank.rankValue ) {
			useRank = p1Rank;
			p = 1;
		} else {
			p = -1;
		}
		
		// Display
		var rCards = useRank.rankedCards;
		var ranked = '';
		for (var i=0; i < rCards.length; i++) {
			ranked += rCards[i].code + ', ';
		}
		ranked = ranked.substring(0, ranked.length - 2);
		highlightPlayer(true, p);
		if (p == -1) {
			ctrl.html( 'Split pot with ' + useRank.rankName + ' (' + ranked + ')');
		} else {
			ctrl.html( 'Player ' + p + ' wins with ' + useRank.rankName + ' (' + ranked + ')');
			$("#playerCard" + p).addClass('bg-success');
		}
		p1Rank = 0;
	});
}