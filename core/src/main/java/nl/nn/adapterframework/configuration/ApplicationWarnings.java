/*
Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import javax.annotation.Priority;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;

import nl.nn.adapterframework.lifecycle.IbisInitializer;

@IbisInitializer
@Priority(Integer.MAX_VALUE)
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ApplicationWarnings extends ApplicationWarningsBase {

	private static ApplicationWarnings self = null;

	/**
	 * Add an AppplicationWarning
	 */
	public static void add(Logger log, String message) {
		add(log, message, null);
	}

	/**
	 * Add an AppplicationWarning and log the exception stack
	 */
	public static void add(Logger log, String message, Throwable t) {
		getInstance().doAdd(null, log, message, t);
	}

	private static ApplicationWarnings getInstance() {
		if(self == null) {
			throw new IllegalArgumentException("ApplicationWarnings not initialized");
		}
		return self;
	}

	public static ApplicationWarnings getInstance(ApplicationContext applicationContext) {
		if(applicationContext == null) {
			throw new IllegalArgumentException("ApplicationContext may not be NULL");
		}

		return applicationContext.getBean("applicationWarnings", ApplicationWarnings.class);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		self = getInstance(getApplicationContext());
	}

	@Override
	public void destroy() throws Exception {
		self = null; //Remove static reference
	}
}
