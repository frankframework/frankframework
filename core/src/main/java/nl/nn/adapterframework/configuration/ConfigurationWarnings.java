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

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IConfigurationAware;

public class ConfigurationWarnings extends ApplicationWarningsBase {

	/**
	 * Add a ConfigurationWarning with INamedObject prefix
	 */
	public static void add(IConfigurationAware source, Logger log, String message) {
		add(source, log, message, (Throwable) null);
	}

	/**
	 * Add a ConfigurationWarning with INamedObject prefix and log the exception stack
	 */
	public static void add(IConfigurationAware source, Logger log, String message, Throwable t) {
		if(source == null) {
			ApplicationWarnings.add(log, message, t);
			log.warn("Unable to log notification in it's proper context", new IllegalArgumentException("no source provided"));
		}

		ConfigurationWarnings instance = getInstance(source);
		if(instance != null) {
			instance.doAdd(source, log, message, t);
		} else {
			ApplicationWarnings.add(log, message, t);
		}
	}

	/**
	 * Add a (globally-)suppressable ConfigurationWarning with INamedObject prefix
	 */
	public static void add(IConfigurationAware source, Logger log, String message, SuppressKeys suppressionKey) {
		add(source, log, message, suppressionKey, null);
	}

	/**
	 * Add a suppressable ConfigurationWarning with INamedObject prefix
	 */
	public static void add(IConfigurationAware source, Logger log, String message, SuppressKeys suppressionKey, IAdapter adapter) {
		ConfigurationWarnings instance = getInstance(source); //We could call two statics, this prevents a double getInstance(..) lookup.
		if(instance != null && !instance.doIsSuppressed(suppressionKey, adapter)) {
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

			instance.doAdd(source, log, message, hint);
		}
	}

	//Helper method to retrieve ConfigurationWarnings from the Configuration Context
	private static ConfigurationWarnings getInstance(IConfigurationAware source) {
		ApplicationContext applicationContext = source.getApplicationContext();
		if(applicationContext == null) {
//			IllegalArgumentException e = new IllegalArgumentException("ApplicationContext may not be NULL");
			return null;
		}

		return applicationContext.getBean("configurationWarnings", ConfigurationWarnings.class);
	}

	private boolean doIsSuppressed(SuppressKeys key, IAdapter adapter) {
		if(key == null) {
			throw new IllegalArgumentException("SuppressKeys may not be NULL");
		}

		return isSuppressed(key) || adapter!=null && getAppConstants().getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}

	public boolean isSuppressed(SuppressKeys key) {
		if(key == null) {
			throw new IllegalArgumentException("SuppressKeys may not be NULL");
		}

		return key.isAllowGlobalSuppression() && getAppConstants().getBoolean(key.getKey(), false); // warning is suppressed globally, for all adapters
	}

	public static boolean isSuppressed(SuppressKeys key, IAdapter adapter) {
		ConfigurationWarnings instance = getInstance(adapter);
		if(instance == null) {
			throw new IllegalArgumentException("ConfigurationWarnings not initialized");
		}

		return instance.doIsSuppressed(key, adapter);
	}
}
