/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.core;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.jmx.JmxAttribute;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.receivers.Receiver;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageKeeper;

/**
 * The Adapter is the central manager in the framework. It has knowledge of both
 * <code>IReceiver</code>s as well as the <code>PipeLine</code> and statistics.
 * The Adapter is the class that is responsible for configuring, initializing and
 * accessing/activating IReceivers, Pipelines, statistics etc.
 *
 **/
public interface IAdapter extends IManagable, HasStatistics {

	/**
	 * Instruct the adapter to configure itself. The adapter will call the pipeline
	 * to configure itself, the pipeline will call the individual pipes to configure
	 * themselves.
	 *
	 * @see AbstractPipe#configure()
	 * @see PipeLine#configure()
	 */
	@Override
	void configure() throws ConfigurationException;

	/**
	 * The messagekeeper is used to keep the last x messages, relevant to display in
	 * the web-functions.
	 */
	MessageKeeper getMessageKeeper();
	Receiver<?> getReceiverByName(String receiverName);
	Iterable<Receiver<?>> getReceivers();
	PipeLineResult processMessage(String messageId, Message message, PipeLineSession pipeLineSession);
	PipeLineResult processMessageWithExceptions(String messageId, Message message, PipeLineSession pipeLineSession) throws ListenerException;

	void setPipeLine(PipeLine pipeline) throws ConfigurationException;
	PipeLine getPipeLine();
	void setConfiguration(Configuration configuration);
	Configuration getConfiguration();
	boolean isAutoStart();

	Message formatErrorMessage(String errorMessage, Throwable t, Message originalMessage, String messageID, INamedObject objectInError, long receivedTime);

	@JmxAttribute(description = "Return the Adapter description")
	String getDescription();
}
