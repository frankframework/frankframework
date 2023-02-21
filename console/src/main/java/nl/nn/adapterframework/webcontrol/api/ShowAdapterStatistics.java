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
package nl.nn.adapterframework.webcontrol.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.management.web.ApiException;
import nl.nn.adapterframework.management.web.Relation;
import nl.nn.adapterframework.receivers.PullingListenerContainer;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.statistics.HasStatistics.Action;
import nl.nn.adapterframework.statistics.ScalarMetricBase;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.DateUtils;

/**
 * Retrieves the statistics
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowAdapterStatistics extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/statistics")
	@Relation("statistics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStatistics(@PathParam("adapterName") String adapterName) throws ApiException {

		// TODO: implement this method using a StatisticsKeeperIterationHandler
		Map<String, Object> statisticsMap = new HashMap<String, Object>();

		statisticsMap.put("labels", StatisticsKeeper.getLabels());
		statisticsMap.put("types", StatisticsKeeper.getTypes());

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		StatisticsKeeper sk = adapter.getStatsMessageProcessingDuration();
		statisticsMap.put("totalMessageProccessingTime", sk.asMap());

		long[] numOfMessagesStartProcessingByHour = adapter.getNumOfMessagesStartProcessingByHour();
		List<Map<String, Object>> hourslyStatistics = new ArrayList<Map<String, Object>>();
		for (int i=0; i<numOfMessagesStartProcessingByHour.length; i++) {
			Map<String, Object> item = new HashMap<String, Object>(2);
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

		List<Map<String, Object>> receivers = new ArrayList<Map<String, Object>>();
		for (Receiver<?> receiver: adapter.getReceivers()) {
			Map<String, Object> receiverMap = new HashMap<String, Object>();

			receiverMap.put("name", receiver.getName());
			receiverMap.put("class", receiver.getClass().getName());
			receiverMap.put("messagesReceived", receiver.getMessagesReceived());
			receiverMap.put("messagesRetried", receiver.getMessagesRetried());

			if (receiver.getListener() instanceof IPullingListener) {
				ArrayList<Map<String, Object>> receiveStatsMap = new ArrayList<Map<String, Object>>();
				if (receiver.getListenerContainer()!=null) {
					PullingListenerContainer container = receiver.getListenerContainer();
					if (container.getMessagePeekingStatistics()!=null) {
						receiveStatsMap.add(container.getMessagePeekingStatistics().asMap());
					}
					receiveStatsMap.add(container.getMessageReceivingStatistics().asMap());
					receiveStatsMap.add(receiver.getMessageExtractionStatistics().asMap());
				}
				receiverMap.put("receiving", receiveStatsMap);
			}

			ArrayList<Map<String, Object>> procStatsMap = new ArrayList<Map<String, Object>>();
//			procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getRequestSizeStatistics(), "stat"));
//			procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getResponseSizeStatistics(), "stat"));
			for (StatisticsKeeper pstat: receiver.getProcessStatistics()) {
				procStatsMap.add(pstat.asMap());
			}
			receiverMap.put("processing", procStatsMap);

			ArrayList<Map<String, Object>> idleStatsMap = new ArrayList<Map<String, Object>>();
			for (StatisticsKeeper istat: receiver.getIdleStatistics()) {
				idleStatsMap.add(istat.asMap());
			}
			receiverMap.put("idle", idleStatsMap);

			receivers.add(receiverMap);
		}
		statisticsMap.put("receivers", receivers);

		Map<String, Object> tmp = new HashMap<String, Object>();
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

		return Response.status(Response.Status.CREATED).entity(statisticsMap).build();
	}

	private class StatisticsKeeperToMap implements StatisticsKeeperIterationHandler {

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
		public void handleScalar(Object data, String scalarName, ScalarMetricBase meter) throws SenderException {
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
				result = DateUtils.format(value, DateUtils.FORMAT_FULL_GENERIC);
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
			List<Object> o = new LinkedList<Object>();
			((Map<String, Object>) parentData).put(type, o);
			return o;
		}

		@Override
		public void closeGroup(Object data) {
		}

	}

}
