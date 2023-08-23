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
package nl.nn.adapterframework.statistics;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;


/**
 * Collection of StatisticsKeeperIterationHandler, that each will be handled.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.8
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

	@Override
	public void configure() throws ConfigurationException {
		if (trace && log.isDebugEnabled()) log.debug("configure()");
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.configure();
		}
	}

	@Override
	public Object start(Date now, Date mainMark, Date detailMark) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("start()");
		List sessionData=new LinkedList();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			sessionData.add(handler.start(now, mainMark, detailMark));
		}
		return sessionData;
	}

	@Override
	public void end(Object data) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("end()");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.end(sessionDataIterator.next());
		}
	}


	@Override
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

	@Override
	public void closeGroup(Object data) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("closeGroup()");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.closeGroup(sessionDataIterator.next());
		}
	}


	@Override
	public void handleScalar(Object data, String scalarName, Date value) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("handleScalar() scalarName ["+scalarName+"] value ["+(value==null?"null":DateUtils.format(value))+"]");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.handleScalar(sessionDataIterator.next(), scalarName, value);
		}
	}

	@Override
	public void handleScalar(Object data, String scalarName, long value) throws SenderException {
		if (trace && log.isDebugEnabled()) log.debug("handleScalar() scalarName ["+scalarName+"] value ["+value+"]");
		List sessionData=(List)data;
		Iterator sessionDataIterator=sessionData.iterator();
		for (Iterator it=iterationHandlerList.iterator();it.hasNext();) {
			StatisticsKeeperIterationHandler handler=(StatisticsKeeperIterationHandler)it.next();
			handler.handleScalar(sessionDataIterator.next(), scalarName, value);
		}
	}

	@Override
	public void handleScalar(Object data, String scalarName, ScalarMetricBase meter) throws SenderException {
		handleScalar(data, scalarName, meter.getValue());
	}

	@Override
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
