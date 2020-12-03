package edu.kit.ipd.are.agentanalysis.evaluator;

import edu.kit.ipd.are.agentanalysis.port.hypothesis.IHypothesesSet;

/**
 * The configuration for the evaluator.
 *
 * @author Dominik Fuchss
 *
 */
public final class Configuration {
	private Configuration() {
		throw new IllegalAccessError();
	}

	/**
	 * The maximum amount of hypotheses selected from pseudo hypotheses.
	 */
	public static int maxHypothesesPerPseudoHypothesis = 3;
	/**
	 * The maximum amount of hypotheses per {@link IHypothesesSet} in a leaf.
	 */
	public static int maxHypothesesForLeaf = 1;
	/**
	 * Indicates whether the evaluator shall skip values beneath a threshold (or
	 * {@code null} if not intended) for leaves.
	 */
	public static Double skipConfidenceLessEqualForLeaf = null; // 0.0;

	/**
	 * Indicates whether paths shall be stored.
	 */
	public static boolean storePaths = false;

}
