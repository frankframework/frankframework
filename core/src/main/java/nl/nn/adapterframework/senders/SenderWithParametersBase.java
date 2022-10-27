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
package nl.nn.adapterframework.senders;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Provides a base class for senders with parameters.
 *
 * @author Gerrit van Brakel
 * @since  4.3
 */
public abstract class SenderWithParametersBase extends SenderBase implements ISenderWithParameters {

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
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	protected void checkStringAttributeOrParameter(String attributeName, String attributeValue, String parameterName) throws ConfigurationException {
		if (StringUtils.isEmpty(attributeValue) && (getParameterList()==null || getParameterList().findParameter(parameterName)==null)) {
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

	protected ParameterValueList getParameterValueList(Message input, PipeLineSession session) throws SenderException {
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
