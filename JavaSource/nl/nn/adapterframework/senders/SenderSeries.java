/*
 * $Log: SenderSeries.java,v $
 * Revision 1.11  2011-11-30 13:52:00  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.9  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.8  2009/12/29 14:37:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified imports to reflect move of statistics classes to separate package
 *
 * Revision 1.7  2009/06/05 07:29:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added throws clause to iterateOverStatistics()
 *
 * Revision 1.6  2008/09/04 12:16:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collect interval statistics
 *
 * Revision 1.5  2008/08/27 16:22:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added reset option to statisticsdump
 *
 * Revision 1.4  2008/07/17 16:18:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show partial results in debug mode
 *
 * Revision 1.3  2008/06/03 15:51:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed superfluous code
 *
 * Revision 1.2  2008/05/21 10:54:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added documentation
 *
 * Revision 1.1  2008/05/15 15:08:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Series of Senders, that are executed one after another.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.ParallelSenders</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>one or more specifications of senders that will be executed one after another. Each sender will get the result of the preceding one as input</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class SenderSeries extends SenderWrapperBase {

	private List senderList=new LinkedList();
	private Map statisticsMap=new HashMap();
	private boolean synchronous=true;

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

	public String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		long t1 = System.currentTimeMillis();
		for (Iterator it=senderList.iterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"sending correlationID ["+correlationID+"] message ["+message+"] to sender ["+sender.getName()+"]");
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

	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		//Object senderData=hski.openGroup(data,getName(),"sender");
		for (Iterator it=getSenderIterator();it.hasNext();) {
			ISender sender = (ISender)it.next();
			hski.handleStatisticsKeeper(data,getStatisticsKeeper(sender));		
			if (sender instanceof HasStatistics) {
				((HasStatistics)sender).iterateOverStatistics(hski,data,action);
			}
		}
		//hski.closeGroup(senderData);
	}

	public String getLogPrefix() {
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
