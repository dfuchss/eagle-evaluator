package edu.kit.ipd.eagle.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fuchss.tools.tuple.Tuple2;
import org.fuchss.tools.tuple.Tuple3;

import edu.kit.ipd.eagle.port.hypothesis.HypothesisRange;
import edu.kit.ipd.eagle.port.hypothesis.IHypothesesSet;
import edu.kit.ipd.eagle.port.hypothesis.IHypothesis;
import edu.kit.ipd.eagle.port.util.Serialize;
import edu.kit.ipd.eagle.port.xplore.IExplorationResult;
import edu.kit.ipd.eagle.port.xplore.dto.ExplorationResultDTO;
import edu.kit.ipd.eagle.port.xplore.dto.HypothesisDTO;
import edu.kit.ipd.eagle.port.xplore.layer.ILayerEntry;

/**
 * Defines the evaluator of multiple (or a single) exploration results.
 *
 * @author Dominik Fuchss
 *
 */
public class Evaluator {

	private IExplorationResult explorationResult;

	private File evaluationResultFile;
	private EvaluationData evaluationData;

	private Queue<Tuple3<Integer, IHypothesesSet, HypothesisDTO>> remainingHypotheses;

	/**
	 * Create a new Evaluator.
	 *
	 * @param explorationResultFile the exploration result for evaluation (may be
	 *                              {@code null} iff eval file exists)
	 * @param evalFile              the evaluation file (may be {@code null} then
	 *                              the file will be generated by the
	 *                              explorationResultFile)
	 * @throws IOException iff deserialization of exploration or evaluation file
	 *                     fails
	 */
	public Evaluator(File explorationResultFile, File evalFile) throws IOException {
		if (explorationResultFile == null && evalFile == null) {
			throw new IllegalArgumentException("At least evalFile has to be != null");
		}

		if (explorationResultFile != null) {
			this.explorationResult = ExplorationResultDTO.load(explorationResultFile);
		}
		int layers = evalFile != null && evalFile.exists() ? -1 : this.findNumOfLayers();
		this.createEvaluationResult(layers, evalFile != null ? evalFile : new File(explorationResultFile.getAbsolutePath() + ".eval.json"));
		this.loadHypotheses();
	}

	int findNumOfLayers() {
		if (this.evaluationData != null) {
			return this.evaluationData.getNumberOfLayers();
		}
		int depth = 1;
		var step = this.explorationResult.getExplorationRoot();
		while (!step.getChildren().isEmpty()) {
			depth++;
			step = step.getChildren().get(0);
		}
		return depth;
	}

	/**
	 * Find next hypothesis to classify.
	 *
	 * @return the next hypothesis (as well as the layer (starting at 0), and word
	 *         iff {@link HypothesisRange#ELEMENT}) for classification or {@code null}
	 *         iff no further classification is needed
	 */
	public Tuple3<Integer, HypothesisDTO, String> findNextHypothesis() {

		while (!this.remainingHypotheses.isEmpty()) {
			var possibleNextTuple = this.remainingHypotheses.poll();
			int layer = possibleNextTuple.getFirst();
			IHypothesesSet hypothesesSet = possibleNextTuple.getSecond();
			String word = hypothesesSet.getHypothesesRange() == HypothesisRange.ELEMENT ? hypothesesSet.getElementOfHypotheses() : null;
			HypothesisDTO possibleNext = possibleNextTuple.getThird();

			Classification classification = this.evaluationData.getClassification(layer, possibleNext);
			if (classification != null) {
				continue;
			}

			HypothesisDTO similar = this.evaluationData.findSimilar(layer, possibleNext);
			if (similar != null) {
				this.evaluationData.setClassification(layer, possibleNext, this.evaluationData.getClassification(layer, similar));
				continue;
			}
			return Tuple3.of(layer, possibleNext, word);
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
		this.evaluationData.setClassification(layer, hypothesis, classification);
	}

	/**
	 * Save the current evaluation to the evaluation file.
	 *
	 * @throws IOException iff serialization was not successful
	 */
	public void save() throws IOException {
		Serialize.getObjectMapper(true).writeValue(this.evaluationResultFile, this.evaluationData);
	}

	private void loadHypotheses() {
		if (this.explorationResult == null) {
			return;
		}
		List<Tuple3<Integer, IHypothesesSet, HypothesisDTO>> hypothesis = new ArrayList<>();
		this.loadHypotheses(0, hypothesis, this.explorationResult.getExplorationRoot());
		this.remainingHypotheses = new LinkedList<>(hypothesis);
	}

	private void loadHypotheses(int layer, List<Tuple3<Integer, IHypothesesSet, HypothesisDTO>> hypotheses, ILayerEntry step) {
		var newHypotheses = step.getHypotheses();
		if (newHypotheses != null) {

			newHypotheses.stream().forEach(hs -> this.loadFromHypothesesSet(layer, hypotheses, hs));
		}
		for (var child : step.getChildren()) {
			this.loadHypotheses(layer + 1, hypotheses, child);
		}
	}

	private void loadFromHypothesesSet(int layer, List<Tuple3<Integer, IHypothesesSet, HypothesisDTO>> hypotheses, IHypothesesSet hs) {
		hs.getHypotheses().stream().map(h -> (HypothesisDTO) h).forEach(h -> hypotheses.add(Tuple3.of(layer, hs, h)));
	}

	private void createEvaluationResult(int layers, File evalFile) throws IOException {
		this.evaluationResultFile = evalFile;

		if (this.evaluationResultFile.exists()) {
			this.evaluationData = Serialize.getObjectMapper(true).readValue(this.evaluationResultFile, EvaluationData.class);
			return;
		}

		this.evaluationData = new EvaluationData(layers);
	}

	/**
	 * Get the id of the exploration result (may be the text).
	 *
	 * @return the id of the exploration (may be the text)
	 */
	public String getId() {
		return this.explorationResult.getId();
	}

	/**
	 * Count the total amount of correct hypothesis (distinct by value).
	 *
	 * @param layer the layer of the hypothesis
	 * @return the total amount of correct hypothesis
	 */
	public int getGoodHypothesesCount(int layer) {
		return this.evaluationData.getGoodHypothesesCount(layer);
	}

	/**
	 * Count the total amount of incorrect hypothesis (distinct by value).
	 *
	 * @param layer the layer of the hypothesis
	 * @return the total amount of incorrect hypothesis
	 */
	public int getBadHypothesesCount(int layer) {
		return this.evaluationData.getBadHypothesesCount(layer);
	}

	/**
	 * Count the correct and incorrect classified hypothesis in an exploration
	 * result. (Distinct by value)
	 *
	 * @param exploration        the exploration result
	 * @param isPseudoHypothesis indicator for pseudo hypotheses
	 * @return (correct classified count, incorrect classified count) for each layer
	 *         in the exploration
	 */
	public List<Tuple2<Integer, Integer>> getHitsWithBad(IExplorationResult exploration, boolean isPseudoHypothesis) {
		List<Tuple2<Integer, IHypothesis>> hits = new ArrayList<>();
		List<Tuple2<Integer, IHypothesis>> bad = new ArrayList<>();
		this.getHitsWithBad(0, hits, bad, exploration.getExplorationRoot(), isPseudoHypothesis);

		int maxLayer = Stream.concat(hits.stream(), bad.stream()).mapToInt(Tuple2::getFirst).max().getAsInt();

		List<Tuple2<Integer, Integer>> result = new ArrayList<>();
		for (int i = 0; i <= maxLayer; i++) {
			final int l = i;
			List<IHypothesis> layerHits = hits.stream().filter(h -> h.getFirst() == l).map(Tuple2::getSecond).collect(Collectors.toList());
			List<IHypothesis> layerBadHits = bad.stream().filter(h -> h.getFirst() == l).map(Tuple2::getSecond).collect(Collectors.toList());

			result.add(Tuple2.of(EvaluationData.countHypotheses(layerHits), EvaluationData.countHypotheses(layerBadHits)));
		}

		return result;
	}

	private void getHitsWithBad(int layer, List<Tuple2<Integer, IHypothesis>> hits, List<Tuple2<Integer, IHypothesis>> bad, ILayerEntry step, boolean isPseudoHypothesis) {
		// Look for selected in path ..

		var selections = step.getSelectionsFromBefore();
		if (selections != null && !isPseudoHypothesis) {
			for (var sel : selections) {
				for (var h : sel.getSelectedHypotheses()) {
					if (Classification.isGood(this.evaluationData.getClassification(layer - 1, (HypothesisDTO) h)) == Boolean.TRUE) {
						hits.add(Tuple2.of(layer - 1, h));
					} else if (Classification.isGood(this.evaluationData.getClassification(layer - 1, (HypothesisDTO) h)) == Boolean.FALSE) {
						bad.add(Tuple2.of(layer - 1, h));
					}
				}
			}
		}

		// Leaf == no child or no selections in (any) child (pseudo hypotheses)
		boolean isLeaf = step.getChildren().isEmpty();
		if (isLeaf || isPseudoHypothesis) {
			// if leaf use generated hypotheses instead of selections ..
			for (var h : this.getHypothesesForLeaf(step, isPseudoHypothesis ? Configuration.MAX_HYPOTHESES_PER_PSEUDO_HYP : Configuration.MAX_HYPOTHESES_PER_LEAF)) {
				if (Classification.isGood(this.evaluationData.getClassification(layer, (HypothesisDTO) h)) == Boolean.TRUE) {
					hits.add(Tuple2.of(layer, h));
				} else if (Classification.isGood(this.evaluationData.getClassification(layer, (HypothesisDTO) h)) == Boolean.FALSE) {
					bad.add(Tuple2.of(layer, h));
				}
			}
		}

		for (var child : step.getChildren()) {
			this.getHitsWithBad(layer + 1, hits, bad, child, isPseudoHypothesis);
		}

	}

	private List<IHypothesis> getHypothesesForLeaf(ILayerEntry step, int max) {
		List<IHypothesis> result = new ArrayList<>();
		for (var hs : step.getHypotheses()) {
			int maxHypotheses = hs.isOnlyOneHypothesisValid() ? 1 : max;

			List<IHypothesis> orderdHypothesis = hs.getHypotheses();
			double score = orderdHypothesis.get(0).getConfidence();

			// Add Hypotheses as long as maxHypotheses not reached (or in same group of
			// confidence as before)
			for (int i = 0; i < orderdHypothesis.size() && (i < maxHypotheses || this.equalScores(score, orderdHypothesis.get(i).getConfidence())); i++) {
				var h = orderdHypothesis.get(i);
				score = h.getConfidence();
				if (Configuration.SKIP_IFF_CONFIDENCE_LESS != null && !Double.isNaN(score) && score <= Configuration.SKIP_IFF_CONFIDENCE_LESS) {
					break;
				}
				result.add(h);
			}
		}

		return result;
	}

	private boolean equalScores(double score1, double score2) {
		return score1 == score2 || (Double.isNaN(score1) && Double.isNaN(score2));
	}

}