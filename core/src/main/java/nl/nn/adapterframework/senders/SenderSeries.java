/*
   Copyright 2013, 2018 Nationale-Nederlanden

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
 */
public class SenderSeries extends SenderWrapperBase {

	private List<ISender> senderList = new LinkedList<ISender>();
	private Map<ISender, StatisticsKeeper> statisticsMap = new HashMap<ISender, StatisticsKeeper>();
	private boolean synchronous=true;

	@Override
	protected boolean isSenderConfigured() {
		return senderList.size()!=0;
	}

	@Override
	public void configure() throws ConfigurationException {
		for (Iterator<ISender> it = getSenderIterator();it.hasNext();) {
			ISender sender = it.next();
			if (sender instanceof ConfigurationAware) {
				((ConfigurationAware)sender).setConfiguration(getConfiguration());
			}
			sender.configure();
		}
		super.configure();
	}


	@Override
	public void open() throws SenderException {
		for (Iterator<ISender> it = getSenderIterator();it.hasNext();) {
			ISender sender = it.next();
			sender.open();
		}
		super.open();
	}
	@Override
	public void close() throws SenderException {
		for (Iterator<ISender> it = getSenderIterator();it.hasNext();) {
			ISender sender = it.next();
			sender.close();
		}
		super.close();
	}

	@Override
	public String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		long t1 = System.currentTimeMillis();
		for (Iterator<ISender> it = getSenderIterator();it.hasNext();) {
			ISender sender = it.next();
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

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		//Object senderData=hski.openGroup(data,getName(),"sender");
		for (Iterator<ISender> it = getSenderIterator();it.hasNext();) {
			ISender sender = it.next();
			hski.handleStatisticsKeeper(data,getStatisticsKeeper(sender));		
			if (sender instanceof HasStatistics) {
				((HasStatistics)sender).iterateOverStatistics(hski,data,action);
			}
		}
		//hski.closeGroup(senderData);
	}

	@Override
	public String getLogPrefix() {
		return ClassUtils.nameOf(this)+" ["+getName()+"] ";
	}

	@Override
	public boolean isSynchronous() {
		return synchronous;
	}
	public void setSynchronous(boolean value) {
		synchronous = value;
	}

	@Override
	public void setSender(ISender sender) {
		senderList.add(sender);
		setSynchronous(sender.isSynchronous()); // set synchronous to isSynchronous of the last Sender added
		statisticsMap.put(sender, new StatisticsKeeper("-> "+ClassUtils.nameOf(sender)));
	}
	protected Iterator<ISender> getSenderIterator() {
		return senderList.iterator();
	}
	protected StatisticsKeeper getStatisticsKeeper(ISender sender) {
		return statisticsMap.get(sender);
	}

}
