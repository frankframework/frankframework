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
package org.frankframework.senders;

import jakarta.annotation.Nonnull;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.FileHandler;

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
		if (!"string".equalsIgnoreCase(outputType) && !"base64".equalsIgnoreCase(outputType)) {
			throw new ConfigurationException("sender doesn't support outputType [" + outputType + "], use file pipe instead");
		}
		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
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
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public void addParameter(IParameter p) {
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
