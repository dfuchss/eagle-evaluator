package edu.kit.ipd.are.agentanalysis.evaluator;

/**
 * Defines a wrapper class for an evaluation score. See
 * <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and
 * Recall</a> and <a href="https://en.wikipedia.org/wiki/F1_score">F1 Score</a>
 *
 * @author Dominik Fuchss
 *
 */
public final class Score implements Comparable<Score> {
	/**
	 * The precision (how many selected elements are relevant): tp/(tp+fp).
	 */
	public final double precision;
	/**
	 * The recall (how many relevant elements are selected): tp/(tp+fn).
	 */
	public final double recall;
	/**
	 * Harmonic mean of {@link #precision} and {@link #recall}.
	 */
	public final double f1;

	/**
	 * Create score wrapper by precision and recall.
	 *
	 * @param precision the precision
	 * @param recall    the recall
	 */
	public Score(double precision, double recall) {
		this.precision = precision;
		this.recall = recall;
		this.f1 = 2 * precision * recall / (precision + recall);
	}

	@Override
	public String toString() {
		String p = String.format("%.2f", this.precision * 100);
		String r = String.format("%.2f", this.recall * 100);
		String f = String.format("%.2f", this.f1 * 100);
		return String.format("P: %s%%, R: %s%%, F1: %s%%", p, r, f);
	}

	@Override
	public int compareTo(Score o) {
		boolean isNaN = Double.isNaN(this.f1);
		boolean otherIsNaN = Double.isNaN(o.f1);

		if (isNaN && otherIsNaN) {
			return Double.compare(Math.max(this.precision, this.recall), Math.max(o.precision, o.recall));
		}
		if (isNaN) {
			return -1;
		}
		if (otherIsNaN) {
			return 1;
		}
		return Double.compare(this.f1, o.f1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.f1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.precision);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.recall);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		Score other = (Score) obj;
		if (Double.doubleToLongBits(this.f1) != Double.doubleToLongBits(other.f1)) {
			return false;
		}
		if (Double.doubleToLongBits(this.precision) != Double.doubleToLongBits(other.precision)) {
			return false;
		}
		if (Double.doubleToLongBits(this.recall) != Double.doubleToLongBits(other.recall)) {
			return false;
		}
		return true;
	}

}
