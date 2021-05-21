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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;

public abstract class ApplicationWarningsBase implements ApplicationContextAware, InitializingBean, DisposableBean {
	private ApplicationContext applicationContext;
	private AppConstants appConstants;
	private List<String> warnings = new LinkedList<>();

	@Override
	public void afterPropertiesSet() {
		appConstants = AppConstants.getInstance(applicationContext.getClassLoader());
	}

	protected void addWarnings(List<String> warnings) {
		this.warnings.addAll(warnings);
	}

	@Override
	public void destroy() throws Exception {
		warnings = null;
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

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	protected final ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	protected final AppConstants getAppConstants() {
		return appConstants;
	}

	public final List<String> getWarnings() {
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

	private String prefixLogMessage(Object source, String message) {
		String msg = "";
		if(source != null) {
			msg = getObjectName(source)+" ";
		}

		return msg += message;
	}

	/**
	 * Add a warning with Object Class + Name prefix
	 */
	public void add(Object source, Logger log, String message) {
		add(source, log, message, null);
	}

	/**
	 * Add a warning with Object Class + Name prefix and log the exception stack
	 */
	public void add(Object source, Logger log, String message, Throwable t) {
		doAdd(source, log, message, t);
	}

	protected void doAdd(Object source, Logger log, String message, Throwable t) {
		doAdd(log, prefixLogMessage(source, message), t);
	}

	protected void doAdd(Object source, Logger log, String message, String hint) {
		doAdd(log, prefixLogMessage(source, message), hint, null);
	}

	protected void doAdd(Logger log, String message, Throwable t) {
		doAdd(log, message, null, t);
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
