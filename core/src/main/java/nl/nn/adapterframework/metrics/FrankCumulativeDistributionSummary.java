package nl.nn.adapterframework.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.cumulative.CumulativeDistributionSummary;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.TimeWindowPercentileHistogram;

public class FrankCumulativeDistributionSummary extends CumulativeDistributionSummary {
	private final AtomicLong min;
	private final LongAdder sumOfSquares;
	private final AtomicLong first;
	private final AtomicLong last;

	public FrankCumulativeDistributionSummary(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig, double scale, boolean supportsAggregablePercentiles) {
		super(id, clock, distributionStatisticConfig, scale, supportsAggregablePercentiles);
		this.min = new AtomicLong(Long.MAX_VALUE);
		this.sumOfSquares = new LongAdder();
		this.first = new AtomicLong(-1);
		this.last = new AtomicLong();
	}

	@Override
	protected void recordNonNegative(double amount) {
		super.recordNonNegative(amount);
		checkMinMax((long) amount);
		addSums((long) amount);
		updateFirstLast((long) amount);
	}

	private void updateFirstLast(long value) {
		first.compareAndSet(-1, value);
		last.set(value);
	}

	public void checkMinMax(long value) {
		if (value < min.get()) {
			min.set(value);
		}
	}

	protected void addSums(long value) {
		sumOfSquares.add(value * value);
	}

	public double getVariance() {
		return calculateVariance(count(), Math.round(totalAmount()), sumOfSquares.sum());
	}
	private double calculateVariance(long count, long sum, long sumOfSquares) {
		if (count>1) {
			return (sumOfSquares-((sum*sum)/count))/(count-1);
		} else {
			return Double.NaN;
		}
	}

	public double getStdDev() {//moet null terug geven
		return Math.sqrt(getVariance());
	}

	public long getMin() {
		long value = min.get();
		if(value == Long.MAX_VALUE) {
			return 0;
		}
		return value;
	}

	public long getFirst() {
		return first.get();
	}

	public long getLast() {
		return last.get();
	}

	public TimeWindowPercentileHistogram getHistogram() {
		return (TimeWindowPercentileHistogram) histogram;
	}
}
