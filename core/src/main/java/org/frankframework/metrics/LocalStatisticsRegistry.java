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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.frankframework.core.Adapter;
import org.frankframework.core.IAdapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeLine;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.receivers.Receiver;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramGauges;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import lombok.Getter;

public class LocalStatisticsRegistry extends SimpleMeterRegistry {
	private static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#");

	public LocalStatisticsRegistry(SimpleConfig config) {
		super(config, Clock.SYSTEM);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id, DistributionStatisticConfig config, double scale) {
		DistributionSummary summary = new LocalDistributionSummary(id, clock, config, scale);

		HistogramGauges.registerWithCommonFormat(summary, this);

		return summary;
	}

	public JsonStructure scrape(String configuration, IAdapter adapter) {
		List<Tag> searchTags = new ArrayList<>();
		searchTags.add(Tag.of("configuration", configuration));
		searchTags.add(Tag.of("adapter", adapter.getName()));

		List<String> pipelineMeterOrder = getPotentialMeters(adapter.getPipeLine());

		Search search = Search.in(this).tags(searchTags);
		Collection<Meter> adapterScopedMeters = search.meters();


		JsonObjectBuilder root = Json.createObjectBuilder();

		Map<String, JsonObjectBuilder> listOfAllMeters = getDistributionSummary(adapterScopedMeters, FrankMeterType.PIPE_DURATION);
		JsonArrayBuilder jsonArray = Json.createArrayBuilder();
		for(String meterName : pipelineMeterOrder) {
			JsonObjectBuilder meterJsonDing = listOfAllMeters.remove(meterName);
			if(meterJsonDing != null) {
				jsonArray.add(meterJsonDing);
			}
		}
		listOfAllMeters.values().stream().forEach(jsonArray::add);
		root.add("durationPerPipe", jsonArray);

		Map<String, JsonObjectBuilder> listOfAllMeters2 = getDistributionSummary(adapterScopedMeters, FrankMeterType.PIPE_SIZE);
		JsonArrayBuilder jsonArray2 = Json.createArrayBuilder();
		jsonArray2.add(listOfAllMeters2.remove("- pipeline in"));
		for(String meterName : pipelineMeterOrder) {
			JsonObjectBuilder inStat = listOfAllMeters2.remove(meterName + " (in)");
			JsonObjectBuilder outStat = listOfAllMeters2.remove(meterName + " (out)");
			if(inStat != null) {
				jsonArray2.add(inStat);
			}
			if(outStat != null) {
				jsonArray2.add(outStat);
			}
		}
		root.add("sizePerPipe", jsonArray2);

		root.add("receivers", getReceivers(adapter, adapterScopedMeters));
		root.add("hourly", getHourlyStatistics((Adapter) adapter));

		root.add("totalMessageProccessingTime", getTotalMessageProccessingTime(adapterScopedMeters));

		return root.build();
	}

	private JsonObjectBuilder getTotalMessageProccessingTime(Collection<Meter> meters) {
		List<LocalDistributionSummary> duration = meters.stream().filter(FrankMeterType.PIPELINE_DURATION::isOfType).map(LocalDistributionSummary.class::cast).toList();
		if(duration.size() > 1) {
			throw new IllegalStateException("found more then 1 "+FrankMeterType.PIPELINE_DURATION+" statistic");
		}
		if(duration.isEmpty()) {
			throw new IllegalStateException("did not find any "+FrankMeterType.PIPELINE_DURATION+" statistic");
		}
		return readSummary(duration.get(0));
	}

	private JsonArrayBuilder getHourlyStatistics(Adapter adapter) {
		long[] numOfMessagesStartProcessingByHour = adapter.getNumOfMessagesStartProcessingByHour();
		JsonArrayBuilder hourslyStatistics = Json.createArrayBuilder();
		for (int i=0; i<numOfMessagesStartProcessingByHour.length; i++) {
			JsonObjectBuilder item = Json.createObjectBuilder();
			String startTime;
			if (i<10) {
				startTime = "0" + i + ":00";
			} else {
				startTime = i + ":00";
			}
			item.add("time", startTime);
			item.add("count", numOfMessagesStartProcessingByHour[i]);
			hourslyStatistics.add(item);
		}
		return hourslyStatistics;
	}

	private JsonArrayBuilder getReceivers(IAdapter adapter, Collection<Meter> meters) {
		JsonArrayBuilder receivers = Json.createArrayBuilder();
		Map<String, Double> received = getCounter(meters, FrankMeterType.RECEIVER_RECEIVED, "receiver");
		Map<String, Double> rejected = getCounter(meters, FrankMeterType.RECEIVER_REJECTED, "receiver");
		Map<String, Double> retried = getCounter(meters, FrankMeterType.RECEIVER_RETRIED, "receiver");

		for (Receiver<?> receiver: adapter.getReceivers()) {
			JsonObjectBuilder receiverMap = Json.createObjectBuilder();

			receiverMap.add("name", receiver.getName());
			receiverMap.add("messagesReceived", received.get(receiver.getName()));
			receiverMap.add("messagesRejected", rejected.get(receiver.getName()));
			receiverMap.add("messagesRetried", retried.get(receiver.getName()));

//			receiverMap.put("processing", null); //TBD
//			receiverMap.put("idle", null); //TBD

			receivers.add(receiverMap);
		}
		return receivers;
	}

	private Map<String, JsonObjectBuilder> getDistributionSummary(Collection<Meter> meters, FrankMeterType meterType) {
		return meters.stream()
				.filter(meterType::isOfType)
				.map(LocalDistributionSummary.class::cast)
				.collect(Collectors.toMap(e->extractNameFromTag(e, "name"), LocalStatisticsRegistry::readSummary));
	}

	private Map<String, Double> getCounter(Collection<Meter> meters, FrankMeterType meterType, String groupByTagName) {
		return meters.stream()
				.filter(meterType::isOfType)
				.map(Counter.class::cast)
				.collect(Collectors.toMap(e->extractNameFromTag(e, groupByTagName), Counter::count));
	}

	private static @Nonnull String extractNameFromTag(Meter meter, String groupByTagName) {
		String name = meter.getId().getTag(groupByTagName);
		if(name != null) {
			return name;
		}
		throw new IllegalStateException("tag ["+groupByTagName+"] not found. tags found "+meter.getId().getTags());
	}

	enum FrankMeterType {
		TOTAL_MESSAGES_IN_ERROR("frank.messagesInError", Meter.Type.COUNTER),
		TOTAL_MESSAGES_PROCESSED("frank.messagesProcessed", Meter.Type.COUNTER),
		TOTAL_MESSAGES_REJECTED("frank.messagesRejected", Meter.Type.COUNTER),

		PIPE_DURATION("frank.pipe.duration", Meter.Type.DISTRIBUTION_SUMMARY),
		PIPE_SIZE("frank.pipe.msgsize", Meter.Type.DISTRIBUTION_SUMMARY),

		PIPELINE_DURATION("frank.pipeline.duration", Meter.Type.DISTRIBUTION_SUMMARY),
		PIPELINE_IN_ERROR("frank.pipeline.messagesInError", Meter.Type.COUNTER),
		PIPELINE_PROCESSED("frank.pipeline.messagesProcessed", Meter.Type.COUNTER),

		RECEIVER_RECEIVED("frank.receiver.messagesReceived", Meter.Type.COUNTER),
		RECEIVER_REJECTED("frank.receiver.messagesRejected", Meter.Type.COUNTER),
		RECEIVER_RETRIED("frank.receiver.messagesRetried", Meter.Type.COUNTER);

		private final @Getter String meterName;
		private final Type type;
		private FrankMeterType(String meterName, Type type) {
			this.meterName = meterName;
			this.type = type;
		}

		boolean isOfType(Meter meter) {
			return type == meter.getId().getType() && meterName.equals(meter.getId().getName());
		}
	}

	private List<String> getPotentialMeters(PipeLine pipeline) {
		List<String> potentialMeterNames = new LinkedList<>();
		IValidator inputValidator = pipeline.getInputValidator();
		if(inputValidator != null) {
			potentialMeterNames.add(inputValidator.getName());
		}
		IValidator outputValidator = pipeline.getOutputValidator();
		if(outputValidator != null) {
			potentialMeterNames.add(outputValidator.getName());
		}

//		pipes.addAll(pipeLine.getPipes()); //ensure this is sorted
		for(IPipe pipe : pipeline.getPipes()) {
			//TODO validators/wrappers
//			String pipeName = pipe.getName();
			if(pipe instanceof MessageSendingPipe) {
				potentialMeterNames.add("getConnection for "+((MessageSendingPipe) pipe).getSender().getName()); //TODO implement getStatisticName
			}
			potentialMeterNames.add(pipe.getName());
		}
		return potentialMeterNames;
	}

	private static JsonObjectBuilder readSummary(LocalDistributionSummary summary) {
		String name = extractNameFromTag(summary, "name");
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
		jsonSummary.add("unit", unit);

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
