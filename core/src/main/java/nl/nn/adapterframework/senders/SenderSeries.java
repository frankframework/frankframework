/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Series of Senders, that are executed one after another.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class SenderSeries extends SenderWrapperBase {

	private List<ISender> senderList = new LinkedList<>();
	private Map<ISender, StatisticsKeeper> statisticsMap = new HashMap<>();
	private @Getter @Setter boolean synchronous=true;

	@Override
	protected boolean isSenderConfigured() {
		return !senderList.isEmpty();
	}

	@Override
	public void configure() throws ConfigurationException {
		for (ISender sender: getSenders()) {
			sender.configure();
		}
		super.configure();
	}


	@Override
	public void open() throws SenderException {
		for (ISender sender: getSenders()) {
			sender.open();
		}
		super.open();
	}
	@Override
	public void close() throws SenderException {
		for (ISender sender: getSenders()) {
			sender.close();
		}
		super.close();
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String correlationID = session==null ? null : session.getCorrelationId();
		SenderResult result=null;
		long t1 = System.currentTimeMillis();
		for (ISender sender: getSenders()) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"sending correlationID ["+correlationID+"] message ["+message+"] to sender ["+sender.getName()+"]");
			result = sender.sendMessage(message, session);
			if (!result.isSuccess()) {
				return result;
			}
			message = result.getResult();
			long t2 = System.currentTimeMillis();
			StatisticsKeeper sk = getStatisticsKeeper(sender);
			sk.addValue(t2-t1);
			t1=t2;
		}
		return result!=null ? result : new SenderResult(message);
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		//Object senderData=hski.openGroup(data,getName(),"sender");
		for (ISender sender: getSenders()) {
			hski.handleStatisticsKeeper(data,getStatisticsKeeper(sender));
			if (sender instanceof HasStatistics) {
				((HasStatistics)sender).iterateOverStatistics(hski,data,action);
			}
		}
		//hski.closeGroup(senderData);
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		if (super.consumesSessionVariable(sessionKey)) {
			return true;
		}
		for (ISender sender:senderList) {
			if (sender.consumesSessionVariable(sessionKey)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * one or more specifications of senders that will be executed one after another. Each sender will get the result of the preceding one as input.
	 * @ff.mandatory
	 */
	public void registerSender(ISender sender) {
		senderList.add(sender);
		setSynchronous(sender.isSynchronous()); // set synchronous to isSynchronous of the last Sender added
		statisticsMap.put(sender, new StatisticsKeeper("-> "+ClassUtils.nameOf(sender)));
	}

	protected Iterable<ISender> getSenders() {
		return senderList;
	}

	protected StatisticsKeeper getStatisticsKeeper(ISender sender) {
		return statisticsMap.get(sender);
	}

}
