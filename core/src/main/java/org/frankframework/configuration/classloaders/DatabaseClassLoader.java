/*
   Copyright 2016 - 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.configuration.classloaders;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.util.ConfigurationUtils;

public class DatabaseClassLoader extends AbstractJarBytesClassLoader {

	private Map<String, Object> configuration;
	private String datasourceName = null;

	public DatabaseClassLoader(ClassLoader parent) {
		super(parent);
	}

	private String getErrorMessage() {
		return "Could not get config '" + getConfigurationName() + "' from database" + (configuration != null ? ", ignoring reload" : "");
	}

	@Override
	protected Map<String, byte[]> loadResources() throws ClassLoaderException {
		Map<String, Object> loadedConfiguration;
		try {
			// Make sure there's a database present
			ApplicationContext ac = getIbisManager().getApplicationContext();
			loadedConfiguration = ConfigurationUtils.getActiveConfigFromDatabase(ac, getConfigurationName(), datasourceName);
		}
		catch (Throwable t) {
			// Make the error a little bit more IBIS-developer intuitive
			throw new ClassLoaderException(getErrorMessage(), t);
		}

		if (loadedConfiguration == null) {
			throw new ClassLoaderException(getErrorMessage());
		} else {
			byte[] jarBytes = (byte[]) loadedConfiguration.get("CONFIG");
			loadedConfiguration.remove("CONFIG");
			this.configuration = loadedConfiguration;
			return readResources(jarBytes);
		}
	}

	public String getFileName() {
		return (String) configuration.get("FILENAME");
	}

	public String getUser() {
		return (String) configuration.get("USER");
	}

	public String getVersion() {
		return (String) configuration.get("VERSION");
	}

	public String getCreationDate() {
		return (String) configuration.get("CREATED");
	}

	/**
	 * @param datasourceName the Datasource to retrieve the configuration jar from
	 */
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		if(datasourceName != null) builder.append(" datasourceName [").append(datasourceName).append("]");
		if(configuration != null && getFileName() != null) builder.append(" fileName [").append(getFileName()).append("]");
		return builder.toString();
	}
}
