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
import nl.nn.adapterframework.core.IConfigurable;

public class ConfigurationWarnings extends ApplicationWarnings {

	/**
	 * Add configuration warning with INamedObject prefix
	 */
	public static void add(IConfigurable source, Logger log, String message) {
		add(source, log, message, (Throwable) null);
	}

	/**
	 * Add configuration warning with INamedObject prefix
	 */
	public static void add(IConfigurable source, Logger log, String message, Throwable t) {
		getInstance(source.getApplicationContext()).doAdd(source, log, message, t);
	}

	public static void add(IConfigurable source, Logger log, String message, SuppressKeys suppressionKey) {
		add(source, log, message, suppressionKey, null);
	}

	public static void add(IConfigurable source, Logger log, String message, SuppressKeys suppressionKey, IAdapter adapter) {
		ConfigurationWarnings warnings = getInstance(source.getApplicationContext()); //We could call two statics, this prevents a double getInstance(..) lookup.
		if(!warnings.doIsSuppressed(suppressionKey, adapter)) {
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

			warnings.doAdd(source, log, message, hint);
		}
	}

	public static ConfigurationWarnings getInstance(ApplicationContext applicationContext) {
		if(applicationContext == null) {
			throw new IllegalArgumentException("ApplicationContext may not be NULL");
		}

		return applicationContext.getBean("configurationWarnings", ConfigurationWarnings.class);
	}

	private boolean doIsSuppressed(SuppressKeys key, IAdapter adapter) {
		if(key == null) {
			throw new IllegalArgumentException("SuppressKeys may not be NULL");
		}

		return super.isSuppressed(key) || adapter!=null && getAppConstants().getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}

	public static boolean isSuppressed(SuppressKeys key, IAdapter adapter) {
		return getInstance(adapter.getApplicationContext()).doIsSuppressed(key, adapter);
	}
}
