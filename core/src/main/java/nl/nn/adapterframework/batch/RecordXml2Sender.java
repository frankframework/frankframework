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
package nl.nn.adapterframework.batch;

import java.util.List;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.ConfigurationAware;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Translate a record into XML, then send it using a sender.
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>Sender that needs to handle the (XML) record</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the recordHandler will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 */
public class RecordXml2Sender extends RecordXmlTransformer implements ConfigurationAware {

	private ISender sender = null;
	private Configuration configuration; 
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" has no sender");
		}
		if (sender instanceof ConfigurationAware) {
			((ConfigurationAware)sender).setConfiguration(getConfiguration());
		}
		sender.configure();
	}
	@Override
	public void open() throws SenderException {
		super.open();
		sender.open();
	}
	@Override
	public void close() throws SenderException {
		super.close();
		sender.close();
	}

	@Override
	public Object handleRecord(IPipeLineSession session, List parsedRecord, ParameterResolutionContext prc) throws Exception {
		String xml = (String)super.handleRecord(session,parsedRecord,prc);
		return getSender().sendMessage(new Message(xml), session).asString(); 
	}
	


	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

}
