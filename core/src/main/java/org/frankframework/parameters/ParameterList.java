/*
   Copyright 2013 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;


/**
 * List of parameters.
 *
 * @author Gerrit van Brakel
 */
public class ParameterList extends ArrayList<IParameter> {
	private AtomicInteger index = new AtomicInteger();
	private boolean inputValueRequiredForResolution;
	private @Getter @Setter boolean namesMustBeUnique;

	@Override
	public void clear() {
		index = new AtomicInteger();
		super.clear();
	}

	public void configure() throws ConfigurationException {
		for(IParameter param : this) {
			param.configure();
		}
		index = null; //Once configured there is no need to keep this in memory
		inputValueRequiredForResolution = parameterEvaluationRequiresInputValue();
		if (isNamesMustBeUnique()) {
			Set<String> names = new LinkedHashSet<>();
			Set<String> duplicateNames = new LinkedHashSet<>();
			for(IParameter param : this) {
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
	public boolean add(IParameter param) {
		int i = index.getAndIncrement();
		if (StringUtils.isEmpty(param.getName())) {
			param.setName("parameter" + i);
		}

		return super.add(param);
	}

	public IParameter getParameter(int i) {
		return get(i);
	}

	public IParameter findParameter(String name) {
		for (IParameter p : this) {
			if (p != null && p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	public boolean hasParameter(String name) {
		return stream().anyMatch(p -> p.getName().equals(name));
	}

	private boolean parameterEvaluationRequiresInputValue() {
		for (IParameter p:this) {
			if (p.requiresInputValueForResolution()) {
				return true;
			}
		}
		return false;
	}

	public @Nonnull ParameterValueList getValues(Message message, PipeLineSession session) throws ParameterException {
		return getValues(message, session, true);
	}

	/**
	 * Returns a List of <link>ParameterValue<link> objects
	 */
	public @Nonnull ParameterValueList getValues(Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		if(inputValueRequiredForResolution && !Message.isNull(message)) {
			try {
				message.preserve();
			} catch (IOException e) {
				throw new ParameterException("<input message>", "Cannot preserve message for parameter resolution", e);
			}
		}
		if (inputValueRequiredForResolution && message != null) {
			message.assertNotClosed();
		}

		ParameterValueList result = new ParameterValueList();
		for (IParameter param : this) {
			// if a parameter has sessionKey="*", then a list is generated with a synthetic parameter referring to
			// each session variable whose name starts with the name of the original parameter
			if (isWildcardSessionKey(param)) {
				addMatchingSessionKeys(result, param, message, session, namespaceAware);
			} else {
				result.add(getValue(result, param, message, session, namespaceAware));
			}
		}
		return result;
	}

	private boolean isWildcardSessionKey(IParameter parm) {
		return "*".equals(parm.getSessionKey());
	}

	private void addMatchingSessionKeys(ParameterValueList result, IParameter parm, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		String parmName = parm.getName();
		for (String sessionKey: session.keySet()) {
			if (PipeLineSession.TS_RECEIVED_KEY.equals(sessionKey) || PipeLineSession.TS_SENT_KEY.equals(sessionKey) || !sessionKey.startsWith(parmName) && !"*".equals(parmName)) {
				continue;
			}
			IParameter newParm = new Parameter();
			newParm.setName(sessionKey);
			newParm.setSessionKey(sessionKey); // TODO: Should also set the parameter.type, based on the type of the session key.
			try {
				newParm.configure();
			} catch (ConfigurationException e) {
				throw new ParameterException(sessionKey, e);
			}
			result.add(getValue(result, newParm, message, session, namespaceAware));
		}
	}

	public ParameterValue getValue(ParameterValueList alreadyResolvedParameters, IParameter p, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		return new ParameterValue(p, p.getValue(alreadyResolvedParameters, message, session, namespaceAware));
	}

	public boolean consumesSessionVariable(String sessionKey) {
		for (IParameter p:this) {
			if (p.consumesSessionVariable(sessionKey)) {
				return true;
			}
		}
		return false;
	}

}
