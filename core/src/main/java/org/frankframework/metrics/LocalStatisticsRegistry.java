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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
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
import jakarta.annotation.Nonnull;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.receivers.Receiver;
import org.frankframework.statistics.FrankMeterType;

public class LocalStatisticsRegistry extends SimpleMeterRegistry {
	private static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#");

	public LocalStatisticsRegistry() {
		super();
	}

	public LocalStatisticsRegistry(SimpleConfig config) {
		super(config, Clock.SYSTEM);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id, DistributionStatisticConfig config, double scale) {
		DistributionSummary summary = new LocalDistributionSummary(id, clock, config, scale);

		HistogramGauges.registerWithCommonFormat(summary, this);

		return summary;
	}

	private Collection<Meter> findMeters(String configuration, Adapter adapter) {
		List<Tag> searchTags = new ArrayList<>();
		searchTags.add(Tag.of("configuration", configuration));
		searchTags.add(Tag.of("adapter", adapter.getName()));

		Search search = Search.in(this).tags(searchTags);
		return search.meters();
	}

	public JsonStructure scrape(String configuration, Adapter adapter) {
		Collection<Meter> adapterScopedMeters = findMeters(configuration, adapter);

		Map<String, JsonObjectBuilder> pipeDurations = getDistributionSummaries(adapterScopedMeters, FrankMeterType.PIPE_DURATION, "");
		JsonObjectBuilder pipelineSize = getDistributionSummary(adapterScopedMeters, FrankMeterType.PIPELINE_SIZE); //called "- pipeline in"
		Map<String, JsonObjectBuilder> pipeSizeIn = getDistributionSummaries(adapterScopedMeters, FrankMeterType.PIPE_SIZE_IN, " (in)");
		Map<String, JsonObjectBuilder> pipeSizeOut = getDistributionSummaries(adapterScopedMeters, FrankMeterType.PIPE_SIZE_OUT, " (out)");
		JsonObjectBuilder pipelineWaitTime = getDistributionSummary(adapterScopedMeters, FrankMeterType.PIPELINE_WAIT_TIME); //called "- pipeline in"
		Map<String, JsonObjectBuilder> pipeWaitTime = getDistributionSummaries(adapterScopedMeters, FrankMeterType.PIPE_WAIT_TIME, "");

		JsonArrayBuilder durationStatistics = Json.createArrayBuilder();
		JsonArrayBuilder sizeStatistics = Json.createArrayBuilder();
		JsonArrayBuilder waitStatistics = Json.createArrayBuilder();
		if(pipelineSize != null) {
			sizeStatistics.add(pipelineSize);
		}
		if(pipelineWaitTime != null) {
			waitStatistics.add(pipelineWaitTime);
		}

		List<String> pipelineMeterOrder = getPotentialMeters(adapter.getPipeLine());
		for(String meterName : pipelineMeterOrder) {
			JsonObjectBuilder pipeDuration = pipeDurations.remove(meterName);
			if(pipeDuration != null) {
				durationStatistics.add(pipeDuration);
			}
			JsonObjectBuilder inStat = pipeSizeIn.remove(meterName);
			if(inStat != null) {
				sizeStatistics.add(inStat);
			}
			JsonObjectBuilder outStat = pipeSizeOut.remove(meterName);
			if(outStat != null) {
				sizeStatistics.add(outStat);
			}
			JsonObjectBuilder waitTime = pipeWaitTime.remove(meterName);
			if(waitTime != null) {
				waitStatistics.add(waitTime);
			}
		}
		pipeDurations.values().stream().forEach(durationStatistics::add); //Add whatever remains (unsorted)

		JsonObjectBuilder root = Json.createObjectBuilder();
		root.add("durationPerPipe", durationStatistics);
		root.add("sizePerPipe", sizeStatistics);
		root.add("waitTimePerPipe", waitStatistics);

		root.add("pipeline", getPipelineStats(adapterScopedMeters));

		root.add("receivers", getReceivers(adapter, adapterScopedMeters));
		root.add("hourly", getHourlyStatistics(adapter));

		JsonObjectBuilder totalMessageProcessingTime = getDistributionSummary(adapterScopedMeters, FrankMeterType.PIPELINE_DURATION);
		if(totalMessageProcessingTime != null) {
			root.add("totalMessageProccessingTime", totalMessageProcessingTime);
		}

		return root.build();
	}

	private JsonObjectBuilder getPipelineStats(Collection<Meter> adapterScopedMeters) {
		JsonObjectBuilder root = Json.createObjectBuilder();
		root.add("inError", getCounter(adapterScopedMeters, FrankMeterType.PIPELINE_IN_ERROR));
		root.add("processed", getCounter(adapterScopedMeters, FrankMeterType.PIPELINE_PROCESSED));
		root.add("inProcess", getGauge(adapterScopedMeters, FrankMeterType.PIPELINE_IN_PROCESS));
		return root;
	}

	private double getCounter(Collection<Meter> meters, FrankMeterType type) {
		List<Counter> counter = meters.stream().filter(type::isOfType).map(Counter.class::cast).toList();
		if(counter.size() > 1) {
			throw new IllegalStateException("found more then 1 "+type+" statistic");
		}
		if(counter.isEmpty()) {
			return 0;
		}
		return counter.get(0).count();
	}

	private double getGauge(Collection<Meter> meters, FrankMeterType type) {
		List<Gauge> counter = meters.stream().filter(type::isOfType).map(Gauge.class::cast).toList();
		if(counter.size() > 1) {
			throw new IllegalStateException("found more then 1 "+type+" statistic");
		}
		if(counter.isEmpty()) {
			return 0;
		}
		return counter.get(0).value();
	}

	private JsonObjectBuilder getDistributionSummary(Collection<Meter> meters, FrankMeterType type) {
		List<LocalDistributionSummary> duration = meters.stream().filter(type::isOfType).map(LocalDistributionSummary.class::cast).toList();
		if(duration.size() > 1) {
			throw new IllegalStateException("found more then 1 "+type+" statistic");
		}
		if(duration.isEmpty()) {
			return null;
		}
		LocalDistributionSummary summary = duration.get(0);
		return readSummary(summary, extractNameFromTag(summary, "name"));
	}

	private JsonArrayBuilder getHourlyStatistics(Adapter adapter) {
		long[] numOfMessagesStartProcessingByHour = adapter.getNumOfMessagesStartProcessingByHour();
		JsonArrayBuilder hourslyStatistics = Json.createArrayBuilder();
		for (int i=0; i<numOfMessagesStartProcessingByHour.length; i++) {
			JsonObjectBuilder item = Json.createObjectBuilder();
			item.add("time", "%02d:00".formatted(i));
			item.add("count", numOfMessagesStartProcessingByHour[i]);
			hourslyStatistics.add(item);
		}
		return hourslyStatistics;
	}

	private JsonArrayBuilder getReceivers(Adapter adapter, Collection<Meter> meters) {
		JsonArrayBuilder receivers = Json.createArrayBuilder();

		for (Receiver<?> receiver: adapter.getReceivers()) {
			if(!receiver.configurationSucceeded()) continue;

			JsonObjectBuilder receiverMap = Json.createObjectBuilder();

			receiverMap.add("name", receiver.getName());
			receiverMap.add("messagesReceived", receiver.getMessagesReceived());
			receiverMap.add("messagesRejected", receiver.getMessagesRejected());
			receiverMap.add("messagesRetried", receiver.getMessagesRetried());

			receiverMap.add("processing", getDistributionSummaryByThread(meters, FrankMeterType.RECEIVER_DURATION));

			receivers.add(receiverMap);
		}
		return receivers;
	}

	private Map<String, JsonObjectBuilder> getDistributionSummaries(Collection<Meter> meters, FrankMeterType meterType, String nameSuffix) {
		return meters.stream()
				.filter(meterType::isOfType)
				.map(LocalDistributionSummary.class::cast)
				.collect(Collectors.toMap(e -> extractNameFromTag(e, "name"), e -> readSummary(e, extractNameFromTag(e, "name")+ nameSuffix)));
	}
	private JsonArrayBuilder getDistributionSummaryByThread(Collection<Meter> meters, FrankMeterType meterType) {
		JsonArrayBuilder threadsStats = Json.createArrayBuilder();

		meters.stream()
				.filter(meterType::isOfType)
				.map(LocalDistributionSummary.class::cast)
				.map(e -> readSummary(e, extractNameFromTag(e, "thread") +" threads processing"))
				.forEach(threadsStats::add);

		return threadsStats;
	}

	private static @Nonnull String extractNameFromTag(Meter meter, String groupByTagName) {
		String name = meter.getId().getTag(groupByTagName);
		if(name != null) {
			return name;
		}
		throw new IllegalStateException("tag ["+groupByTagName+"] not found. tags found "+meter.getId().getTags());
	}

	private List<String> getPotentialMeters(PipeLine pipeline) {
		List<String> potentialMeterNames = new ArrayList<>();
		if(pipeline.getInputValidator() != null) {
			potentialMeterNames.add(pipeline.getInputValidator().getName());
		}
		if(pipeline.getOutputValidator() != null) {
			potentialMeterNames.add(pipeline.getOutputValidator().getName());
		}
		if(pipeline.getInputWrapper() != null) {
			potentialMeterNames.add(pipeline.getInputWrapper().getName());
		}
		if(pipeline.getOutputWrapper() != null) {
			potentialMeterNames.add(pipeline.getOutputWrapper().getName());
		}

		for(IPipe pipe : pipeline.getPipes()) {
			potentialMeterNames.add(pipe.getName());
			if(pipe instanceof MessageSendingPipe sendingPipe) {
				potentialMeterNames.addAll(getSendingPipeMeters(sendingPipe));
			}
		}
		return potentialMeterNames;
	}

	// MessageSendingPipe has 3 additional meters for validators/wrappers and a messagelog.
	private List<String> getSendingPipeMeters(MessageSendingPipe messageSendingPipe) {
		List<String> potentialMeterNames = new ArrayList<>();
		if (messageSendingPipe.getInputValidator() != null || messageSendingPipe.getInputWrapper() != null) {
			potentialMeterNames.add(messageSendingPipe.getName() + " -> PreProcessing");
		}
		if (messageSendingPipe.getOutputValidator() != null || messageSendingPipe.getOutputWrapper() != null) {
			potentialMeterNames.add(messageSendingPipe.getName() + " -> PostProcessing");
		}
		if (messageSendingPipe.getMessageLog() != null) {
			potentialMeterNames.add(messageSendingPipe.getName() + " -> MessageLog");
		}
		return potentialMeterNames;
	}

	private static JsonObjectBuilder readSummary(LocalDistributionSummary summary, String name) {
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

		//le tags, 100ms/1000ms/2000ms/10000ms
		for (CountAtBucket bucket : snapshot.histogramCounts()) {
			String key = DECIMAL_FORMAT.format(bucket.bucket()) + unit;
			double value = bucket.count();
			jsonSummary.add(key, value);
		}

		//phi tags, p50/p90/p95/p98
		for (ValueAtPercentile percentile : snapshot.percentileValues()) {
			String key = "p" + DECIMAL_FORMAT.format(percentile.percentile() * 100);
			jsonSummary.add(key, format(percentile.value()));
		}

		return jsonSummary;
	}

	private static double format(double val) {
		if (Double.isInfinite(val) || Double.isNaN(val)) {
			return 0;
		}
		return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
	}
}
