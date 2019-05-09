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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapSender jco3.SapSender} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapSender jco2.SapSender}
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 */
public class SapSender implements ISapSender {
	private int jcoVersion = -1;
	private ISapSender sender = null;

	public SapSender() throws ConfigurationException {
		jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		}

		try {
			Class<?> clazz = ClassUtils.loadClass("nl.nn.adapterframework.extensions.sap.jco"+jcoVersion+".SapSender");
			Constructor<?> con = clazz.getConstructor();
			sender = (ISapSender) con.newInstance();
		}
		catch (Exception e) {
			throw new ConfigurationException("failed to load SapSender version ["+jcoVersion+"]", e);
		}
	}

	public String getName() {
		return sender.getName();
	}

	public void setName(String name) {
		sender.setName(name);
	}

	public void configure() throws ConfigurationException {
		sender.configure();
	}

	public void open() throws SenderException {
		sender.open();
	}

	public void close() throws SenderException {
		sender.close();
	}

	public boolean isSynchronous() {
		return sender.isSynchronous();
	}

	public void addParameter(Parameter p) {
		sender.addParameter(p);
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sender.sendMessage(correlationID, message);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return sender.sendMessage(correlationID, message, prc);
	}

	public void setSynchronous(boolean b) {
		sender.setSynchronous(b);
	}

	public String getFunctionName() {
		return sender.getFunctionName();
	}

	public void setFunctionName(String string) {
		sender.setFunctionName(string);
	}

	public void setFunctionNameParam(String string) {
		sender.setFunctionNameParam(string);
	}

	public void setLuwHandleSessionKey(String string) {
		sender.setLuwHandleSessionKey(string);
	}

	public void setSapSystemName(String string) {
		sender.setSapSystemName(string);
	}

	public void setSapSystemNameParam(String string) {
		sender.setSapSystemNameParam(string);
	}

	public String getRequestFieldName() {
		return sender.getRequestFieldName();
	}

	public void setCorrelationIdFieldIndex(int i) {
		sender.setCorrelationIdFieldIndex(i);
	}

	public void setCorrelationIdFieldName(String string) {
		sender.setCorrelationIdFieldName(string);
	}

	public void setReplyFieldIndex(int i) {
		sender.setReplyFieldIndex(i);
	}

	public void setReplyFieldName(String string) {
		sender.setReplyFieldName(string);
	}

	public void setRequestFieldIndex(int i) {
		sender.setRequestFieldIndex(i);
	}

	public void setRequestFieldName(String string) {
		sender.setRequestFieldName(string);
	}

	public String getPhysicalDestinationName() {
		return sender.getPhysicalDestinationName();
	}
}
