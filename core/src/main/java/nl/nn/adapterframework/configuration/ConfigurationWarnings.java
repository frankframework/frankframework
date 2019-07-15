/*
   Copyright 2013, 2016, 2017, 2019 Nationale-Nederlanden

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

import org.apache.log4j.Logger;

/**
 * Singleton class that has the configuration warnings for this application.
 * 
 * @author Peter Leeuwenburgh
 */
public final class ConfigurationWarnings extends BaseConfigurationWarnings {
	private static ConfigurationWarnings self = null;
	private Configuration activeConfiguration = null;
	
	public static synchronized ConfigurationWarnings getInstance() {
		if (self == null) {
			self = new ConfigurationWarnings();
		}
		return self;
	}

	public boolean add(Logger log, String msg) {
		return add(log, msg, null, false);
	}
	
	public boolean add(Logger log, String msg, Throwable t) {
		return add(log, msg, t, false);
	}
	
	public boolean add(Logger log, String msg, boolean onlyOnce) {
		return add(log, msg, null, onlyOnce);
	}

	public boolean add(Logger log, String msg, Throwable t, boolean onlyOnce) {
		return add(log, msg, null, onlyOnce, null);
	}

	public boolean add(Logger log, String msg, Throwable t, boolean onlyOnce, Configuration config) {
		if (config!=null) {
			return config.getConfigurationWarnings().add(log, msg, t, onlyOnce);
		} else {
			if (activeConfiguration!=null) {
				return activeConfiguration.getConfigurationWarnings().add(log, msg, t, onlyOnce);
			} else {
				return super.add(log, msg, t, onlyOnce);
			}
		}
	}

	public boolean containsDefaultValueExceptions(String key) {
		if (activeConfiguration!=null) {
			return activeConfiguration.getConfigurationWarnings().containsDefaultValueExceptions(key);
		} else {
			return super.containsDefaultValueExceptions(key);
		}
	}

	public boolean addDefaultValueExceptions(String key) {
		if (activeConfiguration!=null) {
			return activeConfiguration.getConfigurationWarnings().addDefaultValueExceptions(key);
		} else {
			return super.addDefaultValueExceptions(key);
		}
	}
	
	public void setActiveConfiguration (Configuration configuration) {
		activeConfiguration = configuration;
	}
}