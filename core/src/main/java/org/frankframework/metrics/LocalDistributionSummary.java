/*
   Copyright 2022-2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.metrics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.cumulative.CumulativeDistributionSummary;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;

public class LocalDistributionSummary extends CumulativeDistributionSummary {
	private static final CountAtBucket[] EMPTY_HISTOGRAM = new CountAtBucket[0];
	private final AtomicLong min;
	private final LongAdder sumOfSquares;
	private final AtomicLong first;
	private final AtomicLong last;

	public LocalDistributionSummary(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig, double scale) {
		super(id, clock, mergeConfig(distributionStatisticConfig), scale, false);

		this.min = new AtomicLong(Long.MAX_VALUE);
		this.sumOfSquares = new LongAdder();
		this.first = new AtomicLong(-1);
		this.last = new AtomicLong();
	}

	private static DistributionStatisticConfig mergeConfig(DistributionStatisticConfig config) {
		return DistributionStatisticConfig.builder()
		.expiry(Duration.ofDays(7)) // a week should be enough?
		.bufferLength(7)
		.build()
		.merge(config);
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
		if (count > 1) {
			long sumSQ = sumOfSquares-((sum*sum)/count);
			return (double) sumSQ / (count-1);
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

	/**
	 * For Prometheus we cannot use the histogram counts from HistogramSnapshot, as
	 * it is based on a rolling histogram. Prometheus requires a histogram that
	 * accumulates values over the lifetime of the app.
	 *
	 * @return Cumulative histogram buckets.
	 */
	private CountAtBucket[] histogramCounts() {
		return histogram == null ? EMPTY_HISTOGRAM : histogram.takeSnapshot(0, 0, 0).histogramCounts();
	}

	@Override
	public HistogramSnapshot takeSnapshot() {
		HistogramSnapshot snapshot = super.takeSnapshot();
		return new HistogramSnapshot(snapshot.count(), snapshot.total(), snapshot.max(), snapshot.percentileValues(), histogramCounts(), snapshot::outputSummary);
	}
}
