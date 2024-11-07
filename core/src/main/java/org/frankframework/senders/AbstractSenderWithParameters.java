/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Provides a base class for senders with parameters.
 *
 * @author Gerrit van Brakel
 * @since  4.3
 */
public abstract class AbstractSenderWithParameters extends AbstractSender implements ISenderWithParameters {

	protected ParameterList paramList = null;
	protected boolean parameterNamesMustBeUnique;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.setNamesMustBeUnique(parameterNamesMustBeUnique);
			paramList.configure();
		}
	}

	@Override
	public void addParameter(IParameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	@Override
	@Nullable
	public ParameterList getParameterList() {
		return paramList;
	}

	protected void checkStringAttributeOrParameter(String attributeName, String attributeValue, String parameterName) throws ConfigurationException {
		if (StringUtils.isEmpty(attributeValue) && (getParameterList()==null || !getParameterList().hasParameter(parameterName))) {
			throw new ConfigurationException("either attribute "+attributeName+" or parameter "+parameterName+" must be specified");
		}
	}

	protected String getParameterOverriddenAttributeValue(ParameterValueList pvl, String parameterName, String attributeValue) {
		if (pvl!=null && pvl.contains(parameterName)) {
			return pvl.get(parameterName).asStringValue(attributeValue);
		}
		return attributeValue;
	}

	protected int getParameterOverriddenAttributeValue(ParameterValueList pvl, String parameterName, int attributeValue) {
		if (pvl!=null && pvl.contains(parameterName)) {
			return pvl.get(parameterName).asIntegerValue(attributeValue);
		}
		return attributeValue;
	}

	protected @Nullable ParameterValueList getParameterValueList(Message input, PipeLineSession session) throws SenderException {
		try {
			return getParameterList()!=null ? getParameterList().getValues(input, session) : null;
		} catch (ParameterException e) {
			throw new SenderException("cannot determine parameter values", e);
		}
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return getParameterList()!=null && getParameterList().consumesSessionVariable(sessionKey);
	}

}
