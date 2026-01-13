/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.components;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.AopProxyBeanFactoryPostProcessor;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.FrankElement;
import org.frankframework.core.IForwardTarget;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.NameAware;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.ConfiguringLifecycleProcessor;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;

/**
 * @author Niels Meijer
 */
@Log4j2
public class CompositeComponent extends GenericApplicationContext implements InitializingBean, FrankElement, NameAware, ConfigurableLifecycle, IWithParameters, IForwardTarget {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private final @Getter ParameterList parameterList = new ParameterList();

	private @Getter @Setter String plugin;
	private @Getter @Setter String ref;

	@Override
	protected void initLifecycleProcessor() {
		ConfiguringLifecycleProcessor defaultProcessor = new ConfiguringLifecycleProcessor(this);
		defaultProcessor.setBeanFactory(getBeanFactory());
		getBeanFactory().registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, defaultProcessor);
		super.initLifecycleProcessor();
	}

	/**
	 * Checks for correct configuration of forward.
	 */
	@Override
	public void configure() throws ConfigurationException {
		try {
			parameterList.setNamesMustBeUnique(true);
			parameterList.configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException("while configuring parameters", e);
		}
	}

	/**
	 * The method has been made {@code final} to ensure nobody overrides this.
	 */
	@Override
	public final void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
		setParent(applicationContext);
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return this;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isActive()) {
			throw new LifecycleException("unable to refresh, AdapterContext is already active");
		}

		if (getEnvironment().matchesProfiles("aop")) {
			addBeanFactoryPostProcessor(new AopProxyBeanFactoryPostProcessor());
		}

		refresh();
	}

	/**
	 * Adds a parameter to the list of parameters.
	 */
	@Override
	public void addParameter(IParameter param) {
		log.debug("Pipe [{}] added parameter [{}]", getName(), param);
		parameterList.add(param);
	}

	/**
	 * Name of the CompositeComponent
	 * @ff.mandatory
	 */
	@Override
	public void setName(String name) {
		if(name.contains("/")) {
			throw new IllegalStateException("It is not allowed to have '/' in name ["+name+"]");
		}

		setDisplayName("CompositeComponent [" + name + "]");
		setId(name);
	}

	@Override
	public String getName() {
		return getId();
	}

}
