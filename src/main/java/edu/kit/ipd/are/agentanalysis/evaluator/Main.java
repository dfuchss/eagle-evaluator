package edu.kit.ipd.are.agentanalysis.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.fuchss.tools.tuple.Tuple3;

import edu.kit.ipd.are.agentanalysis.port.xplore.dto.HypothesisDTO;

/**
 * The main class of the agent analysis evaluator.
 *
 * @author Dominik Fuchss
 *
 */
public final class Main {
	private static boolean useGUI = true;

	private Main() {
		throw new IllegalAccessError();
	}

	/**
	 * The main method of the agent analysis evaluator.
	 *
	 * @param args you may provide one argument: the exploration result input file
	 */
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		File explorationFile;

		if (args.length == 0) {
			System.out.println("INFO: You can also provide the file for Evaluation via args[0] ..");
			System.out.println("What is the input file?\n");
			explorationFile = new File(scan.nextLine());
		} else {
			explorationFile = new File(args[0]);
		}

		if (explorationFile.isDirectory()) {
			File evalFile = new File(explorationFile.getAbsolutePath() + File.separator + explorationFile.getName() + ".eval.json");
			File[] explorationFiles = explorationFile.listFiles(f -> f.getName().endsWith(".json") && !(f.getName().endsWith(".eval.json") || f.getName().endsWith("-no-hyp.json")));

			if (explorationFiles == null || explorationFiles.length == 0) {
				System.err.println("Explored File for Directory does not exis .. skipping ..");
				System.exit(1);
			}

			for (File exploredFile : explorationFiles) {
				Main.evaluate(scan, exploredFile, evalFile);
			}

			File noHypFile = new File(explorationFile.getAbsolutePath() + File.separator + explorationFile.getName() + "-no-hyp.json");
			if (noHypFile.exists()) {
				Main.evaluate(scan, noHypFile, evalFile);
			}

//			Statistics.generateStats(explorationFile, evalFile);
		} else {
			Main.evaluate(scan, explorationFile, null);
		}
		scan.close();

	}

	private static void evaluate(Scanner scan, File explorationFile, File evalFile) {
		System.err.println("FILE: " + explorationFile.getName());

		Evaluator evaluator = null;
		try {
			evaluator = new Evaluator(explorationFile, evalFile);
		} catch (IOException e) {
			System.err.println("Cannot load file: " + e);
			return;
		}

		Main.startEvaluation(scan, evaluator);

		try {
			evaluator.save();
		} catch (IOException e) {
			System.err.println("Error while saving file ..");
		}

	}

	private static void startEvaluation(Scanner scan, Evaluator evaluator) {
		if (Main.useGUI) {
			Main.setSystemLookAndFeel();
		}

		Tuple3<Integer, HypothesisDTO, String> next;
		while ((next = evaluator.findNextHypothesis()) != null) {
			int layer = next.getFirst();
			HypothesisDTO hypothesis = next.getSecond();
			String word = next.getThird();

			Classification cls;
			if (Main.useGUI) {
				cls = Main.classifyGUI(evaluator, layer, hypothesis, word);
			} else {
				cls = Main.classifyTUI(scan, evaluator, layer, hypothesis, word);
			}
			evaluator.setClassification(next.getFirst(), next.getSecond(), cls);
		}

	}

	private static Classification classifyGUI(Evaluator evaluator, int layer, HypothesisDTO hypothesis, String word) {
		String[] answers = Arrays.asList(Classification.values()).stream().map(Classification::toString).collect(Collectors.toList()).toArray(String[]::new);

		String message = "Sentence: \"" + evaluator.getSentence() + "\"\n";
		if (word == null) {
			message += "Layer: " + layer + ", Hypothesis: " + hypothesis.getValue();
		} else {
			message += "Layer: " + layer + ", Word: \"" + word + "\", Hypothesis: " + hypothesis.getValue();
		}

		int selection = -1;
		while (selection == -1) {
			selection = JOptionPane.showOptionDialog(null, message, "Classification", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, answers, null);
		}
		return Classification.values()[selection];
	}

	private static Classification classifyTUI(Scanner scan, Evaluator evaluator, int layer, HypothesisDTO hypothesis, String word) {
		System.out.println("----------------------");
		System.out.println("Next hypothesis for sentence \"" + evaluator.getSentence() + "\" is:\n");
		if (word == null) {
			System.out.println("Layer: " + layer + ", Hypothesis: " + hypothesis.getValue());
		} else {
			System.out.println("Layer: " + layer + ", Word: \"" + word + "\", Hypothesis: " + hypothesis.getValue());
		}

		Classification cls = null;
		while (cls == null) {
			System.out.println(Classification.QUESTION);
			String result = scan.nextLine().toLowerCase();
			cls = Classification.getByString(result);
		}

		return cls;
	}

	private static void setSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			System.err.println("Cannot set system's look and feel ..");
		}
	}
}
