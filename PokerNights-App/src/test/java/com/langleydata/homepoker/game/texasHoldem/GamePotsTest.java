package com.langleydata.homepoker.game.texasHoldem;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.langleydata.homepoker.TestUtils;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;

public class GamePotsTest {
	private GamePots allPots;
	
	@Test
	public void testSplitPotInSingleRound() {

		allPots = new GamePots();
		List<SidePot> rnd;
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 0);
		final Player pC = TestUtils.makePlayer("C", 0);
		// Note potential winners are always created high-to-low on rank
		
		/* We have 3 pots in this round:
		 * Player A wins £4, player B wins £12 and Player C wins £10
		 */
		
		// Round 1
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(4f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));// A outright winner
		rnd.add(mkSidePot(12f, new HandRank(200, PokerHand.TWO_PAIR, null), pB, pC));// B and C share this pot
		rnd.add(mkSidePot(10f, new HandRank(200, PokerHand.TWO_PAIR, null), pB));// B won last pot against D
		allPots.addSidePots(rnd);
		
		final Map<String, SidePot> result = allPots.getAllPots();
		
		assertPot(result.get(GamePots.FIRST_POT_NAME), PokerHand.FULL_HOUSE, 4f, "A");
		assertPot(result.get("Pot A"), PokerHand.TWO_PAIR, 12f, "B", "C");
		assertPot(result.get("Pot B"), PokerHand.TWO_PAIR, 10f, "B");
		
	}
	
	@Test
	public void testSplitPotInLastRound() {

		allPots = new GamePots();
		List<SidePot> rnd;
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 0);
		final Player pC = TestUtils.makePlayer("C", 0);
		// Note potential winners are always created high-to-low on rank
		
		/* We have 3 pots in this round:
		 * Player A wins £44, player B wins £12 and Player C wins £10
		 */
		
		// Round 1
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(20f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));
		allPots.addSidePots(rnd);
		
		// Round 2
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(20f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));
		allPots.addSidePots(rnd);
		
		// Round 3
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(4f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));// A outright winner
		rnd.add(mkSidePot(12f, new HandRank(200, PokerHand.TWO_PAIR, null), pB, pC));// B and C share this pot
		rnd.add(mkSidePot(10f, new HandRank(200, PokerHand.TWO_PAIR, null), pB));// B won last pot against D
		allPots.addSidePots(rnd);
		
		// Round 4
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(20f, new HandRank(200, PokerHand.TWO_PAIR, null), pB));// B won last pot against D
		allPots.addSidePots(rnd);
		
		final Map<String, SidePot> result = allPots.getAllPots();
		
		assertPot(result.get(GamePots.FIRST_POT_NAME), PokerHand.FULL_HOUSE, 44f, "A");// 40 + when went out
		assertPot(result.get("Pot A"), PokerHand.TWO_PAIR, 12f, "B", "C");
		assertPot(result.get("Pot B"), PokerHand.TWO_PAIR, 30f, "B");// 10 + last round

		assertEquals(3, allPots.getWinners().size());
	}

	@Test
	public void testOnlyMainPotWinner() {
		
		allPots = new GamePots();
		List<SidePot> rnd;
		final Player pA = TestUtils.makePlayer("A", 0);
		// Note potential winners are always created high-to-low on rank
		
		// Round 1
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(10f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));// A outright winner
		allPots.addSidePots(rnd);
		// Round 2
		allPots.addSidePots(rnd);
		// Round 3
		allPots.addSidePots(rnd);
		// Round 4
		allPots.addSidePots(rnd);
		
		final Map<String, SidePot> result = allPots.getAllPots();
		
		assertPot(result.get(GamePots.FIRST_POT_NAME), PokerHand.FULL_HOUSE, 40f, "A");
		assertEquals(1, allPots.getWinners().size());
	}
	
	@Test
	public void testOnlyMainPotTwoWinners() {
		
		allPots = new GamePots();
		List<SidePot> rnd;
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 0);
		// Note potential winners are always created high-to-low on rank
		
		// Round 1
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(10f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA, pB));
		allPots.addSidePots(rnd);
		// Round 2
		allPots.addSidePots(rnd);
		// Round 3
		allPots.addSidePots(rnd);
		// Round 4
		allPots.addSidePots(rnd);
		
		final Map<String, SidePot> result = allPots.getAllPots();
		
		assertPot(result.get(GamePots.FIRST_POT_NAME), PokerHand.FULL_HOUSE, 40f, "A", "B");	
	}

	@Test
	public void testTwoPotsWithTwoWinners() {
		
		allPots = new GamePots();
		List<SidePot> rnd;
		final Player pA = TestUtils.makePlayer("A", 0);
		final Player pB = TestUtils.makePlayer("B", 0);
		// Note potential winners are always created high-to-low on rank
		
		// Round 1
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(10f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));
		rnd.add(mkSidePot(10f, new HandRank(600, PokerHand.STRAIGHT, null), pB));
		allPots.addSidePots(rnd);
		// Round 2
		rnd = new ArrayList<>();
		rnd.add(mkSidePot(5f, new HandRank(600, PokerHand.FULL_HOUSE, null), pA));
		rnd.add(mkSidePot(10f, new HandRank(600, PokerHand.STRAIGHT, null), pB));
		allPots.addSidePots(rnd);
		
		final Map<String, SidePot> result = allPots.getAllPots();
		
		assertPot(result.get(GamePots.FIRST_POT_NAME), PokerHand.FULL_HOUSE, 15f, "A");	
		assertPot(result.get("Pot A"), PokerHand.STRAIGHT, 20f, "B");
		
		assertEquals(2, allPots.getWinners().size());
	}
	
	private SidePot mkSidePot(float potTotal, HandRank bestRank, Player...p ) {
		SidePot pw1 = mock(SidePot.class);
		when(pw1.getPotTotal()).thenReturn(potTotal);
		when(pw1.getPotWinners()).thenReturn(Arrays.asList(p));
		when(pw1.getMaxRank()).thenReturn(bestRank);
		
		return pw1;
	}
	
	private void assertPot(SidePot pot, PokerHand hand, float total, String... winners) {
		assertEquals(total, pot.getPotTotal(), 0.01);
		assertEquals(hand, pot.getMaxRank().getRankName());
		
		Assert.assertTrue(
		pot.getWinners().stream()
			.map(Winner::getPlayerId)
			.collect(Collectors.toList())
			.containsAll(Arrays.asList(winners))
			);
		assertEquals(winners.length, pot.getWinners().size());
	}	
}
