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

import java.lang.reflect.Constructor;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to an implementation of ISapSender.
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 */
public class SapSender implements ISenderWithParameters, HasPhysicalDestination {
	
	private ISapSender delegate;

	public SapSender() throws ConfigurationException {
		int jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		}

		try {
			Class<?> clazz = ClassUtils.loadClass("nl.nn.adapterframework.extensions.sap.jco"+jcoVersion+".SapSender");
			Constructor<?> con = clazz.getConstructor();
			delegate = (ISapSender) con.newInstance();
		}
		catch (Exception e) {
			throw new ConfigurationException("failed to load SapSender version ["+jcoVersion+"]", e);
		}
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
	public void configure() throws ConfigurationException {
		delegate.configure();
	}

	@Override
	public void open() throws SenderException {
		delegate.open();
	}

	@Override
	public void close() throws SenderException {
		delegate.close();
	}

	@Override
	public boolean isSynchronous() {
		return delegate.isSynchronous();
	}

	@Override
	public void addParameter(Parameter p) {
		delegate.addParameter(p);
	}

	@Override
	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return delegate.sendMessage(correlationID, message);
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return delegate.sendMessage(correlationID, message, prc);
	}

	public void setSynchronous(boolean b) {
		delegate.setSynchronous(b);
	}

	public void setFunctionName(String string) {
		delegate.setFunctionName(string);
	}

	public void setFunctionNameParam(String string) {
		delegate.setFunctionNameParam(string);
	}

	public void setLuwHandleSessionKey(String string) {
		delegate.setLuwHandleSessionKey(string);
	}

	public void setSapSystemName(String string) {
		delegate.setSapSystemName(string);
	}

	public void setSapSystemNameParam(String string) {
		delegate.setSapSystemNameParam(string);
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
