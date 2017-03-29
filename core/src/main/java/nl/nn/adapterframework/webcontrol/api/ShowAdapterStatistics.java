/*
Copyright 2016 Integration Partners B.V.

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.ItemList;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.DateUtils;

/**
* Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowAdapterStatistics extends Base {
	@Context ServletConfig servletConfig;

	private DecimalFormat countFormat=new DecimalFormat(ItemList.PRINT_FORMAT_COUNT);
	private DecimalFormat timeFormat=new DecimalFormat(ItemList.PRINT_FORMAT_TIME);
	private DecimalFormat percentageFormat=new DecimalFormat(ItemList.PRINT_FORMAT_PERC);

	@GET
	@RolesAllowed({"ObserverAccess", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/statistics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedules(@PathParam("adapterName") String adapterName) throws ApiException {
		initBase(servletConfig);

		Map<String, Object> statisticsMap = new HashMap<String, Object>();

		Adapter adapter = (Adapter)ibisManager.getRegisteredAdapter(adapterName);

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
		Iterator<?> recIt=adapter.getReceiverIterator();
		if (recIt.hasNext()) {
			while (recIt.hasNext()) {
				IReceiver receiver=(IReceiver) recIt.next();
				Map<String, Object> receiverMap = new HashMap<String, Object>();

				receiverMap.put("name", receiver.getName());
				receiverMap.put("class", receiver.getClass().getName());
				receiverMap.put("messagesReceived", receiver.getMessagesReceived());
				receiverMap.put("messagesRetried", receiver.getMessagesRetried());

				if (receiver instanceof IReceiverStatistics) {
					IReceiverStatistics statReceiver = (IReceiverStatistics)receiver;
					Iterator<?> statsIter;
					statsIter = statReceiver.getProcessStatisticsIterator();
					if (statsIter != null) {
						ArrayList<Map<String, Object>> procStatsMap = new ArrayList<Map<String, Object>>();
//						procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getRequestSizeStatistics(), "stat"));
//						procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getResponseSizeStatistics(), "stat"));
						while(statsIter.hasNext()) {
							StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
							procStatsMap.add(statisticsKeeperToMapBuilder(pstat));
						}
						receiverMap.put("processing", procStatsMap);
					}

					statsIter = statReceiver.getIdleStatisticsIterator();
					if (statsIter != null) {
						ArrayList<Map<String, Object>> idleStatsMap = new ArrayList<Map<String, Object>>();
						while(statsIter.hasNext()) {
							StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
							idleStatsMap.add(statisticsKeeperToMapBuilder(pstat));
						}
						receiverMap.put("idle", idleStatsMap);
					}

					receivers.add(receiverMap);
				}
			}
		}
		statisticsMap.put("receivers", receivers);

		Map<String, Object> tmp = new HashMap<String, Object>();
		StatisticsKeeperToXml handler = new StatisticsKeeperToXml(tmp);
		handler.configure();
		Object handle = handler.start(null, null, null);
		try {
			adapter.getPipeLine().iterateOverStatistics(handler, tmp, HasStatistics.STATISTICS_ACTION_FULL);
			statisticsMap.put("durationPerPipe", tmp.get("pipeStats"));
			statisticsMap.put("sizePerPipe", tmp.get("sizeStats"));
		} catch (SenderException e) {
			log.error(e);
		} finally {
			handler.end(handle);
		}

		return Response.status(Response.Status.CREATED).entity(statisticsMap).build();
	}

	private class StatisticsKeeperToXml implements StatisticsKeeperIterationHandler {

		private Object parent;

		public StatisticsKeeperToXml(Object parent) {
			super();
			this.parent=parent;
		}

		public void configure() {
		}

		public Object start(Date now, Date mainMark, Date detailMark) {
			return parent;
		}
		public void end(Object data) {
		}

		@SuppressWarnings("unchecked")
		public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) {
			if(sk == null) return;

			((Map<String, Object>) data).put(sk.getName(), statisticsKeeperToMapBuilder(sk));
		}

		public void handleScalar(Object data, String scalarName, long value){
			handleScalar(data,scalarName,""+value);
		}
		public void handleScalar(Object data, String scalarName, Date value){
			String result;
			if (value!=null) {
				result = DateUtils.format(value, DateUtils.FORMAT_FULL_GENERIC);
			} else {
				result = "-";
			}
			handleScalar(data,scalarName,result);
		}
		public void handleScalar(Object data, String scalarName, String value) {
			//Not applicable for HashMaps...
		}

		@SuppressWarnings("unchecked")
		public Object openGroup(Object parentData, String name, String type) {
			Map<String, Object> o = new HashMap<String, Object>();
			((Map<String, Object>) parentData).put(type, o);
			return o;
		}

		public void closeGroup(Object data) {
		}
	}

	protected Map<String, Object> statisticsKeeperToMapBuilder(StatisticsKeeper sk) {
		if (sk==null) {
			return null;
		}

		Map<String, Object> tmp = new HashMap<String, Object>();
		for (int i=0; i<sk.getItemCount(); i++) {
			Object item = sk.getItemValue(i);
			String key = sk.getItemName(i).replace("< ", "");
			if (item==null) {
				tmp.put(key, ItemList.ITEM_VALUE_NAN);
			} else {
				String value = "";
				switch (sk.getItemType(i)) {
					case ItemList.ITEM_TYPE_INTEGER: 
						if (countFormat==null) {
							value = ""+ (Long)item;
						} else {
							value = countFormat.format((Long)item);
						}
						break;
					case ItemList.ITEM_TYPE_TIME: 
						value = timeFormat.format(item);
						break;
					case ItemList.ITEM_TYPE_FRACTION:
						value = percentageFormat.format(((Double)item).doubleValue()*100)+ "%";
						break;
				}
				tmp.put(key, value);
			}
		}
		return tmp;
	}
}
