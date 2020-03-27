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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;

/**
 * Wrapper for senders, that allows to get input from a session variable, and to store output in a session variable.
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the senderwrapper will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class SenderWrapper extends SenderWrapperBase {
	private ISender sender;
	
	@Override
	protected boolean isSenderConfigured() {
		return getSender()!=null;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getSender() instanceof ConfigurationAware) {
			((ConfigurationAware)getSender()).setConfiguration(getConfiguration());
		}
		getSender().configure();
	}
	@Override
	public void open() throws SenderException {
		getSender().open();
		super.open();
	}
	@Override
	public void close() throws SenderException {
		super.close();
		getSender().close();
	}

	@Override
	public Message doSendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		return sender.sendMessage(message,session);
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		if (getSender() instanceof HasStatistics) {
			((HasStatistics)getSender()).iterateOverStatistics(hski,data,action);
		}
	}

	@Override
	public boolean isSynchronous() {
		return getSender().isSynchronous();
	}

	@Override
	public void setSender(ISender sender) {
		this.sender=sender;
	}
	protected ISender getSender() {
		return sender;
	}
}
