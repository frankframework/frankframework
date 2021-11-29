/*
Copyright 2016-2020 Integration Partners B.V.

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

import java.math.BigDecimal;
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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.ItemList;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.DateUtils;

/**
 * Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
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

		Map<String, Object> statisticsMap = new HashMap<String, Object>();

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		StatisticsKeeper sk = adapter.getStatsMessageProcessingDuration();
		statisticsMap.put("totalMessageProccessingTime", statisticsKeeperToMapBuilder(sk));

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

			ArrayList<Map<String, Object>> procStatsMap = new ArrayList<Map<String, Object>>();
//			procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getRequestSizeStatistics(), "stat"));
//			procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getResponseSizeStatistics(), "stat"));
			for (StatisticsKeeper pstat: receiver.getProcessStatistics()) {
				procStatsMap.add(statisticsKeeperToMapBuilder(pstat));
			}
			receiverMap.put("processing", procStatsMap);

			ArrayList<Map<String, Object>> idleStatsMap = new ArrayList<Map<String, Object>>();
			for (StatisticsKeeper istat: receiver.getIdleStatistics()) {
				idleStatsMap.add(statisticsKeeperToMapBuilder(istat));
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
			adapter.getPipeLine().iterateOverStatistics(handler, tmp, HasStatistics.STATISTICS_ACTION_FULL);
			statisticsMap.put("durationPerPipe", tmp.get("pipeStats"));
			statisticsMap.put("sizePerPipe", tmp.get("sizeStats"));
		} catch (SenderException e) {
			log.error("unable to parse pipeline statistics", e);
		} finally {
			handler.end(handle);
		}

		return Response.status(Response.Status.CREATED).entity(statisticsMap).build();
	}

	private class StatisticsKeeperToMap implements StatisticsKeeperIterationHandler {

		private Object parent;

		public StatisticsKeeperToMap(Object parent) {
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

			((List<Object>) data).add(statisticsKeeperToMapBuilder(sk));
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

	protected Map<String, Object> statisticsKeeperToMapBuilder(StatisticsKeeper sk) {
		if (sk==null) {
			return null;
		}

		Map<String, Object> tmp = new HashMap<String, Object>();
		tmp.put("name", sk.getName());
		for (int i=0; i<sk.getItemCount(); i++) {
			Object item = sk.getItemValue(i);
			String key = sk.getItemName(i).replace("< ", "");
			if (item==null) {
				tmp.put(key, null);
			} else {
				switch (sk.getItemType(i)) {
					case ItemList.ITEM_TYPE_INTEGER:
						tmp.put(key, item);
						break;
					case ItemList.ITEM_TYPE_TIME:
						if(item instanceof Long) {
							tmp.put(key, item);
						} else {
							Double val = (Double) item;
							if(val.isNaN() || val.isInfinite()) {
								tmp.put(key, null);
							} else {
								tmp.put(key, new BigDecimal(val).setScale(1, BigDecimal.ROUND_HALF_EVEN));
							}
						}
						break;
					case ItemList.ITEM_TYPE_FRACTION:
						Double val = (Double) item;
						if(val.isNaN() || val.isInfinite()) {
							tmp.put(key, null);
						} else {
							tmp.put(key, new BigDecimal(((Double) item).doubleValue()*100).setScale(1,  BigDecimal.ROUND_HALF_EVEN));
						}
						break;
				}
			}
		}
		return tmp;
	}
}
