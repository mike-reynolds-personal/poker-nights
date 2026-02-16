package com.langleydata.homepoker.game.texasHoldem.pots;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.langleydata.homepoker.game.texasHoldem.Winner;


/** A concise view of the game pots (all side pots for all rounds)
 * primarily for display purposes but is also persisted to DB
 * 
 * @author Mike Reynolds
 *
 */
public class GamePots {
	public static final String FIRST_POT_NAME = "Main Pot";
	private Map<String, SidePot> allPots = new HashMap<>();
	final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	@Transient
	private transient Map<Integer, String> lookup = new HashMap<>();
	
	/**
	 * @return the allPots
	 */
	@JsonIgnore
	public Map<String, SidePot> getAllPots() { 
		return allPots;
	}

	@JsonProperty
	Collection<SidePot> getPots() {
		return allPots.values();
	}
	
	/** Add side pots for a specific round
	 * 
	 * @param sidePots
	 */
	public void addSidePots(final List<SidePot> sidePots) {
		sidePots.forEach(this::addSidePot);
	}

	/** Add a side pot for a specific round
	 * 
	 * @param newPot The new pot to add
	 */
	public void addSidePot(SidePot newPot) {
		final int hash = newPot.getPotWinners().hashCode();
		String potName = lookup.get(hash);
		
		if (potName==null) {
			potName = getPotName();
			lookup.put(hash, potName);
		}
		
		SidePot cPot = allPots.get(potName);
		
		if (cPot==null) {
			SidePot sp = new SidePot();
			sp.setName(potName);
			sp.setPotTotal(newPot.getPotTotal());
			// Don't use the method that filters on rank, as already done
			sp.addAllWinners(newPot.getPotWinners());
			sp.setMaxRank(newPot.getMaxRank());
			allPots.put(potName, sp);
		} else {
			cPot.setPotTotal(cPot.getPotTotal() + newPot.getPotTotal());
		}
	}
	
	private String getPotName() {
		if (lookup.size()==0) {
			return FIRST_POT_NAME;
		}
		return "Pot " + ALPHABET[lookup.size() - 1];
	}

	/** Get a distinct list of all winners
	 * 
	 * @return
	 */
	@JsonIgnore
	public List<Winner> getWinners() {
		return allPots.values().stream()
				.map(sp -> sp.getPotWinners())
				.flatMap(List::stream)
				.distinct()
				.map(Winner::new)
				.collect(Collectors.toList());
	}
	
	/** Clear down the pots and lookup
	 * 
	 */
	public void clear() {
		allPots = new HashMap<>();
		lookup = new HashMap<>();
	}
}
