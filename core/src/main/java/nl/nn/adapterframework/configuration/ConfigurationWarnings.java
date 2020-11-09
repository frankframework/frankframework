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
import nl.nn.adapterframework.core.IConfigurable;
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

	public static void addGlobalWarning(Logger log, String msg, SuppressKeys suppressionKey, ClassLoader classLoader) {
		if(suppressionKey == null) {
			addGlobalWarning(log, msg, null);
		} else if(!isSuppressed(suppressionKey, null, classLoader)) {
			addGlobalWarning(log, msg, null);
			// provide suppression hint as info
			String hint = null;
			if(log.isInfoEnabled() && suppressionKey.isAllowGlobalSuppression()) {
				hint = ". This warning can be suppressed by setting the property '"+suppressionKey.getKey()+"=true'";
			}
			addGlobalWarning(log, msg, null, hint);
		}
	}

	private static void addGlobalWarning(Logger log, String msg, Throwable t, String messageSuffixForLog) {
		getInstance().addConfigurationIndependentWarning(log, msg, t, messageSuffixForLog, (t==null));
	}

	/**
	 * Add configuration independent warning
	 */
	public static void addGlobalWarning(Logger log, String message) {
		addGlobalWarning(log, message, null);
	}

	/**
	 * Add configuration independent warning and log the exception stack
	 */
	public static void addGlobalWarning(Logger log, String message, Throwable t) {
		getInstance().addConfigurationIndependentWarning(log, message, t, null, (t==null));
	}

	/**
	 * Add configuration warning with INamedObject prefix
	 */
	public static void add(INamedObject object, Logger log, String message) {
		add(object, log, message, null);
	}

	/**
	 * Adds configuration warning in case warning is not suppressed
	 */
	public static void add(IConfigurable object, Logger log, String message, SuppressKeys suppressionKey, IAdapter adapter) {
		if(!isSuppressed(suppressionKey, adapter, object.getConfigurationClassLoader())) {
			// provide suppression hint as info 
			String hint = null;
			if(log.isInfoEnabled()) {
				if(adapter != null) {
					hint = ". This warning can be suppressed by setting the property '"+suppressionKey.getKey()+"."+adapter.getName()+"=true'";
					if(suppressionKey.isAllowGlobalSuppression()) {
						hint += ", or globally by setting the property '"+suppressionKey.getKey()+"=true'";
					}
				} else if(suppressionKey.isAllowGlobalSuppression()) {
					hint = "This warning can be suppressed globally by setting the property '"+suppressionKey.getKey()+"=true'";
				}
			}
			add(object, log, message, null, hint);
		}
	}

	private static void add(IConfigurable object, Logger log, String message, Throwable t, String messageSuffixForLog) {
		String msg = (object==null?"":ClassUtils.nameOf(object) +" ["+object.getName()+"]")+" "+message;
		getInstance().doAdd(log, msg, t, messageSuffixForLog);
	}

	/**
	 * Add configuration warning with INamedObject prefix and log the exception stack
	 */
	public static void add(INamedObject object, Logger log, String message, Throwable t) {
		String msg = (object==null?"":ClassUtils.nameOf(object) +" ["+object.getName()+"]")+" "+message;
		getInstance().doAdd(log, msg, t, null);
	}

	public static synchronized ConfigurationWarnings getInstance() {
		if (self == null) {
			self = new ConfigurationWarnings();
		}
		return self;
	}

	private void doAdd(Logger log, String msg, Throwable t, String messageSuffixForLog) {
		if (activeConfiguration!=null) {
			activeConfiguration.getConfigurationWarnings().add(log, msg, t, messageSuffixForLog, (t==null));
		} else {
			addConfigurationIndependentWarning(log, msg, t, messageSuffixForLog, (t==null));
		}
	}

	@Override
	protected boolean add(Logger log, String msg, Throwable t, String messageSuffixForLog, boolean onlyOnce) {
		return add(log, msg, null, messageSuffixForLog, onlyOnce, null);
	}

	private boolean add(Logger log, String msg, Throwable t, String messageSuffixForLog, boolean onlyOnce,
			Configuration config) {
		if (config!=null) {
			return config.getConfigurationWarnings().add(log, msg, t, messageSuffixForLog, onlyOnce);
		} else {
			if (activeConfiguration!=null) {
				return activeConfiguration.getConfigurationWarnings().add(log, msg, t, messageSuffixForLog, onlyOnce);
			} else {
				return addConfigurationIndependentWarning(log, msg, t, messageSuffixForLog, onlyOnce);
			}
		}
	}

	private boolean addConfigurationIndependentWarning(Logger log, String msg, Throwable t, String messageSuffixForLog, boolean onlyOnce) {
		return super.add(log, msg, t, messageSuffixForLog, onlyOnce);
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
	
	public static boolean isSuppressed(SuppressKeys key, IAdapter adapter, ClassLoader cl) {
		if(key == null || cl == null) {
			return false;
		}

		AppConstants appConstants = AppConstants.getInstance(cl);
		return key.isAllowGlobalSuppression() && appConstants.getBoolean(key.getKey(), false) // warning is suppressed globally, for all adapters
				|| adapter!=null && appConstants.getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}
}