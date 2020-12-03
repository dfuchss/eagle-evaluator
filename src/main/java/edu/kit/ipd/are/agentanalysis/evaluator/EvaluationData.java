package edu.kit.ipd.are.agentanalysis.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.kit.ipd.are.agentanalysis.port.hypothesis.IHypothesis;
import edu.kit.ipd.are.agentanalysis.port.xplore.dto.HypothesisDTO;

/**
 * Contains the user choices whether a hypotheses is good or bad.
 *
 * @author Dominik Fuchss
 *
 */
public final class EvaluationData {

	private List<Map<Classification, List<HypothesisDTO>>> classificationPerLayer;

	private EvaluationData() {
		this.classificationPerLayer = new ArrayList<>();
	}

	/**
	 * Create new EvaluationData by amount of layers.
	 *
	 * @param layers the amount of layers
	 */
	public EvaluationData(int layers) {
		this();
		for (int i = 0; i < layers; i++) {
			this.classificationPerLayer.add(new EnumMap<>(Classification.class));
		}
	}

	/**
	 * Classify a hypothesis according to the current data.
	 *
	 * @param layer      the layer of the hypothesis
	 * @param hypothesis the hypothesis
	 * @return the classification, {@code null} iff unknown
	 */
	public Classification getClassification(int layer, HypothesisDTO hypothesis) {
		HypothesisDTO copy = new HypothesisDTO(hypothesis);
		copy.setConfidence(Double.NaN);

		for (Classification cls : Classification.values()) {
			var hyps = this.classificationPerLayer.get(layer).get(cls);
			if (hyps != null && hyps.contains(copy)) {
				return cls;
			}
		}

		return null;

	}

	/**
	 * Find a classified hypothesis which is similar to the provided hypothesis.
	 *
	 * @param layer      the layer of the hypothesis
	 * @param hypothesis the hypothesis
	 * @return a similar (identified by {@link HypothesisDTO#getValue()}) hypothesis
	 *         or {@code null} iff none exist
	 */
	public HypothesisDTO findSimilar(int layer, HypothesisDTO hypothesis) {
		for (var h : this.classificationPerLayer.get(layer).values().stream().flatMap(List::stream).collect(Collectors.toList())) {
			if (Objects.equals(h.getValue(), hypothesis.getValue())) {
				return h;
			}
		}
		return null;
	}

	/**
	 * Provide classification information on a hypothesis.
	 *
	 * @param layer          the layer of the hypothesis
	 * @param hypothesis     the hypothesis which shall be classified.
	 * @param classification the classification
	 */
	public void setClassification(int layer, HypothesisDTO hypothesis, Classification classification) {
		List<HypothesisDTO> hyps = this.classificationPerLayer.get(layer).get(classification);
		if (hyps == null) {
			this.classificationPerLayer.get(layer).put(classification, hyps = new ArrayList<>());
		}

		HypothesisDTO copy = new HypothesisDTO(hypothesis);
		copy.setConfidence(Double.NaN);

		hyps.add(copy);
	}

	/**
	 * Count the total amount of hypothesis (distinct by value).
	 *
	 * @param hypotheses the hypotheses
	 * @return the total amount of incorrect hypothesis
	 */
	public static int countHypotheses(Collection<? extends IHypothesis> hypotheses) {
		return EvaluationData.countHypotheses(hypotheses.stream());
	}

	/**
	 * Count the total amount of hypothesis (distinct by value).
	 *
	 * @param hypotheses the hypotheses
	 * @return the total amount of incorrect hypothesis
	 */
	public static int countHypotheses(Stream<? extends IHypothesis> hypotheses) {
		return hypotheses.collect(Collectors.groupingBy(IHypothesis::getValue)).size();
	}

	/**
	 * Count the total amount of correct hypothesis (distinct by value).
	 *
	 * @param layer the layer of the hypothesis
	 * @return the total amount of correct hypothesis
	 */
	public int getGoodHypothesesCount(int layer) {
		return EvaluationData
				.countHypotheses(this.classificationPerLayer.get(layer).entrySet().stream().filter(e -> Classification.isGood(e.getKey()) == Boolean.TRUE).flatMap(e -> e.getValue().stream()));
	}

	/**
	 * Count the total amount of incorrect hypothesis (distinct by value).
	 *
	 * @param layer the layer of the hypothesis
	 * @return the total amount of incorrect hypothesis
	 */
	public int getBadHypothesesCount(int layer) {
		return EvaluationData
				.countHypotheses(this.classificationPerLayer.get(layer).entrySet().stream().filter(e -> Classification.isGood(e.getKey()) == Boolean.FALSE).flatMap(e -> e.getValue().stream()));
	}

	int getNumberOfLayers() {
		return this.classificationPerLayer.size();
	}

	/**
	 * For evaluation only.
	 *
	 * @return the internal data
	 */
	public List<Map<Classification, List<HypothesisDTO>>> readClassificationPerLayer() {
		return this.classificationPerLayer;
	}
}
