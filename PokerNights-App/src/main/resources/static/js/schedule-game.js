/** Copyright Langley Data Solutions Ltd 2020 - Present */
//$.getScript('https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.22.2/moment.min.js', function() {
//	$.getScript('/js/lib/en-gb.js');
//	$.getScript('/js/lib/tempusdominus-bootstrap-4.min.js', function() {
//		$("#gameSchedulePicker").datetimepicker(dtOptions);
//		initControls();
//	});
//	
//});

const maxPlayers = 8;// TODO this should really come from the game settings  template for the game type
const dtOptions = {
	   	 locale: 'en-gb',
		 sideBySide: true,
		 minDate: new Date(),
	     maxDate: new Date(new Date().setDate(new Date().getDate()+365)),
	    icons: {
	        time: "far fa-clock",
	        date: "far fa-calendar"
	    },
	    format: 'DD/MM/YYYY HH:mm',
	    stepping: 15
	};

/* For any page that wants to host this form, set-up the 'createGame' button action
 * Relies on the button id of 'createGame'
 */
$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    
	$("#gameSchedulePicker").datetimepicker(dtOptions);
	initControls();
	
	getSession( function() {
		var identity = JSON.parse(Cookies.get('identity', ''));
		if (identity) {
			$("#orgName").val( identity.playerHandle );
			$("#orgEmail").val( identity.playerEmail );
			
			// default the host to the organiser
			$("#hostName").val( identity.playerHandle );
			$("#hostEmail").val( identity.playerEmail );
			
			if(parseInt(identity.accLevel.replace('TIER_', '')) < 3) {
				$("#hostName").prop('disabled', true);
				$("#hostEmail").prop('disabled', true);
			}
		}
		$("#errorLabel").text('');
		$("#createGameForm").modal('show');
	});

	$('#createGameForm').on('hidden.bs.modal', function (e) {
		// use referer?
		  location.href = 'https://www.pokerroomsnow.com';
		});
});

/** Initialise the button actions on the createGame form */
function initControls() {
    
	$('[data-toggle="tooltip"]').tooltip();
   
	// The game options selection/ update
	$(".gameFormatOpt").click(function() {
		var format =  $(this).text();
		$("#gameFormat").text( format );
		if (format === 'Tournament') {
			$("#cashGameOpts").collapse('hide');
			$("#tourGameOpts").collapse('show');
			$("#buyinLab").text('Entry fee:');
			$("#smallBlind").val(parseInt($("#startingChips").val()) / 100.0);
		} else {
			$("#tourGameOpts").collapse('hide');
			$("#cashGameOpts").collapse('show');
			$("#buyinLab").text('Buy-in:');
		}
		enableCreateBtn();
	});
	$(".gameTypeOpt").click(function() {
		$("#gameType").text( $(this).text() );
	});
	$(".moneyTypeOpt").click(function() {
		$("#moneyType").text( $(this).text() );
	});
	$("#scheduleFor").change(function() {
		enableCreateBtn();
	});
	$("#hostName").on('input', function() {
		enableCreateBtn();
	});
	$("#hostEmail").on('input', function() {
		enableCreateBtn();
	});
	$("#termsAndCons").change(function() {
		enableCreateBtn();
	});
	$("#winningSplit").on('input', function() {
		enableCreateBtn();
	});
	$("#schedNow").click(function() {
		var date = new Date();
		var p1 = ((date.getDate() > 9) ? date.getDate() : ('0' + date.getDate())) 
			+ '/' + ((date.getMonth() > 8) ? (date.getMonth() + 1) : ('0' + (date.getMonth() + 1))) 
			+ '/' + date.getFullYear();
		var h = ''+date.getHours();
		var m = ''+date.getMinutes();
		
		$("#scheduleFor").val(p1 + ' ' + (h.length==1 ? '0' + h : h) + ':' + (m.length==1 ? '0' + m : m));
		enableCreateBtn();
	});
	$("#submitCreateGame").click(function() {
		$("#submitCreateGame").prop('disabled', true);
		return createGameClicked();
	});
	$("#addInvite").click(function() {
		addInvite();
		enableCreateBtn();
	});

	$("#moneyType").click(function() {
		enableCreateBtn();
	});
	$("#gameFormat").click(function() {
		enableCreateBtn();
	});
	$("#gameType").click(function() {
		enableCreateBtn();
	});
	$("#clearInvite").click(function() {
		$("#invitesList").html('');
		$("#numInvited").html('0');
		enableCreateBtn();
	});
	$("#useActionTimeout").click(function() {
		$("#actionTimeout").prop('disabled', $(this).is(":checked")==false);
	});
}

function addInvite() {
	var newEmail = $("#addInviteEmal");
	var list = $("#invitesList");
	var em = newEmail.val();
	
	var invited = list.val().split('\n').length;
	
	if (em && !list.val().includes(em) ) {
		$("#invitesList").append(em + "\n");
		newEmail.val('');
		if (invited >= maxPlayers) {
			$("#numInvited").css({'color': 'red', 'font-weight': 'bold'});
			$("#numInvited").text(invited + ' (> max players for a table)');
		} else {
			$("#numInvited").text('' + invited);
		}
	}
}

function gameCreated(game) {
	$('#createGameForm').off();
	$(".modal").modal('hide');
	location.replace('/?newGame='+ encodeURI(game.gameUrl));
}

function getOption(option) {
	return option.replaceAll(' ', '_').toUpperCase();
}

function enableCreateBtn() {
	var enableCreateBtn = true;
	
	var sf = $("#scheduleFor").val();
	var hn = $("#hostName").val();
	var he = $("#hostEmail").val();
	var on = $("#orgName").val();
	var oe = $("#orgEmail").val();
	var tc = $("#termsAndCons").is(":checked");
	var ante = parseFloat($("#smallBlind").val());
	
	if ( on=='' || oe =='' || hn=='' || he =='' || !tc || sf=='' || ante <= 0) {
		enableCreateBtn = false;
	}

	var ante = 0;
	var gf = getOption( $("#gameFormat").text() );
	if (gf == 'TOURNAMENT') {
		var ws = $("#winningSplit").val();
		if (ws == '') {
			enableCreateBtn = false;
		} else {
			var tot = 0;
			$.each(ws.split(','),function( idx,val ) {
				tot +=parseInt(val);
			});
			if (tot != 100) {
				enableCreateBtn = false;
			}
		}
	}
	
	if ($("#gameType").text() != "Texas Hold'em") {
		enableCreateBtn = false;
	}
	
	$("#submitCreateGame").prop('disabled', !enableCreateBtn);
}

function createGameClicked() {

	var sf = $("#scheduleFor").val();
	var hn = $("#hostName").val();
	var he = $("#hostEmail").val();
	var on = $("#orgName").val();
	var oe = $("#orgEmail").val();
	var ai = $("#additionalInfo").val();
	var tc = $("#termsAndCons").is(":checked");
	var ante = parseFloat($("#smallBlind").val());
	var actionTimeout = -1;
	if ($("#useActionTimeout").is(":checked")) {
		actionTimeout = parseInt($("#actionTimeout").val());
	}
	
	if ( on=='' || oe =='' || hn=='' || he =='' || !tc || sf=='' || ante <=0) {
		return;
	}

	// Collect info and submit game
	var gt = $("#gameType").text();
	if (gt=="Texas Hold'em") {
		gt =  'TEXAS_HOLDEM';
	} else {
		console.log('Invalid game type');
		return;
	}
	
	// Format - Cash or Tournament
	var gf = getOption( $("#gameFormat").text() );
	
	// Tournament options
	var startChips = 0;
	var winSplit = [];
	var numTables = parseInt($("#numTables").val());
	var blindsInc = Number.MAX_SAFE_INTEGER;
	var joinRound = parseInt($("#joinUpTo").val());
	
	// Cash only options
	var hcw = false;
	var rbir = false;
	
	if (gf == 'TOURNAMENT') {
		startChips = parseInt($("#startingChips").val());
		winSplit = $("#winningSplit").val().split(',');
		blindsInc = parseInt($("#blindsIncAt").val()) * 60 * 1000;//to milliseconds
	} else {
		hcw = $("#hostWalletOpt").is(':checked');
		rbir = $("#rebuyInRoundOpt").is(':checked');
	}
	
	// Money type - Real or Virtual
	var mf = getOption( $("#moneyType").text() );
	
	// Calculate the real scheduled time
	const sfmUtc = moment(sf, "D/M/YYYY H:mm");

	var pt = getOption( $("#potTypeOpt").children("option:selected").val() );
	var rb = getOption( $("#rebuyOpt").children("option:selected").val() );
	var so = getOption( $("#shuffleOpt").children("option:selected").val() );
	
	var c = Cookies.get('identity', '');
	var playerId;
	if (c) {
		playerId = JSON.parse(c).playerId;
	}
	
	// Convert invites to array
	var invites = [];
	var rawInv = $("#invitesList").val().split('\n');
	for (var m=0; m < rawInv.length; m++) {
		if (rawInv[m]) {
			invites.push(rawInv[m]);
		}
	}

	var settings = JSON.stringify(
	    	{
	    	'gameType': gt,
	    	'organiserId':  playerId,
	    	'organiserName': on,
	    	'organiserEmail': oe,
	    	'hostName': hn,
	    	'hostEmail': he,
	    	'scheduledTime': sfmUtc.valueOf(),
	    	'buyInAmount': parseInt($("#buyinAmount").val()),
	    	'ante': ante, 
	    	'hostControlledWallet': hcw,
	    	'rebuyInRound': rbir,
	    	'enforceMinimumRaise': $("#enforceRaise").is(':checked'),
	    	'potLimit': pt,
	    	'rebuyOption': rb,
	    	'shuffleOption': so,
	    	'invitedEmails': invites,
	    	'format' : gf, 
	    	'moneyType': mf,
	    	'additionalInfo': ai,
	    	'actionTimeout' : actionTimeout,
	    	
	    	'blindIncreaseInterval': blindsInc,
	    	'tournamentSplit': winSplit,
	    	'openingStack': startChips,
	    	'maxRoundForTournamentEntry': joinRound,
	    	'tournamentTables': numTables
	    	});

	$.ajax({
		  url: '/creategame/' + gt,
		  type: 'POST',
		  data: settings,
		  contentType: 'application/json; charset=utf-8',
		  success: function(data) {
			$("#submitCreateGame").prop('disabled', false);
			if ( !data.success ) {
				$("#errorLabel").text(data.error);
				return;
			}
			
			gameCreated(data);
		  }
		}).done(function(msg){  })
	    .fail(function(xhr, status, error) {
	    	$("#submitCreateGame").prop('disabled', false);
	    	$("#errorLabel").text('There was an error trying to create your game: ' +  status + ' (' + error +')');
	    	return;
	    });

	return false;
}
