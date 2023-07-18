/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;


/**
 * List of parameters.
 *
 * @author Gerrit van Brakel
 */
public class ParameterList extends ArrayList<Parameter> {
	private AtomicInteger index = new AtomicInteger();
	private @Getter boolean inputValueRequiredForResolution;
	private @Getter boolean inputValueOrContextRequiredForResolution;
	private @Getter @Setter boolean namesMustBeUnique;

	@Override
	public void clear() {
		index = new AtomicInteger();
		super.clear();
	}

	public void configure() throws ConfigurationException {
		for(Parameter param : this) {
			param.configure();
		}
		index = null; //Once configured there is no need to keep this in memory
		inputValueRequiredForResolution = parameterEvaluationRequiresInputValue();
		inputValueOrContextRequiredForResolution = parameterEvaluationRequiresInputValueOrContext();
		if (isNamesMustBeUnique()) {
			Set<String> names = new LinkedHashSet<>();
			Set<String> duplicateNames = new LinkedHashSet<>();
			for(Parameter param : this) {
				if (names.contains(param.getName())) {
					duplicateNames.add(param.getName());
				}
				names.add(param.getName());
			}
			if (!duplicateNames.isEmpty()) {
				throw new ConfigurationException("Duplicate parameter names "+duplicateNames);
			}
		}
	}

	@Override
	public boolean add(Parameter param) {
		int i = index.getAndIncrement();
		if (StringUtils.isEmpty(param.getName())) {
			param.setName("parameter" + i);
		}

		return super.add(param);
	}

	public Parameter getParameter(int i) {
		return get(i);
	}

	public Parameter findParameter(String name) {
		for (Parameter p : this) {
			if (p != null && p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	private boolean parameterEvaluationRequiresInputValue() {
		for (Parameter p:this) {
			if (p.requiresInputValueForResolution()) {
				return true;
			}
		}
		return false;
	}

	private boolean parameterEvaluationRequiresInputValueOrContext() {
		for (Parameter p:this) {
			if (p.requiresInputValueOrContextForResolution()) {
				return true;
			}
		}
		return false;
	}

	public ParameterValueList getValues(Message message, PipeLineSession session) throws ParameterException {
		return getValues(message, session, true);
	}
	/**
	 * Returns a List of <link>ParameterValue<link> objects
	 */
	public ParameterValueList getValues(Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		if(inputValueRequiredForResolution && message!=null) {
			try {
				message.preserve();
			} catch (IOException e) {
				throw new ParameterException("Cannot preserve message for parameter resolution", e);
			}
		}
		ParameterValueList result = new ParameterValueList();
		for (Parameter parm : this) {
			String parmSessionKey = parm.getSessionKey();
			// if a parameter has sessionKey="*", then a list is generated with a synthetic parameter referring to
			// each session variable whose name starts with the name of the original parameter
			if ("*".equals(parmSessionKey)) {
				String parmName = parm.getName();
				for (String sessionKey: session.keySet()) {
					if (!PipeLineSession.TS_RECEIVED_KEY.equals(sessionKey) && !PipeLineSession.TS_SENT_KEY.equals(sessionKey)) {
						if ((sessionKey.startsWith(parmName) || "*".equals(parmName))) {
							Parameter newParm = new Parameter();
							newParm.setName(sessionKey);
							newParm.setSessionKey(sessionKey); // TODO: Should also set the parameter.type, based on the type of the session key.
							try {
								newParm.configure();
							} catch (ConfigurationException e) {
								throw new ParameterException(e);
							}
							result.add(getValue(result, newParm, message, session, namespaceAware));
						}
					}
				}
			} else {
				result.add(getValue(result, parm, message, session, namespaceAware));
			}
		}
		return result;
	}

	private ParameterValue getValue(ParameterValueList alreadyResolvedParameters, Parameter p, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		return new ParameterValue(p, p.getValue(alreadyResolvedParameters, message, session, namespaceAware));
	}

	public boolean consumesSessionVariable(String sessionKey) {
		for (Parameter p:this) {
			if (p.consumesSessionVariable(sessionKey)) {
				return true;
			}
		}
		return false;
	}

}
