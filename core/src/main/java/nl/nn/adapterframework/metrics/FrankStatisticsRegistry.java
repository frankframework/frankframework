/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.metrics;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramGauges;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipe;

public class FrankStatisticsRegistry extends SimpleMeterRegistry {
	private static final NumberFormat keyFormat = new DecimalFormat("#");

	public FrankStatisticsRegistry() {
		super(SimpleConfig.DEFAULT, Clock.SYSTEM);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
		DistributionStatisticConfig merged = distributionStatisticConfig.merge(DistributionStatisticConfig.builder().expiry(Duration.ofHours(1)).build());
		DistributionSummary summary = new FrankDistributionSummary(id, clock, merged, scale, false);

		HistogramGauges.registerWithCommonFormat(summary, this);

		return summary;
	}

	public Map<String, Object> doSomething(IAdapter adapter) {
		List<Tag> searchTags = new ArrayList<>();
		searchTags.add(Tag.of("type", "application"));
		if(adapter != null) {
			searchTags.add(Tag.of("adapter", adapter.getName()));
		}

//		JsonObjectBuilder root = Json.createObjectBuilder();
//		JsonObjectBuilder pipeline = Json.createObjectBuilder();
//		root.add("pipeline", pipeline);
//		JsonArrayBuilder duration = Json.createArrayBuilder();
//		pipeline.add("duration", duration);

//		JsonArrayBuilder pipes = Json.createArrayBuilder();
//		JsonObjectBuilder pipe = Json.createObjectBuilder();
//		pipe.add("name", name);
//		duration.add(pipe);

		Search search = Search.in(this).tags(searchTags);
		Map<String, Object> root = new LinkedHashMap<>();

//		for(DistributionSummary meter : search.name("frank.pipeline.duration").summaries()) { //n -> true
//			System.err.println(meter.getClass());
//			String name = meter.getId().getTag("name");

//			List<String> removeTags = Arrays.asList(new String[] {"adapter", "configuration", "type"});
//			List<Tag> tags = new ArrayList<>(meter.getId().getTags());
//			tags.removeIf(e -> removeTags.contains(e.getKey()));

//			System.out.println(meter.getId().getName() + " in " + meter.getId().getBaseUnit()+ " tags" + meter.getId().getTags() + " - ");
//		}

//		frank.messagesInError
//		frank.messagesProcessed
//		frank.receivers.messagesReceived
//		frank.receivers.messagesRejected
//		frank.receivers.messagesRetried

		for(IPipe pipe : adapter.getPipeLine().getPipes()) {
			
			System.out.println(pipe.getName());
		}

		List adapterStats = getDistributionSummary(search, "frank");
		if(adapterStats.isEmpty()) {
			return null;
		}

		root.put("totalMessageProccessingTime", adapterStats.get(0));
//		root.put("receivers", getReceivers(search, "frank.receivers.messagesReceived"));
		root.put("durationPerPipe", getDistributionSummary(search, "frank.pipeline.duration"));
		root.put("sizePerPipe", getDistributionSummary(search, "frank.pipeline.size"));

		return root;
	}

	private Map<String, Map<String, Object>> getMeters(Search search, String meterName) {
		return null;
	}

	private List getDistributionSummary(Search search, String summaryName) {
//		Map<String, Map<String, Object>> root = new ConcurrentSkipListMap<>();
		List<Map<String, Object>> root = new LinkedList<>();
		for(DistributionSummary distSum : search.name(summaryName).summaries()) { //n -> true
			FrankDistributionSummary summary = (FrankDistributionSummary) distSum;
			String name = summary.getId().getTag("name");
//			Map<String, Object> values = root.computeIfAbsent(name, e -> new LinkedHashMap<>());
			Map<String, Object> values = new LinkedHashMap<>();
			String unit = summary.getId().getBaseUnit();
			HistogramSnapshot snapshot = summary.takeSnapshot();//abstractTimeWindowHistogram

			long total = summary.count();
			values.put("name", name);
			values.put("count", total);
			values.put("min", summary.getMin());
			values.put("max", format(summary.max()));
			values.put("avg", format(summary.mean()));
			values.put("stdDev", format(summary.getStdDev()));
			values.put("sum", format(summary.totalAmount()));
			values.put("first", summary.getFirst());
			values.put("last", summary.getLast());

			//le tags, 100/1000/2000/10000
			for (CountAtBucket bucket : snapshot.histogramCounts()) {
				String key = keyFormat.format(bucket.bucket()) + unit;
				values.put(key, bucket.count());
			}

			//phi tags, 50/90/95/98
			for (ValueAtPercentile percentile : snapshot.percentileValues()) {
				String key = "p" + keyFormat.format(percentile.percentile() * 100);
				values.put(key, format(percentile.value()));
			}
			root.add(values);
		}
		return root;
	}

	private Number format(double val) {
		if (Double.isInfinite(val) || Double.isNaN(val)) {
			return 0;
		}
		return new BigDecimal(val).setScale(1, BigDecimal.ROUND_HALF_EVEN);
	}
}
