/*
   Copyright 2013, 2019 Nationale-Nederlanden

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

import java.lang.reflect.Constructor;
import java.util.Map;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapListener jco3.SapListener} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapListener jco2.SapListener}
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 */
public class SapListener implements ISapListener<Object> {
	private final Logger LOG = LogUtil.getLogger(this);
	private int jcoVersion = 3;

	private ISapListener<Object> listener = null;

	@SuppressWarnings("unchecked")
	public SapListener() throws ConfigurationException {
		jcoVersion = JCoVersion.getInstance().getJCoVersion();
		LOG.info("Creating SapListener for SAP version ["+jcoVersion+"]");
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		}
		try {
			Class<?> clazz = ClassUtils.loadClass("nl.nn.adapterframework.extensions.sap.jco"+jcoVersion+".SapListener");
			Constructor<?> con = clazz.getConstructor();
			listener = (ISapListener<Object>) con.newInstance();
		}
		catch (Exception e) {
			throw new ConfigurationException("failed to load SapListener version ["+jcoVersion+"]", e);
		}
	}

	public void configure() throws ConfigurationException {
		listener.configure();
	}

	public void open() throws ListenerException {
		listener.open();
	}

	public void close() throws ListenerException {
		listener.close();
	}

	@Override
	public String getIdFromRawMessage(Object rawMessage, Map<String, Object> context) throws ListenerException {
		return listener.getIdFromRawMessage(rawMessage, context);
	}

	@Override
	public String getStringFromRawMessage(Object rawMessage, Map<String, Object> context) throws ListenerException {
		return listener.getStringFromRawMessage(rawMessage, context);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map<String, Object> context) throws ListenerException {
		listener.afterMessageProcessed(processResult, rawMessage, context);
	}

	public String getName() {
		return listener.getName();
	}

	public void setName(String name) {
		listener.setName(name);
	}

	public void setHandler(IMessageHandler<Object> handler) {
		listener.setHandler(handler);
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		this.listener.setExceptionListener(listener);
	}

	public void setSapSystemName(String string) {
		listener.setSapSystemName(string);
	}

	public void setProgid(String string) {
		listener.setProgid(string);
	}

	public void setConnectionCount(String connectionCount) {
		listener.setConnectionCount(connectionCount);
	}

	public void setCorrelationIdFieldIndex(int i) {
		listener.setCorrelationIdFieldIndex(i);
	}

	public void setCorrelationIdFieldName(String string) {
		listener.setCorrelationIdFieldName(string);
	}

	public void setReplyFieldIndex(int i) {
		listener.setReplyFieldIndex(i);
	}

	public void setReplyFieldName(String string) {
		listener.setReplyFieldName(string);
	}

	public void setRequestFieldIndex(int i) {
		listener.setRequestFieldIndex(i);
	}

	public void setRequestFieldName(String string) {
		listener.setRequestFieldName(string);
	}

	public String getPhysicalDestinationName() {
		return listener.getPhysicalDestinationName();
	}
}
