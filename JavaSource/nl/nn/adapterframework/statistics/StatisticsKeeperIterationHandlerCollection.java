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
/*
 * $Log: StatisticsKeeperIterationHandlerCollection.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @version $Id$
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
