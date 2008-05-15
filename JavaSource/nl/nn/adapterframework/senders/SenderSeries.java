/*
 * $Log: SenderSeries.java,v $
 * Revision 1.1  2008-05-15 15:08:27  europe\L190409
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StatisticsKeeper;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;

import org.apache.log4j.Logger;

/**
 * Series of Senders, that are executed one after another.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class SenderSeries extends SenderWrapperBase {
	protected Logger log = LogUtil.getLogger(this);

	private List senderList=new LinkedList();
	private Map statisticsMap=new HashMap();
	private boolean synchronous;

	protected boolean isSenderConfigured() {
		return senderList.size()!=0;
	}

	public void configure() throws ConfigurationException {
		for (Iterator it=senderList.iterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			sender.configure();
		}
		super.configure();
	}


	public void open() throws SenderException {
		for (Iterator it=senderList.iterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			sender.open();
		}
		super.open();
	}
	public void close() throws SenderException {
		for (Iterator it=senderList.iterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			sender.close();
		}
		super.close();
	}

	protected String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		long t1 = System.currentTimeMillis();
		for (Iterator it=senderList.iterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			if (sender instanceof ISenderWithParameters) {
				message = ((ISenderWithParameters)sender).sendMessage(correlationID,message,prc);
			} else {
				message = sender.sendMessage(correlationID,message);
			}
			long t2 = System.currentTimeMillis();
			StatisticsKeeper sk = getStatisticsKeeper(sender);
			sk.addValue(t2-t1);
			t1=t2;
		}
		return message;
	}

	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data) {
		//Object senderData=hski.openGroup(data,getName(),"sender");
		for (Iterator it=getSenderIterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			hski.handleStatisticsKeeper(data,getStatisticsKeeper(sender));		
			if (sender instanceof HasStatistics) {
				((HasStatistics)sender).iterateOverStatistics(hski,data);
			}
		}
		//hski.closeGroup(senderData);
	}

	protected String getLogPrefix() {
		return ClassUtils.nameOf(this)+" ["+getName()+"] ";
	}

	public boolean isSynchronous() {
		return synchronous;
	}
	public void setSynchronous(boolean value) {
		synchronous=value;
	}

	public void setSender(ISender sender) {
		senderList.add(sender);
		setSynchronous(sender.isSynchronous()); // set synchronous to isSynchronous of the last Sender added
		statisticsMap.put(sender, new StatisticsKeeper("-> "+ClassUtils.nameOf(sender)));
	}
	protected Iterator getSenderIterator() {
		return senderList.iterator();
	}
	protected StatisticsKeeper getStatisticsKeeper(ISender sender) {
		return (StatisticsKeeper)statisticsMap.get(sender);
	}

}
