/** Copyright Langley Data Solutions Ltd 2020 - Present */
var newRow = '<div class="form-row mb-1 justify-content-center" id="row-#">' + 
	'<div class="col-3"><input id="pName#" type="text" class="form-control" placeholder="Name" required></div>' + 
	'<div class="col-3"><input id="pBet#" type="number" class="form-control" placeholder="Bet" required></div>' + 
	'<div class="col-3"><input id="pRank#" type="number" class="form-control" placeholder="Rank" required></div>' +
	'</div>';
var rowNum = 3;

$(function () {
	addHead();
	
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
	
    $("#addRowBtn").click(function() {
    	if (rowNum >= 10) {
    		return;
    	}
    	$("#sidePotInput").append( newRow.replace(/#/g, rowNum) );
    	$("#row-"+rowNum).focus();
    	rowNum++;
    	
	});
    
    $("#calcPotsBtn").click(function() {
    	postPlayers();
	});
    
    $("#resetRowsBtn").click(function() {
    	for (var i=rowNum; i > 2; i--) {
    		$("#row-"+i).remove();
    	}
    	$("#pName1").val('');
    	$("#pBet1").val('');
    	$("#pRank1").val('');
    	$("#pName2").val('');
    	$("#pBet2").val('');
    	$("#pRank2").val('');
    	$("#resultOut tr").remove();
    	rowNum = 3;
    });
    
});

function currencyFormat(num, includePound) {
	if (num) {
	  return (includePound ? '&pound;'  : '') + num.toFixed(2).replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
	} else {
		return (includePound ? '&pound;'  : '') + '0.00';
	}
}

function postPlayers() {
	var players = [];
	for (var i=1; i < rowNum; i++) {
		var pn = $("#pName" + i).val();
		var pb = parseFloat($("#pBet" + i).val());
		var pr = parseFloat($("#pRank" + i).val());
		
		if (pn && pb > 0 && pr > 0) {
			var pl = {"name": pn, 'bet': pb, 'rank': pr};
			players.push(pl);
		}
	}
	if (players.length > 0) {
		var toSend = JSON.stringify(players);

		$.ajax({
			  url: '/calcpots',
			  type: 'POST',
			  data: toSend,
			  contentType: 'application/json; charset=utf-8',
			  success: function(data) {
				  var ctl = $("#resultOut");
				  $("#resultOut tr").remove();
				  if (!data.pots) {
					  ctl.html('Sorry, there was an issue');
					  return;
				  }
				  
				  // Iterate each pot and add to the table
				  for (var i=0; i < data.pots.length; i++) {
					  var pot = data.pots[i];
					  
					  // format the winners text
					  var winners = '';
					  for (var j=0; j < pot.winners.length; j++) {
						  winners += pot.winners[j].playerId + ', ';
					  }
					  winners = winners.substring(0, winners.length-2);
					  
					  var row = '<tr><td>' + pot.name + '</td>' + 
					  		'<td>' + currencyFormat(pot.potTotal, true) + '</td>' + 
					  		'<td>' + winners + '</td></tr>';
					  ctl.append(row);
				  }
			  }
			}).done(function(msg){  })
		    .fail(function(xhr, status, error) {
		    	console.log('Error: ' + status);
		    	return;
		    });
	};
}