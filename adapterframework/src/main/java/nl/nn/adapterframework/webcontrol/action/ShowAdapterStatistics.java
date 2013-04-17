/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.ItemList;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * <code>Action</code> to retrieve the statistics from a
 * specific adapter. The pipeline statistics are sorted by
 * pipename.
 * @version $Id$
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.PipeLine
 * @see nl.nn.adapterframework.core.Adapter
 */
public class ShowAdapterStatistics extends ActionBase {

    private DecimalFormat df=new DecimalFormat(ItemList.ITEM_FORMAT_TIME);
    private DecimalFormat pf=new DecimalFormat(ItemList.ITEM_FORMAT_PERC);
    
   
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	
	    String adapterName = request.getParameter("adapterName");
	    String deepString =  request.getParameter("deep");
	    boolean deep = "true".equals(deepString);
	
	    Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);
	    XmlBuilder adapterXML = new XmlBuilder("adapterStatistics");
	
	    adapterXML.addAttribute("name", adapter.getName());
	    adapterXML.addAttribute("state", adapter.getRunState().toString());
	    adapterXML.addAttribute("upSince", adapter.getStatsUpSince());
	    adapterXML.addAttribute("lastMessageDate", adapter.getLastMessageDate());
	    adapterXML.addAttribute("messagesInProcess", ""+adapter.getNumOfMessagesInProcess());
	    adapterXML.addAttribute("messagesProcessed", ""+adapter.getNumOfMessagesProcessed());
	    adapterXML.addAttribute("messagesInError",   ""+adapter.getNumOfMessagesInError());
	    
	    StatisticsKeeper st = adapter.getStatsMessageProcessingDuration();

	    adapterXML.addSubElement(statisticsKeeperToXmlBuilder(st, "messageProcessingDuration", deep));
	
		XmlBuilder messagesReceivedByHour = new XmlBuilder("messagesStartProcessingByHour");
		adapterXML.addSubElement(messagesReceivedByHour);
		long[] numOfMessagesStartProcessingByHour = adapter.getNumOfMessagesStartProcessingByHour();
		for (int i=0; i<numOfMessagesStartProcessingByHour.length; i++) {
			XmlBuilder item = new XmlBuilder("item");
			messagesReceivedByHour.addSubElement(item);
			String startTime;
			if (i<10) {
				startTime = "0" + i + ":00";
			} else {
				startTime = i + ":00";
			}
			item.addAttribute("startTime", startTime);
			item.addAttribute("count", numOfMessagesStartProcessingByHour[i]);
		}
	    
	    Iterator recIt=adapter.getReceiverIterator();
	    if (recIt.hasNext()) {
			XmlBuilder receiversXML=new XmlBuilder("receivers");
		    while (recIt.hasNext()) {
				IReceiver receiver=(IReceiver) recIt.next();
			    XmlBuilder receiverXML=new XmlBuilder("receiver");
			    receiversXML.addSubElement(receiverXML);
	
			    receiverXML.addAttribute("name",receiver.getName());
				receiverXML.addAttribute("class", receiver.getClass().getName());
				receiverXML.addAttribute("messagesReceived", ""+receiver.getMessagesReceived());
				receiverXML.addAttribute("messagesRetried", ""+receiver.getMessagesRetried());
	/*				  
			    if (receiver instanceof HasSender) {
					ISender sender = ((HasSender) receiver).getSender();
				          if (sender != null) 
					          	receiverXML.addAttribute("senderName", sender.getName());
	 	        }
	*/		    
			    if (receiver instanceof IReceiverStatistics) {
	
				    IReceiverStatistics statReceiver = (IReceiverStatistics)receiver;
				    Iterator statsIter;
	
				    statsIter = statReceiver.getProcessStatisticsIterator();
				    if (statsIter != null) {
					    XmlBuilder procStatsXML = new XmlBuilder("procStats");
//						procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getRequestSizeStatistics(), "stat"));
//						procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(statReceiver.getResponseSizeStatistics(), "stat"));
					    while(statsIter.hasNext()) {				    
					        StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
					        procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(pstat, "stat", deep));
				        }
						receiverXML.addSubElement(procStatsXML);
				    }
	
				    statsIter = statReceiver.getIdleStatisticsIterator();
				    if (statsIter != null) {
				        XmlBuilder procStatsXML = new XmlBuilder("idleStats");
					    while(statsIter.hasNext()) {				    
					        StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
					        procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(pstat, "stat", deep));
			  	      	}
						receiverXML.addSubElement(procStatsXML);
				    }
	
	
				}
			}
	        adapterXML.addSubElement(receiversXML); 
	    }
	
	    
		StatisticsKeeperToXml handler = new StatisticsKeeperToXml(adapterXML, deep);
		handler.configure();
		Object handle = handler.start(null,null,null);
		
		try {
			Object pipelineData = handler.openGroup(handle,null,"pipeline");		
			adapter.getPipeLine().iterateOverStatistics(handler, pipelineData, HasStatistics.STATISTICS_ACTION_FULL);
		} catch (SenderException e) {
			log.error(e);
		} finally {
			handler.end(handle);
		}
	
		if (log.isDebugEnabled()) {
			log.debug("about to set adapterStatistics ["+adapterXML.toXML()+"]");
			
//			XmlBuilder alt = new XmlBuilder("alt");
//			StatisticsKeeperToXml hh = new StatisticsKeeperToXml(alt);
//			adapter.forEachStatisticsKeeper(hh,HasStatistics.STATISTICS_ACTION_NONE);
//			log.debug("alternative ["+alt.toXML()+"]");
			
		}
	    request.setAttribute("adapterStatistics", adapterXML.toXML());
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
	
	private class StatisticsKeeperToXml implements StatisticsKeeperIterationHandler {

		private XmlBuilder parent;
		boolean deep;

		public StatisticsKeeperToXml(XmlBuilder parent, boolean deep) {
			super();
			this.parent=parent; 
			this.deep=deep;
		}

		public void configure() {
		}


		public Object start(Date now, Date mainMark, Date detailMark) {
			return parent;
		}
		public void end(Object data) {
		}

		public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) {
			XmlBuilder parent=(XmlBuilder)data;
			XmlBuilder item=statisticsKeeperToXmlBuilder(sk,"stat", deep);
			parent.addSubElement(item);
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

		public void handleScalar(Object data, String scalarName, String value){
			XmlBuilder item=(XmlBuilder)data;
			item.addAttribute(scalarName,value);
		}

		public Object openGroup(Object parentData, String name, String type) {
			XmlBuilder parent=(XmlBuilder)parentData;
			XmlBuilder group=new XmlBuilder(type);
			//group.addAttribute("name",name);
			parent.addSubElement(group);
			return group;
		}

		public void closeGroup(Object data) {
		}
	}
	
	protected XmlBuilder statisticsKeeperToXmlBuilder(StatisticsKeeper sk, String elementName, boolean deep) {
		if (sk==null) {
			return null;
		}
		return sk.toXml(elementName, deep, df, pf);
	}
}
