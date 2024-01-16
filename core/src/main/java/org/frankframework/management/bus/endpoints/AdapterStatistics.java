/*
   Copyright 2016-2023 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.frankframework.management.bus.TopicSelector;
import org.springframework.messaging.Message;

import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.core.SenderException;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.JsonResponseMessage;
import org.frankframework.receivers.Receiver;
import org.frankframework.statistics.HasStatistics.Action;
import org.frankframework.statistics.ScalarMetricBase;
import org.frankframework.statistics.StatisticsKeeper;
import org.frankframework.statistics.StatisticsKeeperIterationHandler;
import org.frankframework.util.DateFormatUtils;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.ADAPTER)
public class AdapterStatistics extends BusEndpointBase {

	@ActionSelector(BusAction.STATUS)
	public Message<String> getStatistics(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		Map<String, Object> statisticsMap = new HashMap<>();
		statisticsMap.put("labels", StatisticsKeeper.getLabels());
		statisticsMap.put("types", StatisticsKeeper.getTypes());

		StatisticsKeeper sk = adapter.getStatsMessageProcessingDuration();
		statisticsMap.put("totalMessageProccessingTime", sk.asMap());

		long[] numOfMessagesStartProcessingByHour = adapter.getNumOfMessagesStartProcessingByHour();
		List<Map<String, Object>> hourslyStatistics = new ArrayList<>();
		for (int i=0; i<numOfMessagesStartProcessingByHour.length; i++) {
			Map<String, Object> item = new HashMap<>(2);
			String startTime;
			if (i<10) {
				startTime = "0" + i + ":00";
			} else {
				startTime = i + ":00";
			}
			item.put("time", startTime);
			item.put("count", numOfMessagesStartProcessingByHour[i]);
			hourslyStatistics.add(item);
		}
		statisticsMap.put("hourly", hourslyStatistics);

		List<Map<String, Object>> receivers = new ArrayList<>();
		for (Receiver<?> receiver: adapter.getReceivers()) {
			Map<String, Object> receiverMap = new HashMap<>();

			receiverMap.put("name", receiver.getName());
			receiverMap.put("class", receiver.getClass().getName());
			receiverMap.put("messagesReceived", receiver.getMessagesReceived());
			receiverMap.put("messagesRetried", receiver.getMessagesRetried());

			ArrayList<Map<String, Object>> procStatsMap = new ArrayList<>();
			for (StatisticsKeeper pstat: receiver.getProcessStatistics()) {
				procStatsMap.add(pstat.asMap());
			}
			receiverMap.put("processing", procStatsMap);

			ArrayList<Map<String, Object>> idleStatsMap = new ArrayList<>();
			for (StatisticsKeeper istat: receiver.getIdleStatistics()) {
				idleStatsMap.add(istat.asMap());
			}
			receiverMap.put("idle", idleStatsMap);

			receivers.add(receiverMap);
		}
		statisticsMap.put("receivers", receivers);

		Map<String, Object> tmp = new HashMap<>();
		StatisticsKeeperToMap handler = new StatisticsKeeperToMap(tmp);
		handler.configure();
		Object handle = handler.start(null, null, null);
		try {
			adapter.getPipeLine().iterateOverStatistics(handler, tmp, Action.FULL);
			statisticsMap.put("durationPerPipe", tmp.get(PipeLine.PIPELINE_DURATION_STATS));
			statisticsMap.put("sizePerPipe", tmp.get(PipeLine.PIPELINE_SIZE_STATS));
		} catch (SenderException e) {
			log.error("unable to parse pipeline statistics", e);
		} finally {
			handler.end(handle);
		}

		return new JsonResponseMessage(statisticsMap);
	}

	private static class StatisticsKeeperToMap implements StatisticsKeeperIterationHandler {
		private Map<String, Object> parent;

		public StatisticsKeeperToMap(Map<String, Object> parent) {
			super();
			this.parent=parent;
		}

		@Override
		public void configure() {
		}

		@Override
		public Object start(Date now, Date mainMark, Date detailMark) {
			return parent;
		}

		@Override
		public void end(Object data) {
		}

		@Override
		@SuppressWarnings("unchecked")
		public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) {
			if(sk == null) return;

			((List<Object>) data).add(sk.asMap());
		}

		@Override
		public void handleScalar(Object data, String scalarName, ScalarMetricBase meter) {
			handleScalar(data, scalarName, meter.getValue());
		}

		@Override
		public void handleScalar(Object data, String scalarName, long value) {
			handleScalar(data, scalarName, ""+value);
		}

		@Override
		public void handleScalar(Object data, String scalarName, Date value) {
			String result;
			if (value!=null) {
				result = DateFormatUtils.format(value, DateFormatUtils.FULL_GENERIC_FORMATTER);
			} else {
				result = "-";
			}
			handleScalar(data, scalarName, result);
		}

		public void handleScalar(Object data, String scalarName, String value) {
			//Not applicable for HashMaps...
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object openGroup(Object parentData, String name, String type) {
			List<Object> o = new LinkedList<>();
			((Map<String, Object>) parentData).put(type, o);
			return o;
		}

		@Override
		public void closeGroup(Object data) {
		}

	}

}
