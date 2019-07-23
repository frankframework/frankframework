/*
   Copyright 2013,2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap;

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * implementation of ISapListener. Don't use the jco3 or jco2 class in your 
 * Ibis configuration, use this one instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 */
public class SapListener implements IPushingListener<Object>, HasPhysicalDestination {

	private ISapListener delegate;

	public SapListener() throws ConfigurationException {
		int jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		} else if (jcoVersion == 3) {
			delegate = new nl.nn.adapterframework.extensions.sap.jco3.SapListener();
		} else {
			delegate = new nl.nn.adapterframework.extensions.sap.jco2.SapListener();
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		delegate.configure();
	}

	@Override
	public void open() throws ListenerException {
		delegate.open();
	}

	@Override
	public void close() throws ListenerException {
		delegate.close();
	}

	@Override
	public String getIdFromRawMessage(Object rawMessage, Map<String,Object> context) throws ListenerException {
		return delegate.getIdFromRawMessage(rawMessage, context);
	}

	@Override
	public String getStringFromRawMessage(Object rawMessage, Map<String,Object> context) throws ListenerException {
		return delegate.getStringFromRawMessage(rawMessage, context);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map<String,Object> context) throws ListenerException {
		delegate.afterMessageProcessed(processResult, rawMessage, context);
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public void setName(String name) {
		delegate.setName(name);
	}

	@Override
	public void setHandler(IMessageHandler handler) {
		delegate.setHandler(handler);
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		delegate.setExceptionListener(listener);
	}

	public void setSapSystemName(String string) {
		delegate.setSapSystemName(string);
	}

	public void setProgid(String string) {
		delegate.setProgid(string);
	}

	public void setConnectionCount(String connectionCount) {
		delegate.setConnectionCount(connectionCount);
	}

	public void setCorrelationIdFieldIndex(int i) {
		delegate.setCorrelationIdFieldIndex(i);
	}

	public void setCorrelationIdFieldName(String string) {
		delegate.setCorrelationIdFieldName(string);
	}

	public void setReplyFieldIndex(int i) {
		delegate.setReplyFieldIndex(i);
	}

	public void setReplyFieldName(String string) {
		delegate.setReplyFieldName(string);
	}

	public void setRequestFieldIndex(int i) {
		delegate.setRequestFieldIndex(i);
	}

	public void setRequestFieldName(String string) {
		delegate.setRequestFieldName(string);
	}

	@Override
	public String getPhysicalDestinationName() {
		return delegate.getPhysicalDestinationName();
	}
}
