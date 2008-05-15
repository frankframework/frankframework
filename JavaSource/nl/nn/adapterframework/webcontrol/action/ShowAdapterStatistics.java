package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.StatisticsKeeper;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.XmlBuilder;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
/**
 * <code>Action</code> to retrieve the statistics from a
 * specific adapter. The pipeline statistics are sorted by
 * pipename.
 * @version Id
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.PipeLine
 * @see nl.nn.adapterframework.core.Adapter
 */

public final class ShowAdapterStatistics extends ActionBase {

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
	    adapterXML.addSubElement(statisticsKeeperToXmlBuilder(st, "messageProcessingDuration"));
	
	    
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
					    while(statsIter.hasNext()) {				    
					        StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
					        procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(pstat, "stat"));
				        }
						receiverXML.addSubElement(procStatsXML);
				    }
	
				    statsIter = statReceiver.getIdleStatisticsIterator();
				    if (statsIter != null) {
				        XmlBuilder procStatsXML = new XmlBuilder("idleStats");
					    while(statsIter.hasNext()) {				    
					        StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
					        procStatsXML.addSubElement(statisticsKeeperToXmlBuilder(pstat, "stat"));
			  	      	}
						receiverXML.addSubElement(procStatsXML);
				    }
	
	
				}
			}
	        adapterXML.addSubElement(receiversXML); 
	    }
	
	    
		StatisticsKeeperToXml handler = new StatisticsKeeperToXml(adapterXML);
		Object handle = handler.start();

		Object pipelineData = handler.openGroup(handle,"pipeline",null);
//	    XmlBuilder pipelineXML = new XmlBuilder("pipeline");
	
	    Hashtable pipelineStatistics = adapter.getPipeLineStatistics();
	
		Object pipeStatsData = handler.openGroup(pipelineData,"pipeStats",null);
		for(Iterator it=adapter.getPipeLine().getPipes().iterator();it.hasNext();) {
			IPipe pipe = (IPipe)it.next();
			StatisticsKeeper pstat = (StatisticsKeeper) pipelineStatistics.get(pipe.getName());
			handler.handleStatisticsKeeper(pipeStatsData,pstat);
			if (pipe instanceof HasStatistics) {
				((HasStatistics)pipe).iterateOverStatistics(handler,pipeStatsData);
			}
		}
	
		pipelineStatistics = adapter.getWaitingStatistics();
		if (pipelineStatistics.size()>0) {
			Object waitStatsData = handler.openGroup(pipelineData,"waitStats",null);
			for(Iterator it=adapter.getPipeLine().getPipes().iterator();it.hasNext();) {
				IPipe pipe = (IPipe)it.next();
				StatisticsKeeper pstat = (StatisticsKeeper) pipelineStatistics.get(pipe.getName());
				if (pstat!=null) {
					handler.handleStatisticsKeeper(waitStatsData,pstat);
				}
			}
		}
	    request.setAttribute("adapterStatistics", adapterXML.toXML());
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
	
	private class StatisticsKeeperToXml implements StatisticsKeeperIterationHandler {

		private XmlBuilder parent;

		public StatisticsKeeperToXml(XmlBuilder parent) {
			super();
			this.parent=parent; 
		}

		public Object start() {
			return parent;
		}
		public void end(Object data) {
		}

		public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) {
			XmlBuilder parent=(XmlBuilder)data;
			XmlBuilder item=statisticsKeeperToXmlBuilder(sk,"stat");
			parent.addSubElement(item);
		}

		public void handleScalar(Object data, String scalarName, long value){
			handleScalar(data,scalarName,""+value);
		}

		public void handleScalar(Object data, String scalarName, String value){
			XmlBuilder item=(XmlBuilder)data;
			item.addAttribute(scalarName,value);
		}

		public Object openGroup(Object parentData, String name, String type) {
			XmlBuilder parent=(XmlBuilder)parentData;
			XmlBuilder group=new XmlBuilder(name);
			group.addAttribute("name",name);
			group.addAttribute("type",type);
			parent.addSubElement(group);
			return group;
		}

		public void closeGroup(Object data) {
		}
	}
	
	private XmlBuilder statisticsKeeperToXmlBuilder(StatisticsKeeper sk, String elementName) {
		if (sk==null) {
			return null;
		}
		String name = sk.getName();
		XmlBuilder container = new XmlBuilder(elementName);
		if (name!=null)
			container.addAttribute("name", name);
			
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
