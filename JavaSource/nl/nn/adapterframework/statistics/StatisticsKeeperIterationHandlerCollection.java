/*
 * $Log: StatisticsKeeperIterationHandlerCollection.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
 * moved statistics to separate package
 *
 * Revision 1.1  2009/08/26 15:40:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for configurable statisticsHandlers
 *
 */
package nl.nn.adapterframework.statistics;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;


/**
 * Collection of StatisticsKeeperIterationHandler, that each will be handled.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.8
 * @version Id
 */
public class StatisticsKeeperIterationHandlerCollection implements StatisticsKeeperIterationHandler {
	protected Logger log = LogUtil.getLogger(this);
	
	List iterationHandlerList = new LinkedList();
	
	final boolean trace=false;
	
	public StatisticsKeeperIterationHandlerCollection() {
		super();
	}

	public void registerStatisticsHandler(StatisticsKeeperIterationHandler handler) {
		log.debug("registerStatisticsHandler() registering ["+ClassUtils.nameOf(handler)+"]");		
		iterationHandlerList.add(handler);
	}

	public void configure() throws ConfigurationException {	
		if (trace && log.isDebugEnabled()) log.debug("configure()");
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.configure();
		}
	}
	
	public Object start(Date now, Date mainMark, Date detailMark) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("start()");
		List sessionData=new LinkedList();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			sessionData.add(handler.start(now, mainMark, detailMark));
		}
		return sessionData;
	}

	public void end(Object data) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("end()");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.end(sessionDataIterator.next());
		}
	}


	public Object openGroup(Object parentData, String name, String type) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("openGroup() name ["+name+"] type ["+type+"]");
		Iterator parentDataIterator=((List)parentData).iterator();
		List groupData=new LinkedList();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			groupData.add(handler.openGroup(parentDataIterator.next(),name,type));
		}
		return groupData;
	}
	
	public void closeGroup(Object data) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("closeGroup()");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.closeGroup(sessionDataIterator.next());
		}
	}


	public void handleScalar(Object data, String scalarName, Date value) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("handleScalar() scalarName ["+scalarName+"] value ["+(value==null?"null":DateUtils.format(value))+"]");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.handleScalar(sessionDataIterator.next(), scalarName, value);
		}
	}

	public void handleScalar(Object data, String scalarName, long value) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("handleScalar() scalarName ["+scalarName+"] value ["+value+"]");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.handleScalar(sessionDataIterator.next(), scalarName, value);
		}
	}

	public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("handleStatisticsKeeper() StatisticsKeeper ["+sk.getName()+"]");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.handleStatisticsKeeper(sessionDataIterator.next(), sk);
		}
	}

}
