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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Priority;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.util.ClassUtils;

import lombok.Setter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.util.AppConstants;

@IbisInitializer
@Priority(Integer.MAX_VALUE)
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ApplicationWarnings implements ApplicationContextAware, InitializingBean, DisposableBean {

	private @Setter ApplicationContext applicationContext;
	private static ApplicationWarnings self = null;
	private AppConstants appConstants;
	private List<String> warnings;

	public static void add(Logger log, String message) {
		add(log, message, null);
	}

	public static void add(Logger log, String message, Throwable t) {
		add(null, log, message, t);
	}

	public static void add(Object source, Logger log, String message) {
		add(source, log, message, null);
	}

	public static void add(Object source, Logger log, String message, Throwable t) {
		getInstance().doAdd(source, log, message, t);
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
		appConstants = AppConstants.getInstance(applicationContext.getClassLoader());
		warnings = new LinkedList<>();

		self = getInstance(applicationContext);
	}

	@Override
	public void destroy() throws Exception {
		self = null; //Remove static reference
	}

	protected AppConstants getAppConstants() {
		return appConstants;
	}

	public List<String> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	/**
	 * Returns the name of the object. In case a Spring proxy is being used, 
	 * the name will be something like XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d
	 * ClassUtils.getUserClass() makes sure the original class will be returned.
	 */
	protected String getObjectName(Object o) {
		String result = ClassUtils.getUserClass(o).getSimpleName();
		if (o instanceof INamedObject) { //This assumes that setName has already been called
			String named = ((INamedObject) o).getName();
			if (StringUtils.isNotEmpty(named)) {
				return result+=" ["+named+"]";
			}
		}
		return result;
	}

	public boolean isSuppressed(SuppressKeys key) {
		if(key == null) {
			throw new IllegalArgumentException("SuppressKeys may not be NULL");
		}

		return key.isAllowGlobalSuppression() && getAppConstants().getBoolean(key.getKey(), false); // warning is suppressed globally, for all adapters
	}

	public int size() {
		return warnings.size();
	}

	public String get(int i) {
		return warnings.get(i);
	}

	public boolean isEmpty() {
		return warnings.isEmpty();
	}

	private String prefixLogMessage(Object source, String message) {
		String msg = "";
		if(source != null) {
			msg = getObjectName(source)+" ";
		}

		return msg += message;
	}

	/**
	 * Add configuration warning with Object Class + Name prefix and log the exception stack
	 */
	protected void doAdd(Object source, Logger log, String message, Throwable t) {
		doAdd(log, prefixLogMessage(source, message), t);
	}

	protected void doAdd(Object source, Logger log, String message, String hint) {
		doAdd(log, prefixLogMessage(source, message), hint, null);
	}

	/**
	 * Add configuration warning and log the exception stack
	 */
	protected void doAdd(Logger log, String message, Throwable t) {
		doAdd(log, message, message, t);
	}

	private void doAdd(Logger log, String message, String postfixLogMessage, Throwable t) {
		String logMessage = StringUtils.isEmpty(postfixLogMessage) ? message : message + postfixLogMessage;
		if (t == null) {
			log.warn(logMessage);
		} else {
			log.warn(logMessage, t);
		}

		if (t!=null || !warnings.contains(message)) {
			warnings.add(message);
		}
	}
}
