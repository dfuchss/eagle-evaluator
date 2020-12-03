package edu.kit.ipd.are.agentanalysis.evaluator;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The different possibilities for classification of hypotheses.
 *
 * @author Dominik Fuchss
 *
 */
public enum Classification {
	/**
	 * Correct classified.
	 */
	CORRECT(true, "c", 2),
	/**
	 * Mostly correct classified.
	 */
	RATHER_CORRECT(null, "rc", 1),

	/**
	 * Mostly wrong classified.
	 */
	RATHER_WRONG(null, "rw", -1),
	/**
	 * Wrong classified.
	 */
	WRONG(false, "w", -2);

	/**
	 * Get the question for all classificators.
	 */
	public static final String QUESTION = String.join(", ", Arrays.stream(Classification.values()).map(c -> c + " (" + c.id + ")").collect(Collectors.toList()));

	private Boolean good;
	private String id;
	private int value;

	Classification(Boolean good, String id, int value) {
		this.good = good;
		this.id = id;
		this.value = value;
	}

	/**
	 * Indicator whether hypotheses is good.
	 *
	 * @param cls the classification
	 * @return tri-boolean: true, undecided (null), false
	 */
	public static Boolean isGood(Classification cls) {
		return cls == null ? null : cls.good;
	}

	/**
	 * Get classification by string.
	 *
	 * @param str the string
	 * @return the classification or {@code null} iff no match
	 */
	public static Classification getByString(String str) {
		for (Classification cls : Classification.values()) {
			if (cls.id.equals(str)) {
				return cls;
			}
		}
		return null;
	}

	/**
	 * Get the int value of classification,
	 *
	 * @return the int value
	 */
	public int getValue() {
		return this.value;
	}

	/**
	 * Get by value.
	 *
	 * @param value the value
	 * @return the classification
	 */
	public static Classification byValue(int value) {
		for (Classification cls : Classification.values()) {
			if (cls.value == value) {
				return cls;
			}
		}
		throw new IllegalArgumentException("Illegal Value: " + value);
	}
}
