package com.langleydata.homepoker.game.texasHoldem;

import static com.langleydata.homepoker.deck.CardNumber.EIGHT;
import static com.langleydata.homepoker.deck.CardNumber.FIVE;
import static com.langleydata.homepoker.deck.CardNumber.FOUR;
import static com.langleydata.homepoker.deck.CardNumber.JACK;
import static com.langleydata.homepoker.deck.CardNumber.KING;
import static com.langleydata.homepoker.deck.CardNumber.QUEEN;
import static com.langleydata.homepoker.deck.CardNumber.SEVEN;
import static com.langleydata.homepoker.deck.CardNumber.SIX;
import static com.langleydata.homepoker.deck.CardNumber.TEN;
import static com.langleydata.homepoker.deck.CardNumber.TWO;
import static com.langleydata.homepoker.deck.CardSuit.Clubs;
import static com.langleydata.homepoker.deck.CardSuit.Diamonds;
import static com.langleydata.homepoker.deck.CardSuit.Hearts;
import static com.langleydata.homepoker.deck.CardSuit.Spades;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.langleydata.homepoker.TestUtils;
import com.langleydata.homepoker.api.GameSettings;
import com.langleydata.homepoker.api.PlayerAction;
import com.langleydata.homepoker.api.PlayerActionType;
import com.langleydata.homepoker.deck.Card;
import com.langleydata.homepoker.game.players.Player;
import com.langleydata.homepoker.game.players.Players;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePot;
import com.langleydata.homepoker.game.texasHoldem.pots.GamePots;
import com.langleydata.homepoker.game.texasHoldem.pots.SidePot;
import com.langleydata.homepoker.message.PlayerActionMessage;

public class PokerHandEvaluatorTest {

	private PokerHandEvaluator evaluator = new PokerHandEvaluator();

	@Test
	public void testManySplitsButOneWinner() {
		final TexasGameState[] allStates = new TexasGameState[] {TexasGameState.FLOP};
		
		final Player pS = TestUtils.makePlayer("Sarah", allStates, mkBets(6f), new Card("6C"), new Card("7C"));// Sarah
		final Player pAa = TestUtils.makePlayer("Arron", allStates, mkBets(9.6f), new Card("KH"), new Card("5C"));// Arron
		final Player pJ = TestUtils.makePlayer("John", allStates, mkBets(19.2f), new Card("2S"), new Card("JH"));// John
		final Player pG = TestUtils.makePlayer("Gwynneth", allStates, mkBets(14f), new Card("KS"), new Card("9S"));// Gwynneth
		final Player pSa = TestUtils.makePlayer("Samuel", allStates, mkBets(19.2f), new Card("QH"), new Card("4H"));// Samuel
		final Player pAn = TestUtils.makePlayer("Andrew", allStates, mkBets(0f), new Card("TS"), new Card("8S"));// Andrew
		final Player pP = TestUtils.makePlayer("Paul", allStates, mkBets(10.6f), new Card("5D"), new Card("6H"));// Paul
		final Player pC = TestUtils.makePlayer("Chris", allStates, mkBets(0f), new Card("6C"), new Card("8C"));// Chris
		final Player pCs = TestUtils.makePlayer("Chriss", allStates, mkBets(3f), new Card("TC"), new Card("KD"));// Chriss
		pAn.getState().setLastAction(PlayerActionType.FOLD);
		pC.getState().setLastAction(PlayerActionType.FOLD);

		final Players players = new Players();
		players.addAll(Arrays.asList(pS, pAa, pJ, pG, pSa, pAn, pP, pC, pCs));
				
		final GamePots gamePot = evaluator.calculatePotsAndWinners(players, Card.makeCards("3C","4S","4D","AD","JS"));
		final Map<String, SidePot> result = gamePot.getAllPots();
		
		// Check total money in pots
		final double totalValue = result.values().stream()
									.mapToDouble(sp -> sp.getPotTotal())
									.sum();
		assertEquals(81.6, totalValue, 0.001);
		
		// Check equality with buy-ins
		final double totalStacks = players.stream()
									.mapToDouble( p -> p.getCurrentStack().getStack())
									.sum();
		assertEquals(500 * players.size(), totalStacks, 0.01);
		assertEquals(500 - 19.2 + 81.6 , pSa.getCurrentStack().getStack(), 0.01);
	}
	
	@Test
	public void testFinalPotsWithSplitsAndAllIn() {
		final TexasGameState[] allStates = new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.TURN, TexasGameState.RIVER};
		
		final Player pA = TestUtils.makePlayer("A", Arrays.copyOf(allStates, 1), mkBets(1.4f), new Card("3D"), new Card("JD"));// 1st winning hand
		final Player pC = TestUtils.makePlayer("C", Arrays.copyOf(allStates, 2), mkBets(2.6f, 1.77f), new Card("2H"), new Card("TD"));// 2nd best winning
		final Player pO = TestUtils.makePlayer("O", Arrays.copyOf(allStates, 3), mkBets(2.6f, 5.2f, 0.4f), new Card("2C"), new Card("9C"));// 3rd best winning
		final Player pM = TestUtils.makePlayer("M", allStates, mkBets(2.6f, 5.2f, 1f, 4.93f), new Card("TS"), new Card("TC"));//4th winning hand
		
		final Player pP = TestUtils.makePlayer("P", allStates, mkBets(2.6f, 5.2f, 1f, 4.93f), new Card("6H"), new Card("7H"));//nada
		
		final Player pG = TestUtils.makePlayer("G", Arrays.copyOf(allStates, 3), mkBets(2.6f, 5.2f, 1f), new Card("7S"), new Card("6D"));//folded
		final Player pHu = TestUtils.makePlayer("Hu", Arrays.copyOf(allStates, 1), mkBets(2.6f), new Card("AS"), new Card("3S"));//folded
		final Player pHa = TestUtils.makePlayer("Ha", Arrays.copyOf(allStates, 2), mkBets(2.6f, 2.6f), new Card("QS"), new Card("9H"));//folded
		pG.getState().setLastAction(PlayerActionType.FOLD);
		pHu.getState().setLastAction(PlayerActionType.FOLD);
		pHa.getState().setLastAction(PlayerActionType.FOLD);
		
		final Players players = new Players();
		players.addAll(Arrays.asList(pA, pM, pO, pC, pP, pG, pHu, pHa));
				
		List<Card> cardsOnTable = Card.makeCards("JC", "8D", "5D", "2D", "2S");
		final GamePots gamePot = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		final Map<String, SidePot> result = gamePot.getAllPots();
		
		assertEquals(4, result.size());
		// Check total money in pots
		final double totalValue = result.values().stream()
									.mapToDouble(sp -> sp.getPotTotal())
									.sum();
		assertEquals(58.03, totalValue, 0.001);
		
		// Check equality with buy-ins
		final double totalStacks = players.stream()
									.mapToDouble( p -> p.getCurrentStack().getStack())
									.sum();
		assertEquals(500 * players.size(), totalStacks, 0.01);
		
		// ---- Total wins -------- //
		assertEquals(500 - 1.4 + 11.2 , pA.getCurrentStack().getStack(), 0.01);
		assertEquals(500 - 2.6 + 8.4 - 1.77 + (6 * 1.77), pC.getCurrentStack().getStack(), 0.01);
		assertEquals(500 - 2.6 +  0  - 5.2 + 14.55 - 0.4 + 1.6, pO.getCurrentStack().getStack(), 0.01);
		assertEquals(500 - 2.6 +  0  - 5.2 +   0   -  1  + 1.8 - 4.93 + 9.86, pM.getCurrentStack().getStack(), 0.01);
		
	}
	
	@Test
	public void testSidePotsWithSplitsAndAllIn() {
		final TexasGameState[] allStates = new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.TURN, TexasGameState.RIVER};
		
		final Player pA = TestUtils.makePlayer("A", Arrays.copyOf(allStates, 1), mkBets(1.4f), 50);// 1st winning hand
		final Player pC = TestUtils.makePlayer("C", Arrays.copyOf(allStates, 2), mkBets(2.6f, 1.77f), 30);// 2nd best winning
		final Player pO = TestUtils.makePlayer("O", Arrays.copyOf(allStates, 3), mkBets(2.6f, 5.2f, 0.4f), 30);// 3rd best winning
		final Player pM = TestUtils.makePlayer("M", allStates, mkBets(2.6f, 5.2f, 1f, 4.93f), 20);// 4th winning hand
		final Player pP = TestUtils.makePlayer("P", allStates, mkBets(2.6f, 5.2f, 1f, 4.93f), 10);//nada
		
		final Player pG = TestUtils.makePlayer("G", Arrays.copyOf(allStates, 3), mkBets(2.6f, 5.2f, 1f), 0);//folded
		final Player pHu = TestUtils.makePlayer("Hu", Arrays.copyOf(allStates, 1), mkBets(2.6f), 0);//folded
		final Player pHa = TestUtils.makePlayer("Ha", Arrays.copyOf(allStates, 2), mkBets(2.6f, 2.6f), 0);//folded
		pG.getState().setLastAction(PlayerActionType.FOLD);
		pHu.getState().setLastAction(PlayerActionType.FOLD);
		pHa.getState().setLastAction(PlayerActionType.FOLD);
		
		List<Player> players = Arrays.asList(pA, pM, pO, pC, pP, pG, pHu, pHa);
		
		final List<SidePot> spDeal = evaluator.calculateSidePots(new GamePot(TexasGameState.POST_DEAL, players));
		final List<SidePot> spFlop = evaluator.calculateSidePots(new GamePot(TexasGameState.FLOP, players));
		final List<SidePot> spTurn = evaluator.calculateSidePots(new GamePot(TexasGameState.TURN, players));
		final List<SidePot> spRiver = evaluator.calculateSidePots(new GamePot(TexasGameState.RIVER, players));
		
		// Andy wins first pot
		assertEquals(8 * 1.4, spDeal.get(0).getPotTotal(), 0.01);// 8 x £1.4
		assertEquals("A", spDeal.get(0).getPotWinners().get(0).getPlayerId());
		// The remainder of first pot
		assertEquals((19.6 - 11.20), spDeal.get(1).getPotTotal(), 0.01);
		assertEquals(2, spDeal.get(1).getPotWinners().size());

		// Chris and Oli split second pot (based on Chris all-in)
		assertEquals(6 * 1.77, spFlop.get(0).getPotTotal(), 0.01);
		assertEquals(2, spFlop.get(0).getPotWinners().size());
		// The remainder of the second pot
		assertEquals(25.17 - (6 * 1.77), spFlop.get(1).getPotTotal(), 0.01);
		assertEquals("O", spFlop.get(1).getPotWinners().get(0).getPlayerId());
		
		// Oli takes first part of turn pot
		assertEquals(0.4 * 4, spTurn.get(0).getPotTotal(), 0.01);
		assertEquals("O", spTurn.get(0).getPotWinners().get(0).getPlayerId());
		// Remainder of turn pot
		assertEquals(3.4 - (0.4 * 4), spTurn.get(1).getPotTotal(), 0.01);
		assertEquals("M", spTurn.get(1).getPotWinners().get(0).getPlayerId());
		
		// Mike takes river pot
		assertEquals(9.86, spRiver.get(0).getPotTotal(), 0.01);
		assertEquals("M", spRiver.get(0).getPotWinners().get(0).getPlayerId());

	}
	
	private float[] mkBets(float... vals) {
		return vals;
	}
	
	@Test
	public void testSidePotWithFourMatchingBets() {
		
		// Given
		 final TexasGameState state = TexasGameState.TURN;
		// A goes all-in on turn with full-house
		final Player A = TestUtils.makePlayer("A", 
				new TexasGameState[] {state},
				new float[] {10f},
				new Card("5H"), new Card("QC"));

		// D All-in on TURN with matching two-pair
		final Player D = TestUtils.makePlayer("D",
				new TexasGameState[] {state},
				new float[] {10f},
				new Card("3C"), new Card("QH"));
		
		// B bets on TURN with two-pair
		final Player B = TestUtils.makePlayer("B",
				new TexasGameState[] {state},
				new float[] {10f},
				new Card("3H"), new Card("QD"));

		// C calls on TURN with lower two-pair
		final Player C = TestUtils.makePlayer("C",
				new TexasGameState[] {state},
				new float[] {10f},
				new Card("8H"), new Card("JC"));
		A.setRankedHand(new HandRank(300, PokerHand.FULL_HOUSE, null));
		B.setRankedHand(new HandRank(200, PokerHand.TWO_PAIR, null));
		C.setRankedHand(new HandRank(10, PokerHand.HIGH_CARD, null));
		D.setRankedHand(new HandRank(200, PokerHand.TWO_PAIR, null));

		// When
		final List<SidePot> pots = evaluator.calculateSidePots(new GamePot(state, Arrays.asList(A, B, C, D)));
		
		//total pot = 40 with 1 winner
		Assert.assertEquals(1, pots.size());
		SidePot one = pots.get(0);
		
		assertEquals(40, one.getPotTotal(), 0.1);// 4 - 1 from each player
		
		assertEquals(1, one.getPotWinners().size());
		
		Assert.assertEquals(300, one.getMaxRank().getRankValue(), 0.01);

	}
	
	@Test
	public void testSidePotWithThreeDifferentBets() {
		// Given
		 final TexasGameState state = TexasGameState.TURN;
		// A goes all-in on turn with full-house
		final Player A = TestUtils.makePlayer("A", 
				new TexasGameState[] {state},
				new float[] {1f},
				new Card("5H"), new Card("QC"));

		// D All-in on TURN with matching two-pair
		final Player D = TestUtils.makePlayer("D",
				new TexasGameState[] {state},
				new float[] {5f},
				new Card("3C"), new Card("QH"));
		
		// B bets on TURN with two-pair
		final Player B = TestUtils.makePlayer("B",
				new TexasGameState[] {state},
				new float[] {10f},
				new Card("3H"), new Card("QD"));

		// C calls on TURN with lower two-pair
		final Player C = TestUtils.makePlayer("C",
				new TexasGameState[] {state},
				new float[] {10f},
				new Card("8H"), new Card("JC"));
		A.setRankedHand(new HandRank(300, PokerHand.FULL_HOUSE, null));
		B.setRankedHand(new HandRank(200, PokerHand.TWO_PAIR, null));
		C.setRankedHand(new HandRank(10, PokerHand.HIGH_CARD, null));
		D.setRankedHand(new HandRank(200, PokerHand.TWO_PAIR, null));
		
		// When
		final List<SidePot> pots = evaluator.calculateSidePots(new GamePot(state, Arrays.asList(A, B, C, D)));
		
		//total pot = 106
		// Pot 1 = 4 (4) - 1 from each player
		// Pot 2 = 4, +4 +4 (12) - 4 from each remaining player
		// Pot 3 = 5 + 5 (10) - 5 from each remaining player
		assertEquals(3, pots.size());
		SidePot one = pots.get(0);
		SidePot two = pots.get(1);
		SidePot three = pots.get(2);
		
		assertEquals(4, one.getPotTotal(), 0.1);// 4 - 1 from each player
		assertEquals(12, two.getPotTotal(), 0.1); // 4 bet, +4 +4 (12) - 4 from each remaining player
		assertEquals(10, three.getPotTotal(), 0.1); // 5 bet + 5 (10) - 5 from each remaining player
		
		assertEquals(1, one.getPotWinners().size());
		assertEquals(2, two.getPotWinners().size());
		assertEquals(1, three.getPotWinners().size());
		
		Assert.assertEquals(300d, one.getMaxRank().getRankValue(), 0.01);
		Assert.assertEquals(200d, two.getMaxRank().getRankValue(), 0.01);
		Assert.assertEquals(200d, three.getMaxRank().getRankValue(), 0.01);

	}
	
	@Test
	public void testWhenLowestPotWinsOthersRefunded() {
		// A goes all-in on turn with full-house
		final Player A = TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.TURN},
				new float[] {10f, 10f, 1f},
				new Card("5H"), new Card("QC"));

		// D All-in on TURN with matching two-pair
		final Player D = TestUtils.makePlayer("D",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.TURN},
				new float[] {10f, 10f, 5f},
				new Card("3C"), new Card("QH"));
		
		// B bets on TURN with two-pair
		final Player B = TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.TURN},
				new float[] {10f, 10f, 10f},
				new Card("3H"), new Card("QD"));

		// C calls on TURN with lower two-pair
		final Player C = TestUtils.makePlayer("C",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.TURN},
				new float[] {10f, 10f, 10f},
				new Card("8H"), new Card("JC"));
		
		final Players players = new Players();
		players.addAll(Arrays.asList(A, B, C, D));
				
		List<Card> cardsOnTable = Card.makeCards("3D", "5S", "5D", "8C", "QS");
		final GamePots gamePot = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		final Map<String, SidePot> result = gamePot.getAllPots();
		
		// A takes deal
		SidePot deal = result.get(GamePots.FIRST_POT_NAME);
		Assert.assertNotNull(deal.getPotWinners());
		Assert.assertEquals(1, deal.getPotWinners().size());
		assertEquals("A", deal.getPotWinners().get(0).getPlayerId());
		Assert.assertEquals(84f, deal.getPotTotal(), 0.01);
		
		// A and B share the turn as B out-bet A 
		SidePot turn = result.get("Pot A");
		Assert.assertNotNull(turn.getPotWinners());
		Assert.assertEquals(2, turn.getPotWinners().size());
		Assert.assertEquals(12f, turn.getPotTotal(), 0.01);
		
		SidePot river = result.get("Pot B");
		Assert.assertNotNull(river.getPotWinners());
		Assert.assertEquals(1, river.getPotWinners().size());
		Assert.assertEquals(10f, river.getPotTotal(), 0.01);
		
		//total pot = 106
		// Pot 1 = 4 (4) - 1 from each player
		// Pot 2 = 4, +4 +4 (12) - 4 from each remaining player
		// Pot 3 = 5 + 5 (10) - 5 from each remaining player
		
		// So A wins 4, C wins 12/2=6, B wins 12/2=6+10 = 16
		
		// Check pots (stack - total bet + win per round)
		Assert.assertEquals(500 - 21 + 80 + 4, A.getCurrentStack().getStack(), 0.01);// A wins deal and flop and (£4) from turn
		Assert.assertEquals(500 - 30 + 16, B.getCurrentStack().getStack(), 0.01); // B wins half pot 2 + all pot 3 (£16)
		Assert.assertEquals(500 - 30, C.getCurrentStack().getStack(), 0.01);// C gets nada
		Assert.assertEquals(500 - 25 + 6, D.getCurrentStack().getStack(), 0.01);// D only wins half his side pot (£6)
		
		
		// Check equality with buy-ins
		final double totalStacks = players.stream()
									.mapToDouble( p -> p.getCurrentStack().getStack())
									.sum();
		assertEquals(500 * players.size(), totalStacks, 0.01);

	}
	
	@Test
	public void testPlayerSittingOutDoesntEffectOutcome() {
		// Given
		// A has two pair
		final Player pA = TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card("TD"), new Card("QH"));
		
		// B has flush
		final Player pB = TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card("5H"), new Card("QC"));
		
		// C joined late so sitting out
		final Player pC = TestUtils.makePlayer("C",
				new TexasGameState[] {TexasGameState.FLOP},
				new float[] {0f},
				null, null);
		pC.getState().initialise(TexasGameState.FLOP, mock(GameSettings.class));
		
		assertTrue(pC.getState().isSittingOut());
		final Players players = new Players();
		players.addAll(Arrays.asList(pA, pB, pC));
		
		List<Card> cardsOnTable = Card.makeCards("3D", "5S", "5D", "8C", "QS");
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		SidePot flop = result.getAllPots().get(GamePots.FIRST_POT_NAME);
		Assert.assertNotNull(flop.getPotWinners());
		Assert.assertEquals(1, flop.getPotWinners().size());
		Assert.assertEquals("B",flop.getPotWinners().get(0).getPlayerId());
		assertEquals(40f, flop.getPotTotal(), 0.1);
		
		assertEquals(480f, pA.getCurrentStack().getStack(), 0.1);
		assertEquals(520f, pB.getCurrentStack().getStack(), 0.1);
		assertEquals(500f, pC.getCurrentStack().getStack(), 0.1);

		// Check equality with buy-ins
		final double totalStacks = players.stream()
									.mapToDouble( p -> p.getCurrentStack().getStack())
									.sum();
		assertEquals(500 * players.size(), totalStacks, 0.01);

	}
	
	@Test
	public void testRealWorldFullHouseVsTwoPair() {
		// Given
		Players players = new Players();
		// A goes all-in on deal
		players.add(TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card("TD"), new Card("QH"))
				);
		// B goes all-in on FLOP with straight
		players.add(TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card("5H"), new Card("QC"))
				);
		
		List<Card> cardsOnTable = Card.makeCards("3D", "5S", "5D", "8C", "QS");
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		SidePot flop = result.getAllPots().get(GamePots.FIRST_POT_NAME);
		Assert.assertNotNull(flop.getPotWinners());
		Assert.assertEquals(1, flop.getPotWinners().size());
		Assert.assertEquals("B",flop.getPotWinners().get(0).getPlayerId());
		assertEquals(40f, flop.getPotTotal(), 0.1);
	}
	
	@Test
	public void testSplitPotsWithTwoWinnersForOnePot() {
		
		
		// Given
		// Player C wins with a straight, but player B has a flush on the flop side-pot
		Players players = new Players();
		// A goes all-in on deal
		players.add(TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(TEN, Clubs), new Card(TWO, Diamonds))
				);
		// B goes all-in on FLOP with straight
		players.add(TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card(SEVEN, Clubs), new Card(EIGHT, Clubs))
				);
		// Last two players are in to the end. C has equal straight to B
		players.add(TestUtils.makePlayer("C", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(SEVEN, Hearts), new Card(EIGHT, Spades))
				);
		// 
		players.add(TestUtils.makePlayer("D",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(KING, Clubs), new Card(TEN, Spades))
				);
		
		List<Card> cardsOnTable = Arrays.asList(new Card[] {
				new Card(FOUR, Clubs), 
				new Card(FIVE, Diamonds),
				new Card(SIX, Clubs),
				new Card(KING, Diamonds),
				new Card(TEN, Hearts)
				} );

		long start = System.currentTimeMillis();
		
		// When
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		System.out.println("---- Pot calc: " + (System.currentTimeMillis() - start) + "ms -----");
		
		// Then
		// all player bet the same on the same round, therefore player C takes all
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("A").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.STRAIGHT, players.getPlayerById("B").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.STRAIGHT, players.getPlayerById("C").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.TWO_PAIR, players.getPlayerById("D").getRankedHand().getRankName());
		
		final SidePot potA = result.getAllPots().get("Pot A");
		final SidePot potMain = result.getAllPots().get("Main Pot");
		SidePot river = potMain;
		SidePot flop = potA;
		if (potA.getPotWinners().size()==1) {
			river = potA;
			flop = potMain;
		}
		
		Assert.assertNotNull(flop.getPotWinners());
		Assert.assertEquals(2, flop.getPotWinners().size());
		Assert.assertEquals("B",flop.getPotWinners().get(0).getPlayerId());
		Assert.assertEquals("C",flop.getPotWinners().get(1).getPlayerId());
		assertEquals(70f, flop.getPotTotal(), 0.1);
		
		Assert.assertNotNull(river.getPotWinners());
		Assert.assertEquals(1, river.getPotWinners().size());
		Assert.assertEquals("C",river.getPotWinners().get(0).getPlayerId());
		assertEquals(20f, river.getPotTotal(), 0.1);
		
		// Player B won half the first and second side pots as drew with C - Pot total = 60 / 2 = 30
		Assert.assertEquals((500d - 20d) + 30d + 5d, players.getPlayerById("B").getCurrentStack().getStack(), 0.1);
		// C only won the same as player B in the first two pots, and all of the last pot
		Assert.assertEquals((500d - 30d) + 30d + 20d +5, players.getPlayerById("C").getCurrentStack().getStack(), 0.1);
	}
	
	@Test
	public void testSplitPotsWithOneWinnerEachPot() {
		
		
		// Given
		// Player C wins with a straight, but player B has a flush on the flop side-pot
		Players players = new Players();
		// A goes all-in on deal
		players.add(TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(TEN, Hearts), new Card(TWO, Diamonds))
				);
		// B goes all-in on FLOP with a flush
		players.add(TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card(JACK, Clubs), new Card(QUEEN, Clubs))
				);
		// Last two players are in to the end. C has a straight so wins over D
		players.add(TestUtils.makePlayer("C", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(SEVEN, Clubs), new Card(EIGHT, Spades))
				);
		players.add(TestUtils.makePlayer("D",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(KING, Clubs), new Card(TEN, Spades))
				);
		
		List<Card> cardsOnTable = Arrays.asList(new Card[] {
				new Card(FOUR, Clubs), 
				new Card(FIVE, Clubs),
				new Card(SIX, Clubs),
				new Card(KING, Diamonds),
				new Card(TEN, Diamonds)
				} );

		long start = System.currentTimeMillis();
		
		// When
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		System.out.println("---- Pot Calc: " + (System.currentTimeMillis() - start) + "ms -----");
		
		// Then
		// all player bet the same on the same round
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("A").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.FLUSH, players.getPlayerById("B").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.STRAIGHT, players.getPlayerById("C").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.TWO_PAIR, players.getPlayerById("D").getRankedHand().getRankName());
		
		SidePot flop = result.getAllPots().get(GamePots.FIRST_POT_NAME);
		Assert.assertNotNull(flop.getPotWinners());
		Assert.assertEquals(1, flop.getPotWinners().size());
		
		SidePot river = result.getAllPots().get("Pot A");
		Assert.assertNotNull(river.getPotWinners());
		Assert.assertEquals(1, river.getPotWinners().size());
		
		// Player B won all the pots up to and including the flop
		Assert.assertEquals(500d + 50d, players.getPlayerById("B").getCurrentStack().getStack(), 0.1);
		// C only won 10 off of D and lost 2 to B in previous rounds
		Assert.assertEquals((500d + 10d) - 20d, players.getPlayerById("C").getCurrentStack().getStack(), 0.1);
	}

	@Test
	public void testSplitPotOnePlayerFolded() {

		// Given
		Players players = new Players();
		// A Folds in deal
		players.add(TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card[] {null})
				);
		players.getPlayerById("A").getState().setLastAction(PlayerActionType.FOLD);
		
		// Pair on the table
		players.add(TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card(SEVEN, Hearts), new Card(EIGHT, Hearts))
				);
		// Last two players are in to the end
		players.add(TestUtils.makePlayer("C", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(TWO, Clubs), new Card(SIX, Spades))
				);

		
		List<Card> cardsOnTable = Arrays.asList(new Card[] {
				new Card(FIVE, Spades), 
				new Card(TEN, Spades),
				new Card(SIX, Clubs),
				new Card(FIVE, Hearts),
				new Card(TWO, Hearts)
				} );

		// When
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		// Then
		// all player bet the same on the same round, therefore player C takes all
		Assert.assertNull(players.getPlayerById("A").getRankedHand());
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("B").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.TWO_PAIR, players.getPlayerById("C").getRankedHand().getRankName());
		
		// Check C won all three pots
		Assert.assertEquals("C", result.getWinners().get(0).getPlayerId());
		assertEquals(60f, result.getAllPots().get(GamePots.FIRST_POT_NAME).getPotTotal(), 0.1);
		
		// Check the players stack is original stack + won off other players, including the fold
		Assert.assertEquals(500d + 30d, players.getPlayerById("C").getCurrentStack().getStack(), 0.1);
		
	}
	
	@Test
	public void testSplitPotsButOneWinner() {

		// Given
		// Player C wins with a straight
		Players players = new Players();
		// A goes all-in on deal
		players.add(TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(TEN, Clubs), new Card(TWO, Diamonds))
				);
		// B goes all-in on FLOP
		players.add(TestUtils.makePlayer("B",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP},
				new float[] {10f, 10f},
				new Card(JACK, Hearts), new Card(JACK, Diamonds))
				);
		// Last two players are in to the end
		players.add(TestUtils.makePlayer("C", 
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(SEVEN, Spades), new Card(EIGHT, Spades))
				);
		players.add(TestUtils.makePlayer("D",
				new TexasGameState[] {TexasGameState.POST_DEAL, TexasGameState.FLOP, TexasGameState.RIVER},
				new float[] {10f, 10f, 10f},
				new Card(KING, Clubs), new Card(TEN, Spades))
				);
		
		List<Card> cardsOnTable = Arrays.asList(new Card[] {
				new Card(FOUR, Clubs), 
				new Card(FIVE, Clubs),
				new Card(SIX, Clubs),
				new Card(KING, Diamonds),
				new Card(TEN, Hearts)
				} );

		// When
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		// Then
		// all player bet the same on the same round, therefore player C takes all
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("A").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("B").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.STRAIGHT, players.getPlayerById("C").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.TWO_PAIR, players.getPlayerById("D").getRankedHand().getRankName());
		
		// Check C won all three pots
		Assert.assertEquals("C", result.getWinners().get(0).getPlayerId());
		assertEquals(90f, result.getAllPots().get(GamePots.FIRST_POT_NAME).getPotTotal(), 0.1);
		
		// Check the players stack is original stack + won off other players
		Assert.assertEquals(500d + 60d, players.getPlayerById("C").getCurrentStack().getStack(), 0.1);
		
	}

	@Test
	public void testOneOfThreePlayersRevealOnRiver() {
		final Player pA = TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.RIVER}, 
				new float[] {0.5f}, 
				new Card("2H"), new Card("6H"));
		final Player pB = TestUtils.makePlayer("B", 
				new TexasGameState[] {TexasGameState.RIVER}, 
				new float[] {0.5f}, 
				new Card("3C"), new Card("JD"));
		final Player pC = TestUtils.makePlayer("C", 
				new TexasGameState[] {TexasGameState.RIVER}, 
				new float[] {0f}, 
				new Card("AC"), new Card("7S"));
		final Players players = new Players();
		players.addAll(Arrays.asList(pA, pB, pC));
		
		final List<Card> cardsOnTable = Card.makeCards("JH","7D","KS","2S","AS");
		
		// Player C reveals
		PlayerAction rev = mock(PlayerActionMessage.class);
		Mockito.when(rev.getAction()).thenReturn(PlayerActionType.REVEAL);
		pC.doPlayerAction(rev, TexasGameState.RIVER, mock(GameSettings.class), 50);
		
		// When
		final GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		// Then
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("A").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("B").getRankedHand().getRankName());
		Assert.assertEquals("B", result.getWinners().get(0).getPlayerId());
		assertEquals(1f, result.getAllPots().get(GamePots.FIRST_POT_NAME).getPotTotal(), 0.1);
	}
	
	@Test
	public void testFourEqualPotsInSameRound() {

		// Given
		// Player C wins with a straight, all player bet the same in same round
		Players players = new Players();
		players.add(TestUtils.makePlayer("A", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(TEN, Hearts), new Card(TWO, Diamonds))
				);
		players.add(TestUtils.makePlayer("B", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(JACK, Hearts), new Card(JACK, Diamonds))
				);
		players.add(TestUtils.makePlayer("C", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(SEVEN, Clubs), new Card(EIGHT, Spades))
				);
		players.add(TestUtils.makePlayer("D", 
				new TexasGameState[] {TexasGameState.POST_DEAL},
				new float[] {10f},
				new Card(KING, Clubs), new Card(TEN, Spades))
				);
		
		List<Card> cardsOnTable = Arrays.asList(new Card[] {
				new Card(FOUR, Clubs), 
				new Card(FIVE, Clubs),
				new Card(SIX, Clubs),
				new Card(KING, Diamonds),
				new Card(TEN, Diamonds)
				} );

		long start = System.currentTimeMillis();
		
		// When
		GamePots result = evaluator.calculatePotsAndWinners(players, cardsOnTable);
		
		// Then
		// all player bet the same on the same round, therefore player C takes all
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("A").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.PAIR, players.getPlayerById("B").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.STRAIGHT, players.getPlayerById("C").getRankedHand().getRankName());
		Assert.assertEquals(PokerHand.TWO_PAIR, players.getPlayerById("D").getRankedHand().getRankName());
		
		// Main pot
		final SidePot cResult = result.getAllPots().get(GamePots.FIRST_POT_NAME);
		Assert.assertNotNull(cResult);
		Assert.assertNotNull(cResult.getPotWinners());
		Assert.assertEquals(1, cResult.getPotWinners().size());
		// C wins overall, therefore takes the main pot (the total amount)
		Assert.assertEquals(40d, cResult.getPotTotal(), 0.01);
		
		long end = System.currentTimeMillis() - start;
		
		System.out.println("---- " + end + "ms -----");
	}

}