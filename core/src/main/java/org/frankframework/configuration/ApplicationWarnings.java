/*
Copyright 2021, 2022 WeAreFrank!

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
package org.frankframework.configuration;

import java.util.List;

import jakarta.annotation.Priority;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.LogUtil;

@IbisInitializer
@Priority(Integer.MAX_VALUE)
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ApplicationWarnings extends AbstractApplicationWarnings {
	private static final Logger LOG = LogUtil.getLogger(ApplicationWarnings.class);

	//Only allow static references to this class to ensure no objects are stored elsewhere
	private static ApplicationWarnings instance = null;

	public ApplicationWarnings() {
		this(true);
	}

	private ApplicationWarnings(boolean springInstantiated) {
		LOG.debug("ApplicationWarnings instantiated {}", springInstantiated ? "through Spring" : "manually");
	}

	/**
	 * Add an ApplicationWarning
	 */
	public static void add(Logger log, String message) {
		add(log, message, null);
	}

	/**
	 * Add an ApplicationWarning and log the exception stack
	 */
	public static void add(Logger log, String message, Throwable t) {
		getInstance().doAdd(null, log, message, t);
	}



	private static synchronized ApplicationWarnings getInstance() {
		if(instance == null) {
			instance = new ApplicationWarnings(false);
		}
		return instance;
	}

	public static void removeInstance() {
		instance = null;
	}

	private static synchronized void overrideInstance(ApplicationWarnings springInstance) {
		if(instance != null) {
			List<String> warnings = instance.getWarnings();
			springInstance.addWarnings(warnings);
			if(!warnings.isEmpty()) {
				LOG.debug("appending [{}] warning(s)", warnings.size());
			}
		}
		instance = springInstance;
	}

	@Override
	public void afterPropertiesSet() {
		if(getApplicationContext() == null) {
			throw new IllegalArgumentException("ApplicationContext may not be NULL");
		}

		super.afterPropertiesSet();

		//Register the bean in the Spring Context
		overrideInstance(this);
	}

	@Override
	public void destroy() {
		removeInstance(); //Remove static reference when Spring shuts down.
	}

	public static int getSize() {
		return getInstance().getWarnings().size();
	}

	public static List<String> getWarningsList() {
		return getInstance().getWarnings();
	}
}
