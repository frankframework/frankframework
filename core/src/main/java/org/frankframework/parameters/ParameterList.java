/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;


/**
 * List of parameters.
 *
 * @author Gerrit van Brakel
 */
public class ParameterList implements Iterable<IParameter> {
	private boolean inputValueRequiredForResolution;
	private @Getter @Setter boolean namesMustBeUnique;
	private @Nullable List<IParameter> parameters;

	public ParameterList() {
		super();
	}

	public ParameterList(@Nullable ParameterList parameterList) {
		if (parameterList == null) {
			return;
		}
		this.inputValueRequiredForResolution = parameterList.inputValueRequiredForResolution;
		this.namesMustBeUnique = parameterList.namesMustBeUnique;
		if (parameterList.parameters != null) {
			this.parameters = new ArrayList<>(parameterList.parameters);
		}
	}

	public void clear() {
		if (parameters != null) {
			parameters.clear();
		}
	}

	public void configure() throws ConfigurationException {
		if (parameters == null) {
			return;
		}
		for(IParameter param : parameters) {
			param.configure();
		}
		inputValueRequiredForResolution = parameterEvaluationRequiresInputValue();
		if (isNamesMustBeUnique()) {
			List<String> duplicateNames = parameters.stream()
					.collect(Collectors.groupingBy(IParameter::getName, Collectors.counting()))
					.entrySet().stream()
					.filter(entry -> entry.getValue() > 1)
					.map(Map.Entry::getKey)
					.toList();
			if (!duplicateNames.isEmpty()) {
				throw new ConfigurationException("Duplicate parameter names "+duplicateNames);
			}
		}
	}

	public synchronized boolean add(IParameter param) {
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		if (StringUtils.isEmpty(param.getName())) {
			param.setName("parameter" + parameters.size());
		}

		return parameters.add(param);
	}

	public boolean remove(IParameter param) {
		if (parameters == null) {
			return false;
		}
		return parameters.remove(param);
	}

	public IParameter remove(String name) {
		final IParameter param = findParameter(name);
		if (param == null) {
			return null;
		}
		if (remove(param)) {
			return param;
		} else {
			return null;
		}
	}

	public IParameter getParameter(int i) {
		if (parameters == null) {
			throw new IndexOutOfBoundsException("Parameter list is empty");
		}
		return parameters.get(i);
	}

	public IParameter findParameter(String name) {
		if (parameters == null) {
			return null;
		}
		return parameters.stream()
				.filter(p -> p.getName().equals(name))
				.findFirst().orElse(null);
	}

	public boolean hasParameter(String name) {
		if (parameters == null) {
			return false;
		}
		return parameters.stream()
				.anyMatch(p -> p.getName().equals(name));
	}

	private boolean parameterEvaluationRequiresInputValue() {
		if (parameters == null) {
			return false;
		}
		for (IParameter p: parameters) {
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
		ParameterValueList result = new ParameterValueList();
		if (parameters == null) {
			return result;
		}
		if (inputValueRequiredForResolution && message != null) {
			message.assertNotClosed();
		}

		for (IParameter param : parameters) {
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

	public ParameterValue getValue(ParameterValueList alreadyResolvedParameters, @Nonnull IParameter p, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		return new ParameterValue(p, p.getValue(alreadyResolvedParameters, message, session, namespaceAware));
	}

	public boolean consumesSessionVariable(String sessionKey) {
		if (parameters == null) {
			return false;
		}
		for (IParameter p: parameters) {
			if (p.consumesSessionVariable(sessionKey)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
		if (parameters == null) {
			return true;
		}
		return parameters.isEmpty();
	}

	@Nonnull
	@Override
	public Iterator<IParameter> iterator() {
		if (parameters == null) {
			return Collections.emptyIterator();
		}
		return parameters.iterator();
	}

	@Override
	public void forEach(Consumer<? super IParameter> action) {
		if (parameters == null) {
			return;
		}
		parameters.forEach(action);
	}

	@Override
	public Spliterator<IParameter> spliterator() {
		return Iterable.super.spliterator();
	}

	public @Nonnull Stream<IParameter> stream() {
		if (parameters == null) {
			return Stream.empty();
		}
		return parameters.stream();
	}

	public int size() {
		if (parameters == null) {
			return 0;
		}
		return parameters.size();
	}

	/**
	 * Get a list of all parameter names in this ParameterList. Names do not need to be unique unless {@link #setNamesMustBeUnique(boolean)} has been set
	 * to {@code true}, they will appear multiple times in the list if they are present multiple times in the ParameterList.
	 */
	public @Nonnull List<String> getParameterNames() {
		if (parameters == null) {
			return List.of();
		}
		return parameters.stream().map(IParameter::getName).toList();
	}
}
