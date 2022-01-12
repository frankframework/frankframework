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
package nl.nn.adapterframework.configuration.classloaders;

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;

public class DatabaseClassLoader extends JarBytesClassLoader {

	private Map<String, Object> configuration;
	private String dataSourceName = null;

	public DatabaseClassLoader(ClassLoader parent) {
		super(parent);
	}

	private String getErrorMessage() {
		return "Could not get config '" + getConfigurationName() + "' from database" + (configuration != null ? ", ignoring reload" : "");
	}

	@Override
	protected Map<String, byte[]> loadResources() throws ConfigurationException {
		Map<String, Object> configuration = null;
		try { //Make sure there's a database present
			configuration = ConfigurationUtils.getConfigFromDatabase(getIbisContext(), getConfigurationName(), dataSourceName);
		}
		catch (Throwable t) {
			//Make the error a little bit more IBIS-developer intuitive
			throw new ConfigurationException(getErrorMessage(), t);
		}

		if (configuration == null) {
			throw new ConfigurationException(getErrorMessage());
		} else {
			byte[] jarBytes = (byte[]) configuration.get("CONFIG");
			configuration.remove("CONFIG");
			this.configuration = configuration;
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

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}
}
