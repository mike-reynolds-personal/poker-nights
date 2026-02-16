

function formatAsMoney(num) {
	var val = num.toFixed(2).replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
	
	if (val.startsWith('-')) {
		return '<font color="red">-&pound;' + val.replace('-', '') + '</font>';
	} else {
		return '<font color="green">' + val + '</font>';
	}
}

function showAccInfo(compact) {
	if (!identity.playerId) {
		return;
	}
	$("#copyAccount").off();
	$("#copyAccount").click(function() {
		copyToImage('copyAreaAccount');
	});
	
	$("#accountArea tr").remove();
	$("#accountStatsArea tr").remove();
	
	$.get('/account/' + identity.playerId, function(data) {
		if (jQuery.isEmptyObject(data)) {
			console.log('No account data available');
			return;
		}
		
		var jd = JSON.parse(data);
		
		if (jd.errorMessage) {
			console.warn(jd.errorMessage);
			return;
		}

		$("#accountNum").text(identity.playerId);
		
		$("#profilePic").attr('src', jd.picture);
		var head = $("#accountArea");
		head.append(getRow('Player Handle', jd.playerHandle));
		head.append(getRow('Full Name', jd.fullName));
		head.append(getRow('Email', jd.email));
		head.append(getRow('Acc Level', jd.accLevel.replace('_', ' ')));
		head.append(getRow('Roles', jd.roles.join()));
		
		var stats = $("#accountStatsArea");
		stats.append(getRow('Balance', formatAsMoney(jd.accStats.balance)));
		stats.append(getRow( 'Won / Lost', jd.accStats.won + ' / ' + jd.accStats.lost));
		stats.append(getRow('Hands played', jd.accStats.handsPlayed));
		stats.append(getRow('Vol. bets', jd.accStats.volBets));
		stats.append(getRow('Re-buys', jd.accStats.rebuys));
		stats.append(getRow('Games played', jd.accStats.gamesPlayedIn.length));
		stats.append(getRow('Games organised', Object.keys(jd.accStats.organised).length));
		stats.append(getRow('Last played', new Date(jd.accStats.lastPlayed).toUTCString()));
		stats.append(getRow('Total time played', new Date(jd.accStats.timePlayed * 1000).toISOString().substr(11, 8)));
		
		if (jd.accLevel == 'TIER_3' || jd.accLevel == 'TIER_4') {
			$("#upgradeArea").hide();
		} else {
			$("#upgradeArea").show();
		}
		
		$("#modalAccount").modal('show');
	});
    
}

function getRow(key, value) {
	return '<tr><td>' + key + '</td><td>' + value + '</td></tr>';
}