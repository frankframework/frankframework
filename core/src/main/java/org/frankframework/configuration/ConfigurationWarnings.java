/*
Copyright 2021-2025 WeAreFrank!

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import org.frankframework.core.Adapter;
import org.frankframework.statistics.HasApplicationContext;
import org.frankframework.util.ClassUtils;

/**
 * If the source implements {@link NameAware} it uses that as prefix, else it will use the ClassName.
 * See {@link ClassUtils#nameOf(Object)}.
 */
public class ConfigurationWarnings extends AbstractApplicationWarnings {

	/**
	 * Add a ConfigurationWarning. 
	 */
	public static void add(HasApplicationContext source, Logger log, String message) {
		add(source, log, message, (Throwable) null);
	}

	/**
	 * Add a ConfigurationWarning (optionally with NameAware prefix) and log the exception stack
	 */
	public static void add(HasApplicationContext source, Logger log, String message, Throwable t) {
		ConfigurationWarnings instance = getInstance(source);
		if(instance != null) {
			instance.doAdd(source, log, message, t);
		} else {
			ApplicationWarnings.add(log, message, t);
		}
	}

	/**
	 * Add a (globally-)suppressible ConfigurationWarning (optionally with NameAware prefix).
	 */
	public static void add(HasApplicationContext source, Logger log, String message, SuppressKeys suppressionKey) {
		add(source, log, message, suppressionKey, null);
	}

	/**
	 * Add a suppressible ConfigurationWarning (optionally with NameAware prefix).
	 */
	public static void add(HasApplicationContext source, Logger log, String message, SuppressKeys suppressionKey, Adapter adapter) {
		ConfigurationWarnings instance = getInstance(source); // We could call two statics, this prevents a double getInstance(..) lookup.
		if(instance != null) {
			instance.add((Object) source, log, message, suppressionKey, adapter);
		} else {
			ApplicationWarnings.add(log, message);
		}
	}

	// Helper method to retrieve ConfigurationWarnings from the Configuration Context
	private static ConfigurationWarnings getInstance(HasApplicationContext source) {
		if(source == null) {
			IllegalArgumentException e = new IllegalArgumentException("no source provided");
			LogManager.getLogger(ConfigurationWarnings.class).warn("Unable to log notification in it's proper context", e);
			return null;
		}

		ApplicationContext applicationContext = source.getApplicationContext();
		if(applicationContext == null) {
			IllegalArgumentException e = new IllegalArgumentException("ApplicationContext may not be NULL");
			LogManager.getLogger(ConfigurationWarnings.class).warn("Unable to retrieve ApplicationContext from source [{}]", source, e);
			return null;
		}

		return applicationContext.getBean("configurationWarnings", ConfigurationWarnings.class);
	}

	private boolean doIsSuppressed(SuppressKeys key, Adapter adapter) {
		if(key == null) {
			throw new IllegalArgumentException("SuppressKeys may not be NULL");
		}

		return isSuppressed(key) || adapter!=null && getAppConstants().getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}

	public boolean isSuppressed(SuppressKeys key) {
		if(key == null) {
			throw new IllegalArgumentException("SuppressKeys may not be NULL");
		} else if(key == SuppressKeys.SQL_INJECTION_SUPPRESS_KEY) {
			return "true".equals(System.getProperty(SuppressKeys.SQL_INJECTION_SUPPRESS_KEY.getKey()));
		}

		return key.isAllowGlobalSuppression() && getAppConstants().getBoolean(key.getKey(), false); // warning is suppressed globally, for all adapters
	}

	public static boolean isSuppressed(SuppressKeys key, Adapter adapter) {
		ConfigurationWarnings instance = getInstance(adapter);
		if(instance == null) {
			throw new IllegalArgumentException("ConfigurationWarnings not initialized");
		}

		return instance.doIsSuppressed(key, adapter);
	}

	public void add(Object source, Logger log, String message, SuppressKeys suppressionKey, Adapter adapter) {
		if(!doIsSuppressed(suppressionKey, adapter)) {
			// provide suppression hint as info
			String hint = null;
			if(log.isInfoEnabled()) {
				if(adapter != null) {
					hint = ". This warning can be suppressed by setting the property '"+suppressionKey.getKey()+"."+adapter.getName()+"=true'";
					if(suppressionKey.isAllowGlobalSuppression()) {
						hint += ", or globally by setting the property '"+suppressionKey.getKey()+"=true'";
					}
				} else if(suppressionKey.isAllowGlobalSuppression()) {
					hint = ". This warning can be suppressed globally by setting the property '"+suppressionKey.getKey()+"=true'";
				}
			}

			doAdd(source, log, message, hint);
		}
	}

}
