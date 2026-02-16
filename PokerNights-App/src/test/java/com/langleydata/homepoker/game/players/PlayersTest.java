package com.langleydata.homepoker.game.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.langleydata.homepoker.TestUtils;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.game.texasHoldem.HandRank;
import com.langleydata.homepoker.game.texasHoldem.PokerHand;
import com.langleydata.homepoker.game.texasHoldem.TexasGameState;
import com.langleydata.homepoker.game.texasHoldem.Winner;
import com.langleydata.homepoker.player.PlayerInfo;

public class PlayersTest {
	
	@Test
	public void testGetUnrankedPlayers() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p.add(p2);
		Player p3 = makePlayer("C", 2);
		p.add(p3);
		Player p4 = makePlayer("D", 3);
		p.add(p4);
		
		// All players have stacks, so empty list
		assertEquals(0, p.getUnRankedLoosers().size());
		
		// Two players with 0 stack, C bet more
		p2.getCurrentStack().addToTable(TexasGameState.TURN, 10);
		p3.getCurrentStack().addToTable(TexasGameState.FLOP, 2);
		p2.getCurrentStack().setStack(0);
		p3.getCurrentStack().setStack(0);
		
		List<Player> loosers = p.getUnRankedLoosers();
		assertEquals(2, loosers.size());
		assertEquals("C", loosers.get(0).getPlayerHandle());
		assertEquals("B", loosers.get(1).getPlayerHandle());
		
		p4.getCurrentStack().addToTable(TexasGameState.RIVER, 1);
		p4.getCurrentStack().setStack(0);
		loosers = p.getUnRankedLoosers();
		assertEquals(3, loosers.size());
		assertEquals("D", loosers.get(0).getPlayerHandle());
		assertEquals("C", loosers.get(1).getPlayerHandle());
		assertEquals("B", loosers.get(2).getPlayerHandle());
	}
	
	@Test
	public void testGetRrankedPlayers() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p.add(p2);
		Player p3 = makePlayer("C", 2);
		p.add(p3);
		Player p4 = makePlayer("D", 3);
		p.add(p4);
		
		// All players have no rank, so empty list
		assertEquals(0, p.getRankedPlayers().size());
		
		// Two players with ranks, C is higher
		p2.getCurrentStack().getGameStats().setRank(3);
		p3.getCurrentStack().getGameStats().setRank(4);
		
		List<Player> loosers = p.getRankedPlayers();
		assertEquals(2, loosers.size());
		assertEquals("B", loosers.get(0).getPlayerHandle());
		assertEquals("C", loosers.get(1).getPlayerHandle());
	}
	
	@Test
	public void testWinStatsForSingle() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p.add(p2);
		Player p3 = makePlayer("C", 2);
		p.add(p3);
		
		// A wins first
		p.setWinners(Collections.singletonList(p1));
		
		assertEquals(1, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(0, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		// B wins second
		p.setWinners(Collections.singletonList(p2));
		
		assertEquals(1, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p3.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p3.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p3.getCurrentStack().getGameStats().getHandsPlayed());
		
		
		// A wins again
		p.setWinners(Collections.singletonList(p1));
		
		assertEquals(2, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p3.getCurrentStack().getGameStats().getWon());
		assertEquals(3, p3.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p3.getCurrentStack().getGameStats().getHandsPlayed());
	}
	
	@Test
	public void testPlayerEqualsWinner() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p1.setRankedHand(new HandRank(100, PokerHand.FLUSH, Collections.EMPTY_LIST));
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p2.setRankedHand(new HandRank(100, PokerHand.FLUSH, Collections.EMPTY_LIST));
		p.add(p2);
		
		Winner w1 = new Winner(p1);
		Winner w2 = new Winner(p2);
		
		assertTrue(p1.equals(p1));
		assertTrue(p2.equals(p2));
		assertFalse(p1.equals(p2));
		
		assertTrue(w1.equals(w1));
		assertTrue(w2.equals(w2));
		assertFalse(w1.equals(w2));
		
		assertTrue(p1.equals(w1));
		assertTrue(p2.equals(w2));
		assertFalse(p1.equals(w2));
	}
	
	@Test
	public void testGetPlayerAsWinner() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p1.setRankedHand(new HandRank(100, PokerHand.FLUSH, Collections.EMPTY_LIST));
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p2.setRankedHand(new HandRank(100, PokerHand.FLUSH, Collections.EMPTY_LIST));
		p.add(p2);
		
		Winner w1 = new Winner(p1);
		Winner w2 = new Winner(p2);
		
		assertEquals(p1, p.getPlayer(p1));
		assertEquals(p2, p.getPlayer(p2));
		assertEquals(p1, p.getPlayer(w1));
		assertEquals(p2, p.getPlayer(w2));
		
		assertFalse(p1.equals(p.getPlayer(w2)));
	}
	
	@Test
	public void testCollectBetsDoesRefundOnFold() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p.add(p2);
		Player p3 = makePlayer("C", 2);
		p.add(p3);
		
		TexasGameState rd = TexasGameState.FLOP;
		p1.getCurrentStack().addToTable(rd, 0.4f);
		p2.getCurrentStack().addToTable(rd, 0.4f);
		p3.getCurrentStack().addToTable(rd, 2f);
		
		// Players fold large bet
		p1.getState().setLastAction(PlayerActionType.FOLD);
		p2.getState().setLastAction(PlayerActionType.FOLD);
		
		assertEquals(0.4 * 3, p.collectBets(rd), 0.001);
		assertEquals(9.6, p1.getCurrentStack().getStack(), 0.001);
		assertEquals(9.6, p2.getCurrentStack().getStack(), 0.001);
		assertEquals(9.6, p3.getCurrentStack().getStack(), 0.001);
		
		rd = TexasGameState.TURN;
		p1.getCurrentStack().addToTable(rd, 0.4f);
		p2.getCurrentStack().addToTable(rd, 2.4f);
		p3.getCurrentStack().addToTable(rd, 0.4f);
		
		assertEquals(0.4 * 3, p.collectBets(rd), 0.001);
		assertEquals(9.2, p1.getCurrentStack().getStack(), 0.001);
		assertEquals(9.2, p2.getCurrentStack().getStack(), 0.001);
		assertEquals(9.2, p3.getCurrentStack().getStack(), 0.001);
	}
	
	@Test
	public void testWinStatsForMultiple() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p.add(p2);
		Player p3 = makePlayer("C", 2);
		p.add(p3);
		Player p4 = makePlayer("D", 3);
		p.add(p4);
		
		List<PlayerInfo> winners = new ArrayList<>();
		// A wins first
		winners.add(p1);
		p.setWinners(winners);
		
		assertEquals(1, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(0, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p2.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p3.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p3.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p3.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p4.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p4.getCurrentStack().getGameStats().getLost());
		assertEquals(1, p4.getCurrentStack().getGameStats().getHandsPlayed());
		
		// B wins second
		winners.clear();
		winners.add(p2);
		p.setWinners(winners);
		
		assertEquals(1, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(1, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p2.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p3.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p3.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p3.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(0, p4.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p4.getCurrentStack().getGameStats().getLost());
		assertEquals(2, p4.getCurrentStack().getGameStats().getHandsPlayed());
		
		// C and D wins third (split pot)
		winners.clear();
		winners.add(p3);
		winners.add(p4);
		p.setWinners(winners);
		
		assertEquals(1, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p2.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p3.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p3.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p3.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p4.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p4.getCurrentStack().getGameStats().getLost());
		assertEquals(3, p4.getCurrentStack().getGameStats().getHandsPlayed());
		
		// A and D win third (split pot)
		winners.clear();
		winners.add(p1);
		winners.add(p4);
		p.setWinners(winners);
		
		assertEquals(2, p1.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p1.getCurrentStack().getGameStats().getLost());
		assertEquals(4, p1.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p2.getCurrentStack().getGameStats().getWon());
		assertEquals(3, p2.getCurrentStack().getGameStats().getLost());
		assertEquals(4, p2.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(1, p3.getCurrentStack().getGameStats().getWon());
		assertEquals(3, p3.getCurrentStack().getGameStats().getLost());
		assertEquals(4, p3.getCurrentStack().getGameStats().getHandsPlayed());
		
		assertEquals(2, p4.getCurrentStack().getGameStats().getWon());
		assertEquals(2, p4.getCurrentStack().getGameStats().getLost());
		assertEquals(4, p4.getCurrentStack().getGameStats().getHandsPlayed());
	}
	
	@Test
	public void testOverbetRefunded() {
		Players p = new Players();
		Player p1 = makePlayer("A", 0);
		p.add(p1);
		Player p2 = makePlayer("B", 1);
		p.add(p2);
		Player p3 = makePlayer("C", 2);
		p.add(p3);
		
		p1.getCurrentStack().setStack(50);
		p1.getCurrentStack().addToTable(TexasGameState.FLOP, 50);
		
		p2.getCurrentStack().setStack(50);
		p2.getCurrentStack().addToTable(TexasGameState.FLOP, 20);
		
		p3.getCurrentStack().setStack(50);
		p3.getCurrentStack().addToTable(TexasGameState.FLOP, 20);
		
		assertEquals(60, p.collectBets(TexasGameState.FLOP), 0.001);
		assertEquals(30, p1.getCurrentStack().getStack(), 0.001);
		assertEquals(30, p2.getCurrentStack().getStack(), 0.001);
		assertEquals(30, p3.getCurrentStack().getStack(), 0.001);
		
		assertEquals(0, p1.getCurrentStack().getOnTable(), 0.001);
		assertEquals(0, p2.getCurrentStack().getOnTable(), 0.001);
		assertEquals(0, p3.getCurrentStack().getOnTable(), 0.001);
	}

	@Test
	public void testRelativePositionsMovingTheAction() {
		
		Players p = new Players();
		p.add(makePlayer(UUID.randomUUID().toString(), 1));
		p.add(makePlayer(UUID.randomUUID().toString(), 3));
		p.add(makePlayer(UUID.randomUUID().toString(), 4));
		p.add(makePlayer(UUID.randomUUID().toString(), 6));
		Player player8 = makePlayer(UUID.randomUUID().toString(), 8);
		p.add(player8);

		// Set initial position (1)
		Player actionOn = getPlayerBySeat(p, 1);
		actionOn.getState().setActionOn(true);
		Assert.assertEquals(1, actionOn.getSeatingPos());
		
		// move the action from the dealer to the next position (3)
		Assert.assertTrue(p.moveActionToNextPlayer(actionOn));
		actionOn = p.getActionOn();
		Assert.assertEquals(3, actionOn.getSeatingPos());
		
		// Fold position (3)
		actionOn.getState().setLastAction(PlayerActionType.FOLD);
		
		// Move on to pos 4
		Assert.assertTrue(p.moveActionToNextPlayer(actionOn));
		actionOn = p.getActionOn();
		Assert.assertEquals(4, actionOn.getSeatingPos());
		
		// Move on to pos 6
		Assert.assertTrue(p.moveActionToNextPlayer(actionOn));
		actionOn = p.getActionOn();
		Assert.assertEquals(6, actionOn.getSeatingPos());
		
		// now remove a player and re-test
		p.remove(player8);
		
		// jumps to pos 1
		Assert.assertTrue(p.moveActionToNextPlayer(actionOn));
		actionOn = p.getActionOn();
		Assert.assertEquals(1, actionOn.getSeatingPos());
		
		// p4 has gone all in, so no cash left to bet
		Player p4 = getPlayerBySeat(p, 4);
		p4.getState().setLastAction(PlayerActionType.ALL_IN);
		p4.getCurrentStack().setStack(0);
		
		// Player 3 has folded and player 4 has no case to bet
		Assert.assertTrue(p.moveActionToNextPlayer(actionOn));
		Assert.assertEquals(6, p.getActionOn().getSeatingPos());
		
	}
	
	@Test
	public void testPlayersLeavingGame() {
		Players p = new Players();
		Player p1 = makePlayer(UUID.randomUUID().toString(), 0);
		p.add(p1);
		Player p2 = makePlayer(UUID.randomUUID().toString(), 1);
		p.add(p2);
		Player p3 = makePlayer(UUID.randomUUID().toString(), 2);
		p.add(p3);
		
		p1.getState().setActionOn(true);
		p1.getState().setLastAction(PlayerActionType.FOLD);
		
		// when
		p.moveActionToNextPlayer(p1);
		Assert.assertTrue(p2.getState().isActionOnMe());
		// player 2 folds, move action to 3
		p2.getState().setLastAction(PlayerActionType.FOLD);
		p.moveActionToNextPlayer(p2);
		Assert.assertTrue(p3.getState().isActionOnMe());
		
		// Game could should trap when only 1 player left in the game
	}
	
	@Test
	public void testGetDealer() {
		Players p = new Players();
		
		// No players
		Assert.assertNull(p.getDealer());
		
		// One player, not dealer
		Player a = makePlayer("A", 0);
		p.add(a);
		Assert.assertEquals(a, p.getDealer());

		Player b = makePlayer("B", 0);
		p.add(b);
		Player c = makePlayer("C", 0);
		p.add(c);
		// 3 players, no dealer
		Assert.assertEquals(a, p.getDealer());
		
		// 3 players, dealer is pos 2
		p = new Players();
		a = makePlayer("A", 0);
		p.add(a);
		b = makePlayer("B", 0);
		p.add(b);
		c = makePlayer("C", 0);
		c.getState().setDealer(true);
		p.add(c);
		Assert.assertEquals(c, p.getDealer());
		
		// 3 players out of seating order, no flag set
		p = new Players();
		a = makePlayer("A", 4);
		p.add(a);
		b = makePlayer("B", 2);
		p.add(b);
		c = makePlayer("C", 8);
		p.add(c);
		Assert.assertEquals(b, p.getDealer());
		
		// 3 players out of seating order, with flag set
		p = new Players();
		a = makePlayer("A", 4);
		p.add(a);
		b = makePlayer("B", 2);
		p.add(b);
		c = makePlayer("C", 8);
		c.getState().setDealer(true);
		p.add(c);
		Assert.assertEquals(c, p.getDealer());
	}

	@Test
	public void testPlayersAddedAretAutoSeated() {
		Players tg = new Players();
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("B", -1);
		Player p3 = makePlayer("C", -1);
		tg.add(p1);
		tg.add(p2);
		tg.add(p3);
		
		assertEquals(0, p1.getSeatingPos());
		assertEquals(1, p2.getSeatingPos());
		assertEquals(2, p3.getSeatingPos());
		
	}
	
	@Test
	public void testPlayersAddedSeatedAtPositions() {
		Players tg = new Players();
		Player p1 = makePlayer("A", 1);
		Player p2 = makePlayer("B", 3);
		Player p3 = makePlayer("C", 8);
		Player p4 = makePlayer("D", -1);
		
		tg.add(p1);
		tg.add(p2);
		tg.add(p3);
		tg.add(p4);
		
		assertEquals(1, p1.getSeatingPos());
		assertEquals(3, p2.getSeatingPos());
		assertEquals(8, p3.getSeatingPos());
		assertEquals(0, p4.getSeatingPos());
		
	}
	
	@Test
	public void testRelativePositionsToDealer() {
		
		Players p = new Players();
		p.add(makePlayer(UUID.randomUUID().toString(), 1));
		p.add(makePlayer(UUID.randomUUID().toString(), 3));
		Player player4 = makePlayer(UUID.randomUUID().toString(), 4);
		p.add(player4);
		p.add(makePlayer(UUID.randomUUID().toString(), 6));
		p.add(makePlayer(UUID.randomUUID().toString(), 8));

		// Dealer will default to pos 1
		Player dealer = p.getDealer();
		Assert.assertEquals(1, dealer.getSeatingPos());
		
		// dealer at pos 1, +4 places = player at 8 
		Assert.assertEquals(8, p.getPlayerRelativeTo(p.getDealer(), 4, true).getSeatingPos());
		
		// Move the dealer to next position (3)
		dealer = moveDealerOn(p);
		
		Assert.assertEquals(3, p.getDealer().getSeatingPos());
		// dealer (3) + 4 seats = 1
		Assert.assertEquals(1, p.getPlayerRelativeTo(p.getDealer(), 4, true).getSeatingPos());
		// dealer (3) - 2 seats = 8
		Assert.assertEquals(8, p.getPlayerRelativeTo(p.getDealer(), -2, true).getSeatingPos());
		
		
		// now remove a player and re-test
		p.remove(player4);
		dealer = moveDealerOn(p);
		
		// Dealer now at pos 6 (skipping pos 4)
		Assert.assertEquals(6, p.getDealer().getSeatingPos());
		// 4 players left, so dealer + 4 seats = the dealer at 6
		Assert.assertEquals(6, p.getPlayerRelativeTo(p.getDealer(), 4, true).getSeatingPos());
		
		// fold pos 1
		getPlayerBySeat(p, 1).getState().setLastAction(PlayerActionType.FOLD);
		
		// dealer (6) -2 seats = pos 8 (4 gone, 1 folded)
		Assert.assertEquals(8, p.getPlayerRelativeTo(p.getDealer(), -2, true).getSeatingPos());
		
	}
	
	@Test
	public void testPlayerRelativeToPlayer() {
		Players p = new Players();
		
		p = getNPlayers(3);
		Assert.assertEquals(0, p.getPlayerRelativeTo(getPlayerBySeat(p, 0), 0, true).getSeatingPos());
		Assert.assertEquals(1, p.getPlayerRelativeTo(getPlayerBySeat(p, 0), 1, true).getSeatingPos());
		

		p = getNPlayers(9);
		Assert.assertEquals(7, p.getPlayerRelativeTo(getPlayerBySeat(p, 4), 3, true).getSeatingPos());
		Assert.assertEquals(1, p.getPlayerRelativeTo(getPlayerBySeat(p, 8), 2, true).getSeatingPos());// p9 + 2 = p2
		
		p = getNPlayers(5);
		Assert.assertEquals(4, p.getPlayerRelativeTo(getPlayerBySeat(p, 0), 4, true).getSeatingPos());

		p = getNPlayers(6);
		Assert.assertEquals(1, p.getPlayerRelativeTo(getPlayerBySeat(p, 5), 2, true).getSeatingPos());
		Assert.assertEquals(0, p.getPlayerRelativeTo(getPlayerBySeat(p, 3), 3, true).getSeatingPos());
		Assert.assertEquals(4, p.getPlayerRelativeTo(getPlayerBySeat(p, 5), -7, true).getSeatingPos()); // p6 -7 = 4 (full rotation
		
		p = getNPlayers(6);
		Assert.assertEquals(2, p.getPlayerRelativeTo(getPlayerBySeat(p, 5), -3, true).getSeatingPos());
		Assert.assertEquals(4, p.getPlayerRelativeTo(getPlayerBySeat(p, 1), -3, true).getSeatingPos());
		Assert.assertEquals(4, p.getPlayerRelativeTo(getPlayerBySeat(p, 0), 4, true).getSeatingPos());
		
		p = getNPlayers(3);
		Assert.assertEquals(2, p.getPlayerRelativeTo(getPlayerBySeat(p, 2), -3, true).getSeatingPos());//p3 - 3 = p3 (seat 2)
		Assert.assertEquals(1, p.getPlayerRelativeTo(getPlayerBySeat(p, 0), -2, true).getSeatingPos());// p1 - 2 = p2 (seat 1)
		Assert.assertEquals(0, p.getPlayerRelativeTo(getPlayerBySeat(p, 0), -3, true).getSeatingPos());// p1 - 3 = p1 (seat 0)
	}
	
	@Test
	public void testCollectBets() {
		Players tg = new Players();
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("B", -1);
		Player p3 = makePlayer("C", -1);
		tg.add(p1);
		tg.add(p2);
		tg.add(p3);
		
		final TexasGameState state = TexasGameState.FLOP;
		
		p1.getCurrentStack().addToTable(state, 5f);
		p2.getCurrentStack().addToTable(state, 5f);
		p3.getCurrentStack().addToTable(state, 5f);
		
		final float tot = tg.collectBets(state);
		assertEquals(15f, tot, 0.01);
		assertEquals(5f, p1.getCurrentStack().getCommitedPerRound().get(state), 0.01);
		assertEquals(5f, p2.getCurrentStack().getCommitedPerRound().get(state), 0.01);
		assertEquals(5f, p3.getCurrentStack().getCommitedPerRound().get(state), 0.01);
		
		assertEquals(0f, p1.getCurrentStack().getOnTable(), 0.1);
		assertEquals(0f, p2.getCurrentStack().getOnTable(), 0.1);
		assertEquals(0f, p3.getCurrentStack().getOnTable(), 0.1);
		
		assertEquals(5f, p1.getCurrentStack().getStack(), 0.1);
		assertEquals(5f, p2.getCurrentStack().getStack(), 0.1);
		assertEquals(5f, p3.getCurrentStack().getStack(), 0.1);
	}
	
	@Test
	public void testLastPlayerActioned() throws InterruptedException {
		Players tg = new Players();
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("B", -1);
		Player p3 = makePlayer("C", -1);
		tg.add(p1);
		tg.add(p2);
		tg.add(p3);
		
		p1.getState().setLastAction(PlayerActionType.BET);
		Thread.sleep(1);
		p2.getState().setLastAction(PlayerActionType.CALL);
		Thread.sleep(1);
		p3.getState().setLastAction(PlayerActionType.BET);
		
		assertEquals("C", tg.getLastPlayerToAction(true).getPlayerId());
		
		// Check excludes a fold
		Thread.sleep(1);
		p2.getState().setLastAction(PlayerActionType.CALL);
		Thread.sleep(1);
		p3.getState().setLastAction(PlayerActionType.FOLD);
		
		assertEquals("B", tg.getLastPlayerToAction(true).getPlayerId());
		
		// Check doesn't exclude fold
		assertEquals("C", tg.getLastPlayerToAction(false).getPlayerId());
		
		// Check doesn't bomb
		Player p4 = makePlayer("D", -1);
		tg.add(p4);
		assertEquals("C", tg.getLastPlayerToAction(false).getPlayerId());
		
		p1.getState().setLastAction(PlayerActionType.CALL);
		assertEquals("A", tg.getLastPlayerToAction(true).getPlayerId());
		
		Thread.sleep(1);
		p4.getState().setLastAction(PlayerActionType.ALL_IN);
		assertEquals("D", tg.getLastPlayerToAction(true).getPlayerId());		
	}
	
	@Test
	public void testZeroBetsNotCollected() {
		Players tg = new Players();
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("B", -1);
		Player p3 = makePlayer("C", -1);
		tg.add(p1);
		tg.add(p2);
		tg.add(p3);
		
		final TexasGameState state = TexasGameState.FLOP;
		
		final float tot = tg.collectBets(state);
		assertEquals(0f, tot, 0.01);
	}
	
	@Test
	public void testRefundOverbet() {
		Players tg = new Players();
		Player p1 = makePlayer("A", -1);
		Player p2 = makePlayer("B", -1);
		Player p3 = makePlayer("C", -1);
		tg.add(p1);
		tg.add(p2);
		tg.add(p3);
		
		final TexasGameState state = TexasGameState.FLOP;
		
		p1.getCurrentStack().addToTable(state, 10f);
		p2.getCurrentStack().addToTable(state, 5f);
		p3.getCurrentStack().addToTable(state, 5f);
		
		final float tot = tg.collectBets(state);
		assertEquals(15f, tot, 0.01);
		assertEquals(5f, p1.getCurrentStack().getCommitedPerRound().get(state), 0.01);
		assertEquals(5f, p2.getCurrentStack().getCommitedPerRound().get(state), 0.01);
		assertEquals(5f, p3.getCurrentStack().getCommitedPerRound().get(state), 0.01);
		
		assertEquals(0f, p1.getCurrentStack().getOnTable(), 0.1);
		assertEquals(0f, p2.getCurrentStack().getOnTable(), 0.1);
		assertEquals(0f, p3.getCurrentStack().getOnTable(), 0.1);
		
		assertEquals(5f, p1.getCurrentStack().getStack(), 0.1);
		assertEquals(5f, p2.getCurrentStack().getStack(), 0.1);
		assertEquals(5f, p3.getCurrentStack().getStack(), 0.1);
		
	}
	
	private Player moveDealerOn(Players players) {
		Player newDealer = players.getPlayerRelativeTo(players.getDealer(), 1, true);
		players.forEach(p->p.getState().setDealer(false));
		newDealer.getState().setDealer(true);
		return newDealer;
	}
	private Players getNPlayers(int num) {
		Players p = new Players();
		for (int i = 0; i < num; i++) {
			p.add(makePlayer(UUID.randomUUID().toString(), i));
		}
		return p;
	}
	/** Create a player with a wallet of 10 and a stack of 10
	 * 
	 * @param id
	 * @param pos
	 * @return
	 */
	private Player makePlayer(final String id, final int pos) {
		Player p = TestUtils.makePlayer(id, pos);
		p.getCurrentStack().initialise(20, false);
		p.getCurrentStack().reBuy(10, 0);
		return p;
	}
	
	private Player getPlayerBySeat(Players players, int seat) {
		return players.stream()
				.sorted(Comparator.comparing(Player::getSeatingPos, Comparator.naturalOrder()))
				.filter(p -> p.getSeatingPos()==seat)
				.findFirst()
				.orElse(null);
	}
}
