package edu.kit.ipd.are.agentanalysis.evaluator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.fuchss.tools.tuple.Tuple2;

import edu.kit.ipd.are.agentanalysis.impl.xplore.rating.HypothesesSelectionFunction;
import edu.kit.ipd.are.agentanalysis.impl.xplore.rating.LayerCombination;
import edu.kit.ipd.are.agentanalysis.impl.xplore.rating.LayerEntryEvaluation;
import edu.kit.ipd.are.agentanalysis.impl.xplore.rating.NormalizedAggregate;
import edu.kit.ipd.are.agentanalysis.port.util.Serialize;
import edu.kit.ipd.are.agentanalysis.port.xplore.IExplorationResult;
import edu.kit.ipd.are.agentanalysis.port.xplore.IPath;
import edu.kit.ipd.are.agentanalysis.port.xplore.dto.ExplorationResultDTO;

/**
 * Helper class to generate statistics for evaluation.
 *
 * @author Dominik Fuchss
 *
 */
public final class Statistics {
	private Statistics() {
		throw new IllegalAccessError();
	}

	/**
	 * Generate statistics by exploration file / dir and evaluation file.
	 *
	 * @param explorationFileOrDir the exploration file or directory
	 * @param evalFile             the evaluation file ({@link EvaluationData})
	 * @deprecated agent-analysis-evaluation is used to generate the evaluation
	 *             results. statistics not needed anymore.
	 */
	@Deprecated
	public static void generateStats(File explorationFileOrDir, File evalFile) {
		System.err.println("Writing Stat file ..");
		Evaluator evaluator = null;
		try {
			evaluator = new Evaluator(null, evalFile);
		} catch (IOException e) {
			System.err.println("Cannot load file: " + e);
			return;
		}

		List<File> toScore = explorationFileOrDir.isDirectory() //
				? Arrays.asList(explorationFileOrDir.listFiles(f -> !f.isDirectory() && f.getName().endsWith(".json") && !f.getName().endsWith(".eval.json")))
				: List.of(explorationFileOrDir);

		// Find all possible hits by layer and summed
		List<Integer> allPossibleHitsPerLayer = IntStream.range(0, evaluator.findNumOfLayers()).mapToObj(evaluator::getGoodHypothesesCount).collect(Collectors.toList());
		int allPossibleHits = allPossibleHitsPerLayer.stream().mapToInt(h -> h).sum();

		// Counted stats ..
		List<Tuple2<String, List<Score>>> fileXScores = Statistics.createScores(Statistics.extractExplorationResults(explorationFileOrDir.getAbsolutePath(), toScore), evaluator,
				allPossibleHitsPerLayer, allPossibleHits);
		Statistics.storeStats(fileXScores, evalFile, explorationFileOrDir);

		// Generate CSV stats
		if (evalFile != null) {
			try {
				Statistics.generateCSV(evalFile, evaluator, toScore);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void generateCSV(File evalFile, Evaluator evaluator, List<File> toScore) throws IOException {
		File target = new File(evalFile.getAbsolutePath() + ".stats.csv");
		FileWriter fw = new FileWriter(target);
		var paths = Statistics.extractExplorationResults("Scenario", toScore);

		int layers = evaluator.findNumOfLayers();
		Statistics.generateCSVHeader(fw, layers, paths.get(0).getSecond().getInputText());
		for (var path : paths) {
			// TODO Check NoHyp works ..
			Statistics.appendLine(layers, fw, path, evaluator, path.getFirst().contains("no-hyp"));
		}
		fw.close();
	}

	private static void generateCSVHeader(FileWriter fw, int layers, String sentence) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(sentence).append("\n");

		sb.append("Scenario;Score;");
		for (int i = 0; i < layers; i++) {
			sb.append("Layer-").append(i).append(" # Hit;");
		}
		sb.append("# Hit;");

		for (int i = 0; i < layers; i++) {
			sb.append("Layer-").append(i).append(" # Bad;");
		}
		sb.append("# Bad;");

		sb.append("\n");

		fw.append(sb.toString());
	}

	private static void appendLine(int layers, FileWriter fw, Tuple2<String, IExplorationResult> path, Evaluator evaluator, boolean isPseudoHypothesis) throws IOException {
		var hitsXbadPerLayer = evaluator.getHitsWithBad(path.getSecond(), isPseudoHypothesis);

		fw.append(path.getFirst()).append(";").append(Statistics.getScore(path.getSecond().getInputText())).append(";");

		// Good values
		int hits = 0;
		for (int i = 0; i < hitsXbadPerLayer.size(); i++) {
			int hit = hitsXbadPerLayer.get(i).getFirst();
			fw.append(String.valueOf(hit)).append(";");
			hits += hit;
		}

		for (int i = hitsXbadPerLayer.size(); i < layers; i++) {
			fw.append(";");
		}

		fw.append(String.valueOf(hits)).append(";");

		// Bad Values
		int bads = 0;
		for (int i = 0; i < hitsXbadPerLayer.size(); i++) {
			int bad = hitsXbadPerLayer.get(i).getSecond();
			fw.append(String.valueOf(bad)).append(";");
			bads += bad;
		}

		for (int i = hitsXbadPerLayer.size(); i < layers; i++) {
			fw.append(";");
		}

		fw.append(String.valueOf(bads)).append(";");

		fw.append("\n");
	}

	private static String getScore(String inputText) {
		int start = inputText.indexOf('(');
		int end = inputText.indexOf(')');
		if (start == -1 || end == -1 || start + 1 >= end) {
			return "";
		}
		double score = Double.parseDouble(inputText.substring(start + 1, end));
		return String.format("%.4f", score);
	}

	private static List<Tuple2<String, IExplorationResult>> extractExplorationResults(String basePath, List<File> toScore) {
		// Should contain the explored file and an optional no-hyp file ..
		assert toScore.size() <= 2;
		List<Tuple2<String, IExplorationResult>> result = new ArrayList<>();

		for (File f : toScore) {
			IExplorationResult exploration;
			try {
				exploration = ExplorationResultDTO.load(f);
			} catch (IOException e) {
				System.err.println("Cannot load file: " + e);
				continue;
			}

			List<IPath> paths = exploration.getPaths();

			if (paths.size() != 1) {
				// Find all other paths .. with ratings ..
				List<Tuple2<String, IExplorationResult>> ratedPaths = Statistics.generateRatings(basePath, f.getName(), exploration.getInputText(), paths);
				result.addAll(ratedPaths);
			}

			result.add(Tuple2.of(f.getName(), exploration));

		}

		return result;
	}

	private static List<Tuple2<String, IExplorationResult>> generateRatings(String basePath, String key, String text, List<IPath> paths) {
		List<Tuple2<String, IExplorationResult>> result = new ArrayList<>();
		for (var selector : HypothesesSelectionFunction.values()) {
			for (var le : LayerEntryEvaluation.values()) {
				for (var lc : LayerCombination.values()) {
					Statistics.examineText(result, basePath, key, text, selector, le, lc, paths);
				}
			}
		}
		return result;
	}

	private static void examineText(List<Tuple2<String, IExplorationResult>> result, String basePath, String key, String text, //
			HypothesesSelectionFunction selector, LayerEntryEvaluation lEval, LayerCombination lComb, List<IPath> paths) {
		var nam = new NormalizedAggregate(selector, lEval, lComb, 1E-8, 1 - 1E-8);
		var scores = nam.ratePaths(paths);
		// Sort by Range
		var pathXscore = IntStream.range(0, paths.size())//
				.mapToObj(i -> Tuple2.of(paths.get(i), scores.get(i))).sorted((a, b) -> Double.compare(a.getSecond(), b.getSecond())).collect(Collectors.toList());

		int pad = String.valueOf(pathXscore.size() - 1).length();

		for (int i = 0; i < pathXscore.size(); i++) {
			var path = pathXscore.get(i);
			String pathText = text + " (" + path.getSecond() + ")";
			String name = key.substring(0, key.length() - ".json".length()) + "-Top-" + String.format("%0" + pad + "d", i) + "-" + selector + "-" + lEval + "-" + lComb + "-best.json";
			result.add(Tuple2.of(name, path.getFirst().toExplorationResult(pathText)));

			if (Configuration.storePaths) {
				try {
					var mapper = Serialize.getObjectMapperForGetters(true);
					var jsonGetter = mapper.writeValueAsString(path.getFirst().toExplorationResult(pathText));
					var fw = new FileWriter(new File(basePath + File.separator + name));
					fw.write(jsonGetter);
					fw.close();
				} catch (IOException e) {
					System.err.println("Error while storing file .. " + e);
				}
			}
		}

	}

	private static List<Tuple2<String, List<Score>>> createScores(List<Tuple2<String, IExplorationResult>> toScore, Evaluator evaluator, List<Integer> allPossibleHitsPerLayer, int allPossibleHits) {
		List<Tuple2<String, List<Score>>> fileXScores = new ArrayList<>();

		for (Tuple2<String, IExplorationResult> input : toScore) {
			// TODO Check NoHyp works ..
			var hitsXbadPerLayer = evaluator.getHitsWithBad(input.getSecond(), input.getFirst().contains("no-hyp"));
			Tuple2<Integer, Integer> allHitsXallBad = Tuple2.of(//
					hitsXbadPerLayer.stream().mapToInt(Tuple2::getFirst).sum(), //
					hitsXbadPerLayer.stream().mapToInt(Tuple2::getSecond).sum()//
			);
			List<Score> scores = new ArrayList<>();

			for (int i = 0; i < hitsXbadPerLayer.size(); i++) {
				scores.add(Statistics.getScore(hitsXbadPerLayer.get(i).getFirst(), hitsXbadPerLayer.get(i).getSecond(), allPossibleHitsPerLayer.get(i)));
			}
			scores.add(Statistics.getScore(allHitsXallBad.getFirst(), allHitsXallBad.getSecond(), allPossibleHits));

			fileXScores.add(Tuple2.of(input.getFirst(), scores));
		}

		Collections.sort(fileXScores, (a, b) -> b.getFirst().compareTo(a.getFirst()));
		Collections.sort(fileXScores, (a, b) -> b.getSecond().get(b.getSecond().size() - 1).compareTo(a.getSecond().get(a.getSecond().size() - 1)));

		return fileXScores;
	}

	private static Score getScore(int hitCount, int badCount, int allHits) {
		// Precision
		double precision = (1.0 * hitCount) / (hitCount + badCount);
		// Recall
		double recall = (1.0 * hitCount) / allHits;
		return new Score(precision, recall);
	}

	private static void storeStats(List<Tuple2<String, List<Score>>> fileXScores, File evalFile, File explorationFile) {
		StringBuilder resultString = new StringBuilder();
		StringBuilder resultStringDetails = new StringBuilder();

		for (var hit : fileXScores) {
			var name = hit.getFirst();
			var scores = hit.getSecond();
			var score = scores.get(scores.size() - 1);
			var subscores = scores.subList(0, scores.size() - 1);
			resultString.append(name).append(" Score: ").append(score).append("\n");
			resultStringDetails.append(name).append(" Score: ").append(score).append("\n");

			resultStringDetails.append("\tDetails:\n");
			for (var sub : subscores) {
				resultStringDetails.append("\t").append(sub).append("\n");
			}
		}

		try (FileWriter fw = new FileWriter(new File((evalFile == null ? explorationFile.getAbsolutePath() : evalFile.getAbsolutePath()) + ".stats.txt"))) {
			fw.write(resultString.toString());
		} catch (IOException e) {
			System.err.println("Cannot write file: " + e);
		}

		try (FileWriter fw = new FileWriter(new File((evalFile == null ? explorationFile.getAbsolutePath() : evalFile.getAbsolutePath()) + ".stats-details.txt"))) {
			fw.write(resultStringDetails.toString());
		} catch (IOException e) {
			System.err.println("Cannot write file: " + e);
		}

	}
}
