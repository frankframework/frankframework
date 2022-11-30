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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;

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
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.pipes.SenderPipe;

public class FrankStatisticsRegistry extends SimpleMeterRegistry {
	private static final NumberFormat keyFormat = new DecimalFormat("#");

	public FrankStatisticsRegistry(SimpleConfig config) {
		super(config, Clock.SYSTEM);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
		DistributionStatisticConfig merged = distributionStatisticConfig.merge(DistributionStatisticConfig.builder().expiry(Duration.ofHours(1)).build());
		DistributionSummary summary = new FrankDistributionSummary(id, clock, merged, scale, false);

		HistogramGauges.registerWithCommonFormat(summary, this);

		return summary;
	}

	public JsonStructure scrape(IAdapter adapter) {
		List<Tag> searchTags = new ArrayList<>();
		searchTags.add(Tag.of("type", "application"));
		if(adapter != null) {
			searchTags.add(Tag.of("adapter", adapter.getName()));
		}

		List<IPipe> pipes = adapter.getPipeLine().getPipes();
		Search search = Search.in(this).tags(searchTags);

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

		JsonObjectBuilder root = Json.createObjectBuilder();
		root.add("totalMessageProccessingTime", getDistributionSummary(search));
//		root.put("receivers", getReceivers(search, "frank.receivers.messagesReceived"));
		root.add("durationPerPipe", getDistributionSummary(pipes, search, "frank.pipeline.duration"));
		root.add("sizePerPipe", getDistributionSummary(pipes, search, "frank.pipeline.size"));

		return root.build();
	}

	private JsonObjectBuilder getDistributionSummary(Search search) {
		DistributionSummary summary = search.name("frank").summary();
		return readSummary((FrankDistributionSummary) summary);
	}

	private JsonArrayBuilder getDistributionSummary(Search search, String summaryName) {
		JsonArrayBuilder root = Json.createArrayBuilder();
		Collection<DistributionSummary> summaries = search.name(summaryName).summaries();

		for(DistributionSummary distSum : summaries) { //n -> true
			root.add(readSummary((FrankDistributionSummary) distSum));
		}

		return root;
	}

	//This feels too complicated still...
	private JsonArrayBuilder getDistributionSummary(List<IPipe> pipes, Search search, String summaryName) {
		//See PipeLine.iterateOverStatistics(...)
		List<String> metricOrder = new ArrayList<>();
		for (IPipe pipe : pipes) {
			metricOrder.add(pipe.getName());

			if(pipe instanceof SenderPipe) {
				//TODO validators/wrappers
				ISender sender = ((SenderPipe) pipe).getSender();
				metricOrder.add("getConnection for "+sender.getName()); //TODO implement getStatisticName
			}
		}

		List<FrankDistributionSummary> sum = new ArrayList<>();
		Collection<DistributionSummary> summaries = search.name(summaryName).summaries();
		for(DistributionSummary summary : summaries) {
			sum.add((FrankDistributionSummary) summary);
		}
		sum.sort(Comparator.comparing(FrankDistributionSummary::getId, (s1, s2) -> {
			return metricOrder.indexOf(s1.getName());
		}));

		JsonArrayBuilder root = Json.createArrayBuilder();
		for(FrankDistributionSummary summary : sum) {
			root.add(readSummary(summary));
		}
		return root;
	}

	private JsonObjectBuilder readSummary(FrankDistributionSummary summary) {
		String name = summary.getId().getTag("name");
		JsonObjectBuilder jsonSummary = Json.createObjectBuilder();
		String unit = summary.getId().getBaseUnit();
		HistogramSnapshot snapshot = summary.takeSnapshot();//abstractTimeWindowHistogram

		long total = summary.count();
		jsonSummary.add("name", name);
		jsonSummary.add("count", total);
		jsonSummary.add("min", summary.getMin());
		jsonSummary.add("max", format(summary.max()));
		jsonSummary.add("avg", format(summary.mean()));
		jsonSummary.add("stdDev", format(summary.getStdDev()));
		jsonSummary.add("sum", format(summary.totalAmount()));
		jsonSummary.add("first", summary.getFirst());
		jsonSummary.add("last", summary.getLast());

		//le tags, 100/1000/2000/10000
		for (CountAtBucket bucket : snapshot.histogramCounts()) {
			String key = keyFormat.format(bucket.bucket()) + unit;
			jsonSummary.add(key, bucket.count());
		}

		//phi tags, 50/90/95/98
		for (ValueAtPercentile percentile : snapshot.percentileValues()) {
			String key = "p" + keyFormat.format(percentile.percentile() * 100);
			jsonSummary.add(key, format(percentile.value()));
		}

		return jsonSummary;
	}

	private double format(double val) {
		if (Double.isInfinite(val) || Double.isNaN(val)) {
			return 0;
		}
		return new BigDecimal(val).setScale(1, BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}
}
