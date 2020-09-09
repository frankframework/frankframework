/*
   Copyright 2013, 2016-2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;


/**
 * Singleton class that has the configuration warnings for this application.
 * 
 * @author Peter Leeuwenburgh
 */
public final class ConfigurationWarnings extends BaseConfigurationWarnings {
	private static ConfigurationWarnings self = null;
	private Configuration activeConfiguration = null;

	/**
	 * Add configuration independent warning
	 */
	public static void add(Logger log, String message) {
		add(log, message, null);
	}

	/**
	 * Add configuration independent warning and log the exception stack
	 */
	public static void add(Logger log, String message, Throwable t) {
		getInstance().addConfigurationIndependentWarning(log, message, t, (t==null));
	}

	/**
	 * Add configuration warning with INamedObject prefix
	 */
	public static void add(INamedObject object, Logger log, String message) {
		add(object, log, message, null);
	}

	/**
	 * Add configuration warning with INamedObject prefix and log the exception stack
	 */
	public static void add(INamedObject object, Logger log, String message, Throwable t) {
		String msg = (object==null?"":ClassUtils.nameOf(object) +" ["+object.getName()+"]")+" "+message;
		getInstance().doAdd(log, msg, t);
	}

	public static synchronized ConfigurationWarnings getInstance() {
		if (self == null) {
			self = new ConfigurationWarnings();
		}
		return self;
	}

	private void doAdd(Logger log, String msg, Throwable t) {
		if (activeConfiguration!=null) {
			activeConfiguration.getConfigurationWarnings().add(log, msg, t, (t==null));
		} else {
			addConfigurationIndependentWarning(log, msg, t, (t==null));
		}
	}


	@Override
	protected boolean add(Logger log, String msg, Throwable t, boolean onlyOnce) {
		return add(log, msg, null, onlyOnce, null);
	}

	protected boolean add(Logger log, String msg, Throwable t, boolean onlyOnce, Configuration config) {
		if (config!=null) {
			return config.getConfigurationWarnings().add(log, msg, t, onlyOnce);
		} else {
			if (activeConfiguration!=null) {
				return activeConfiguration.getConfigurationWarnings().add(log, msg, t, onlyOnce);
			} else {
				return addConfigurationIndependentWarning(log, msg, t, onlyOnce);
			}
		}
	}

	private boolean addConfigurationIndependentWarning(Logger log, String msg, Throwable t, boolean onlyOnce) {
		return super.add(log, msg, t, onlyOnce);
	}

	@Override
	public boolean containsDefaultValueException(String key) {
		if (activeConfiguration!=null) {
			return activeConfiguration.getConfigurationWarnings().containsDefaultValueException(key);
		} else {
			return super.containsDefaultValueException(key);
		}
	}

	public void addDefaultValueExceptions(String key) {
		if (activeConfiguration!=null) {
			activeConfiguration.getConfigurationWarnings().addDefaultValueException(key);
		} else {
			super.addDefaultValueException(key);
		}
	}

	public void setActiveConfiguration (Configuration configuration) {
		activeConfiguration = configuration;
	}
	
	public static boolean isSuppressed(String key, IAdapter adapter, ClassLoader cl) {
		AppConstants appConstants = AppConstants.getInstance(cl);
		return appConstants.getBoolean(key, false) // warning is suppressed globally, for all adapters
				|| adapter!=null && appConstants.getBoolean(key+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}
}