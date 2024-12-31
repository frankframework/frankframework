/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.TransactionAttributes;
import org.frankframework.doc.Forward;
import org.frankframework.doc.Mandatory;
import org.frankframework.monitoring.EventPublisher;
import org.frankframework.monitoring.EventThrowing;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.Locker;
import org.frankframework.util.SpringUtils;

/**
 * Base class for {@link IPipe Pipe}.
 * A Pipe represents an action to take in a {@link PipeLine Pipeline}. This class is meant to be extended
 * for defining steps or actions to take to complete a request. <br/>
 * The contract is that a pipe is created (by the digester), {@link #setName(String)} is called and
 * other setters are called, and then {@link IPipe#configure()} is called, optionally
 * throwing a {@link ConfigurationException}. <br/>
 * As much as possible, class instantiating should take place in the
 * {@link IPipe#configure()} method.
 * The object remains alive while the framework is running. When the pipe is to be run,
 * the {@link IPipe#doPipe(Message, PipeLineSession) doPipe} method is activated.
 * <p>
 * For the duration of the processing of a message by the {@link PipeLine pipeline} has a {@link PipeLineSession pipeLineSession}.
 * <br/>
 * By this mechanism, pipes may communicate with one another.<br/>
 * However, use this functionality with caution, as it is not desirable to make pipes dependent
 * on each other. If a pipe expects something in a session, it is recommended that
 * the key under which the information is stored is configurable (has a setter for this keyname).
 * Also, the setting of something in the <code>PipeLineSession</code> should be done using
 * this technique (specifying the key under which to store the value by a parameter).
 * </p>
 * <p>Since 4.1 this class also has parameters, so that descendants of this class automatically are parameter-enabled.
 * However, your documentation should say if and how parameters are used!<p>
 * <p> All pipes support a forward named 'exception' which will be followed in the pipeline in case the PipeRunExceptions are not handled by the pipe itself
 *
 * @author     Johan Verrips / Gerrit van Brakel
 *
 * @see PipeLineSession
 */
@Forward(name = "exception", description = "some error happened while processing the message; represents the 'unhappy or error flow' and is not limited to Java Exceptions.")
public abstract class AbstractPipe extends TransactionAttributes implements IPipe, EventThrowing, ApplicationContextAware, IWithParameters, HasStatistics {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter ApplicationContext applicationContext;

	private @Getter String name;
	private @Getter String getInputFromSessionKey=null;
	private @Getter String getInputFromFixedValue=null;
	private @Getter String storeResultInSessionKey=null;
	private @Getter boolean preserveInput=false;

	private @Getter int maxThreads = 0;
	private @Getter long durationThreshold = -1;

	private @Getter String chompCharSize = null;
	private @Getter String elementToMove = null;
	private @Getter String elementToMoveSessionKey = null;
	private @Getter String elementToMoveChain = null;
	private @Getter boolean removeCompactMsgNamespaces = true;
	private @Getter boolean restoreMovedElements=false;

	private boolean sizeStatistics = AppConstants.getInstance(configurationClassLoader).getBoolean("statistics.size", true);
	private @Getter Locker locker;
	private @Getter String emptyInputReplacement=null;
	private @Getter boolean writeToSecLog = false;
	private @Getter String secLogSessionKeys = null;
	private @Getter String logIntermediaryResults = null;
	private @Getter String hideRegex = null;

	private final List<PipeForward> registeredForwards = new ArrayList<>();
	private final Map<String, PipeForward> configuredForwards = new HashMap<>();
	private final ParameterList parameterList = new ParameterList();
	protected boolean parameterNamesMustBeUnique;
	private @Setter EventPublisher eventPublisher=null;

	private @Getter @Setter PipeLine pipeLine;

	/**
	 * <code>configure()</code> is called after the {@link PipeLine Pipeline} is registered
	 * at the {@link Adapter Adapter}. Purpose of this method is to reduce
	 * creating connections to databases etc. in the {@link #doPipe(Message, PipeLineSession) doPipe()} method.
	 * As much as possible class-instantiating should take place in the
	 * <code>configure()</code> method, to improve performance.
	 */
	// For testing purposes the configure method should not require the PipeLine to be present.
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(StringUtils.isNotEmpty(getName()) && getName().contains("/")) {
			throw new ConfigurationException("It is not allowed to have '/' in pipe name ["+getName()+"]");
		}

		registeredForwards.forEach(this::configureForward);

		ParameterList params = getParameterList();
		if (params!=null) {
			try {
				params.setNamesMustBeUnique(parameterNamesMustBeUnique);
				params.configure();
			} catch (ConfigurationException e) {
				throw new ConfigurationException("while configuring parameters",e);
			}
		}

		if (!StringUtils.isEmpty(getElementToMove()) && !StringUtils.isEmpty(getElementToMoveChain())) {
			throw new ConfigurationException("cannot have both an elementToMove and an elementToMoveChain specified");
		}

		if (getLocker() != null) {
			getLocker().configure();
		}
	}

	private void configureForward(PipeForward forward) {
		String forwardName = forward.getName();

		if (StringUtils.isBlank(forwardName)) {
			ConfigurationWarnings.add(this, log, "pipe contains a forward without a name");
			return;
		}

		final List<String> allowedForwards = getAllowedForwards();
		if (!allowedForwards.contains("*") && !allowedForwards.contains(forwardName)) {
			ConfigurationWarnings.add(this, log, "the forward [" + forwardName + "] does not exist and cannot be used in this pipe");
		}

		PipeForward current = configuredForwards.get(forwardName);
		if (current==null){
			configuredForwards.put(forwardName, forward);
		} else {
			if (StringUtils.isNotBlank(forward.getPath()) && forward.getPath().equals(current.getPath())) {
				ConfigurationWarnings.add(this, log, "the forward [" + forwardName + "] is already registered on this pipe");
			} else {
				log.info("PipeForward [{}] already registered, pointing to [{}]. Ignoring new one, that points to [{}]", forwardName, current.getPath(), forward.getPath());
			}
		}
	}

	/**
	 * Hierarchical list of forwards that may be present on this {@link AbstractPipe pipe}.
	 */
	@Nonnull
	private List<String> getAllowedForwards() {
		Class<?> clazz = getClass();

		Set<Forward> forwards = new HashSet<>();
		while (clazz != null) {
			forwards.addAll(List.of(clazz.getAnnotationsByType(Forward.class)));

			clazz = clazz.getSuperclass();
		}

		return forwards.stream().map(Forward::name).toList();
	}

	/**
	 * final method to ensure nobody overrides this...
	 */
	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected <T> T createBean(Class<T> beanClass) {
		return SpringUtils.createBean(applicationContext, beanClass);
	}

	@Override
	public void start() {}

	@Override
	public void stop() {}

	/**
	 * Add a parameter to the list of parameters
	 */
	@Override
	public void addParameter(IParameter param) {
		log.debug("Pipe [{}] added parameter [{}]", getName(), param);
		parameterList.add(param);
	}

	/**
	 * return the Parameters
	 */
	@Override
	public ParameterList getParameterList() {
		return parameterList;
	}

	@Override
	public void setLocker(Locker locker) {
		this.locker = locker;
	}

	/** Forwards are used to determine the next Pipe to execute in the Pipeline */
	@Override
	public void addForward(PipeForward forward) {
		registeredForwards.add(forward);
	}

	public boolean hasRegisteredForward(@Nullable String forward) {
		if (StringUtils.isEmpty(forward)) {
			return false;
		}

		return registeredForwards.stream().anyMatch(f -> forward.equals(f.getName()));
	}

	/**
	 * Looks up a key in the pipeForward hashtable. <br/>
	 * A typical use would be on return from a Pipe: <br/>
	 * <pre>{@code
	 * return new PipeRunResult(findForward("success"), result);
	 * }</pre>
	 * findForward searches:<ul>
	 * <li>All forwards defined in xml under the pipe element of this pipe</li>
	 * <li>All global forwards defined in xml under the PipeLine element</li>
	 * <li>All pipe names with their (identical) path</li>
	 * </ul>
	 */
	@Nullable
	public PipeForward findForward(@Nullable String forward) {
		if (StringUtils.isEmpty(forward)) {
			return null;
		}
		if (configuredForwards.containsKey(forward)) {
			return configuredForwards.get(forward);
		}
		if (pipeLine == null) {
			return null;
		}
		PipeForward result = pipeLine.getGlobalForwards().get(forward);
		if (result == null) {
			IPipe pipe = pipeLine.getPipe(forward);
			if (pipe!=null) {
				result = new PipeForward(forward, forward);
			}
		}
		if (result == null) {
			PipeLineExit exit = pipeLine.getPipeLineExits().get(forward);
			if (exit != null) {
				result = new PipeForward(forward, forward);
			}
		}
		if (result != null) {
			configuredForwards.put(forward, result);
		}
		return result;
	}

	@Override
	@Nonnull
	public Map<String, PipeForward> getForwards() {
		Map<String, PipeForward> forwards = new HashMap<>(configuredForwards);
		PipeLine pipeline = getPipeLine();
		if (pipeline == null) {
			return forwards;
		}

		// Omit global pipeline-forwards and only return local pipe-forwards
		pipeline.getPipes()
				.forEach(pipe -> forwards.remove(pipe.getName()));
		return forwards;
	}

	@Override
	public String getEventSourceName() {
		return getName().trim();
	}

	@Override
	public void registerEvent(String description) {
		if (eventPublisher != null) {
			eventPublisher.registerEvent(this, description);
		}
	}

	@Override
	public void throwEvent(String event, Message message) {
		if (eventPublisher != null) {
			eventPublisher.fireEvent(this, event);
		}
	}

	@Override
	public Adapter getAdapter() {
		if (getPipeLine() != null) {
			return getPipeLine().getAdapter();
		}
		return null;
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return sessionKey.equals(getInputFromSessionKey) || parameterList!=null && parameterList.consumesSessionVariable(sessionKey);
	}

	/**
	 * The functional name of this pipe. Can be referenced by the <code>path</code> attribute of a {@link PipeForward}.
	 */
	@Override
	@Mandatory
	public void setName(String name) {
		this.name=name;
	}

	@Override
	public void setGetInputFromSessionKey(String string) {
		getInputFromSessionKey = string;
	}

	@Override
	public void setGetInputFromFixedValue(String string) {
		getInputFromFixedValue = string;
	}

	@Override
	public void setEmptyInputReplacement(String string) {
		emptyInputReplacement = string;
	}

	@Override
	public void setPreserveInput(boolean preserveInput) {
		this.preserveInput = preserveInput;
	}

	/**
	 * If set, the pipe result is copied to a session key that has the name defined by this attribute. The
	 * pipe result is still written as the output message as usual.
	 */
	@Override
	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}

	/**
	 * The maximum number of threads that may {@link #doPipe process messages} simultaneously.
	 * A value of 0 indicates an unlimited number of threads.
	 * @ff.default 0
	 */
	public void setMaxThreads(int newMaxThreads) {
		maxThreads = newMaxThreads;
	}

	@Override
	public void setChompCharSize(String string) {
		chompCharSize = string;
	}

	@Override
	public void setElementToMove(String string) {
		elementToMove = string;
	}

	@Override
	public void setElementToMoveSessionKey(String string) {
		elementToMoveSessionKey = string;
	}

	@Override
	public void setElementToMoveChain(String string) {
		elementToMoveChain = string;
	}

	@Override
	public void setDurationThreshold(long maxDuration) {
		this.durationThreshold = maxDuration;
	}

	@Override
	public void setRemoveCompactMsgNamespaces(boolean b) {
		removeCompactMsgNamespaces = b;
	}

	@Override
	public void setRestoreMovedElements(boolean restoreMovedElements) {
		this.restoreMovedElements = restoreMovedElements;
	}


	/**
	 * Collect and aggregate Message size statistics
	 */
	public void setSizeStatistics(boolean sizeStatistics) {
		this.sizeStatistics = sizeStatistics;
	}
	@Override
	public boolean sizeStatisticsEnabled() {
		return sizeStatistics;
	}

	/** when set to <code>true</code> a record is written to the security log when the pipe has finished successfully */
	@Override
	public void setWriteToSecLog(boolean b) {
		writeToSecLog = b;
	}

	/** (only used when <code>writeToSecLog=true</code>) comma separated list of keys of session variables that is appended to the security log record */
	@Override
	public void setSecLogSessionKeys(String string) {
		secLogSessionKeys = string;
	}

	@Override
	public void setLogIntermediaryResults(String string) {
		logIntermediaryResults = string;
	}

	@Override
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
}
