/*
 * $Log: ShowAdapterStatistics.java,v $
 * Revision 1.11  2009-06-05 07:54:34  L190409
 * support for adapter level only statistics
 * end-processing of statisticskeeperhandler in a finally clause
 * added hidden 'deep' option, to output full contents of statisticskeeper
 *
 * Revision 1.10  2009/03/19 08:27:36  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Adapter statistics by the hour
 *
 * Revision 1.9  2008/09/22 13:33:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * exchanged name and type in statisticshandler
 * changed reference to Hashtable to Map
 *
 * Revision 1.8  2008/09/04 12:20:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collect interval statistics
 *
 * Revision 1.7  2008/08/27 16:27:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added reset option to statisticsdump
 *
 * Revision 1.6  2008/08/12 15:50:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added messagesRetried
 *
 * Revision 1.5  2008/05/22 07:42:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.StatisticsKeeper;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * <code>Action</code> to retrieve the statistics from a
 * specific adapter. The pipeline statistics are sorted by
 * pipename.
 * @version Id
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.PipeLine
 * @see nl.nn.adapterframework.core.Adapter
 */
public class ShowAdapterStatistics extends ActionBase {

    private DecimalFormat df=new DecimalFormat(DateUtils.FORMAT_MILLISECONDS);
    private DecimalFormat pf=new DecimalFormat("##0.0");
    
   
	private void addNumber(XmlBuilder xml, String name, String value) {
	    XmlBuilder item = new XmlBuilder("item");
	
	    item.addAttribute("name", name);
	    item.addAttribute("value", value);
	    xml.addSubElement(item);
	}
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
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
		Object handle = handler.start();
		
		try {
			Object pipelineData = handler.openGroup(handle,null,"pipeline");
//			XmlBuilder pipelineXML = new XmlBuilder("pipeline");
	
			Map pipelineStatistics = adapter.getPipeLineStatistics();
	
			Object pipeStatsData = handler.openGroup(pipelineData,null,"pipeStats");
			for(Iterator it=adapter.getPipeLine().getPipes().iterator();it.hasNext();) {
				IPipe pipe = (IPipe)it.next();
				StatisticsKeeper pstat = (StatisticsKeeper) pipelineStatistics.get(pipe.getName());
				handler.handleStatisticsKeeper(pipeStatsData,pstat);
				if (pipe instanceof HasStatistics) {
					try {
						((HasStatistics)pipe).iterateOverStatistics(handler,pipeStatsData,HasStatistics.STATISTICS_ACTION_FULL);
					} catch (SenderException e) {
						error("Could not iterator over statistics of pipe ["+pipe.getName()+"]",e);
					}
				}
			}
			pipelineStatistics = adapter.getWaitingStatistics();
			if (pipelineStatistics.size()>0) {
				Object waitStatsData = handler.openGroup(pipelineData,null,"waitStats");
				for(Iterator it=adapter.getPipeLine().getPipes().iterator();it.hasNext();) {
					IPipe pipe = (IPipe)it.next();
					StatisticsKeeper pstat = (StatisticsKeeper) pipelineStatistics.get(pipe.getName());
					if (pstat!=null) {
						handler.handleStatisticsKeeper(waitStatsData,pstat);
					}
				}
			}
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

		public Object start() {
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
		if (deep) {
			 return sk.dumpToXml();
		}
		String name = sk.getName();
		XmlBuilder container = new XmlBuilder(elementName);
		if (name!=null) {
			container.addAttribute("name", name);
		}
		XmlBuilder stats = new XmlBuilder("summary");
	
	    for (int i=0; i<sk.getItemCount(); i++) {
		    Object item = sk.getItemValue(i);
		    if (item==null) {
		    	addNumber(stats, sk.getItemName(i), "-");
		    } else {
		    	switch (sk.getItemType(i)) {
			    	case StatisticsKeeper.ITEM_TYPE_INTEGER: 
				    	addNumber(stats, sk.getItemName(i), ""+ (Long)item);
		  			  	break;
			    	case StatisticsKeeper.ITEM_TYPE_TIME: 
				    	addNumber(stats, sk.getItemName(i), df.format(item));
		  			  	break;
			    	case StatisticsKeeper.ITEM_TYPE_FRACTION:
				    	addNumber(stats, sk.getItemName(i), ""+pf.format(((Double)item).doubleValue()*100)+ "%");
		  			  	break;
		    	}
	    	}
	    }
	    container.addSubElement(stats);
	    return container;
	}
}
