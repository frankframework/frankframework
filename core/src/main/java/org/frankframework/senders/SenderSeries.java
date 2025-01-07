/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.senders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.stream.Message;

/**
 * Series of Senders, that are executed one after another.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class SenderSeries extends AbstractSenderWrapper {

	private final List<ISender> senderList = new ArrayList<>();
	private final Map<ISender, DistributionSummary> statisticsMap = new ConcurrentHashMap<>();
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
	public void start() {
		getSenders().forEach(ISender::start);

		super.start();
	}

	@Override
	public void stop() {
		getSenders().forEach(ISender::stop);

		super.stop();
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String correlationID = session.getCorrelationId();
		SenderResult result=null;
		long t1 = System.currentTimeMillis();
		for (ISender sender: getSenders()) {
			if (log.isDebugEnabled())
				log.debug("sending correlationID [{}] message [{}] to sender [{}]", correlationID, message, sender.getName());
			result = sender.sendMessage(message, session);
			if (!result.isSuccess()) {
				return result;
			}
			message = result.getResult();
			long t2 = System.currentTimeMillis();
			DistributionSummary summary = getStatisticsKeeper(sender);
			summary.record((double) t2-t1);
			t1=t2;
		}
		return result!=null ? result : new SenderResult(message);
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



	@Deprecated // replaced by setSender, to allow for multiple senders in XSD. Method must be present, as it is used by Digester
	public final void setSender(ISender sender) {
		addSender(sender);
	}

	/**
	 * one or more specifications of senders that will be executed one after another. Each sender will get the result of the preceding one as input.
	 * @ff.mandatory
	 */
	public void addSender(ISender sender) {
		senderList.add(sender);
		setSynchronous(sender.isSynchronous()); // set synchronous to isSynchronous of the last Sender added
	}

	protected @Nonnull DistributionSummary getStatisticsKeeper(ISender sender) {
		return statisticsMap.computeIfAbsent(sender, ignored -> configurationMetrics.createSubDistributionSummary(this, sender, FrankMeterType.PIPE_DURATION));
	}

	protected Iterable<ISender> getSenders() {
		return senderList;
	}

}
