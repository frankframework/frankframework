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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.FileHandler;

/**
 * <p>See {@link FileHandler}</p>
 *
 * @author Jaco de Groot
 */
@Deprecated
@ConfigurationWarning("Please use LocalFileSystemSender instead, or when retrieving files from the classpath use the FixedResultSender")
public class FileSender extends FileHandler implements ISenderWithParameters {
	private String name;
	protected ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (!outputType.equalsIgnoreCase("string") && !outputType.equalsIgnoreCase("base64")) {
			throw new ConfigurationException(getLogPrefix(null) + "sender doesn't support outputType [" + outputType + "], use file pipe instead");
		}
		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		try {
			return new SenderResult(Message.asMessage(handle(message, session, getParameterList())));
		} catch(Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void open() throws SenderException {
	}

	@Override
	public void close() throws SenderException {
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}
}
