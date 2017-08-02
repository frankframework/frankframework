/*
   Copyright 2016 Nationale-Nederlanden

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
import nl.nn.adapterframework.configuration.IbisContext;

public class DatabaseClassLoader extends JarBytesClassLoader {
	private IbisContext ibisContext;
	private String configurationName;
	private Map<String, Object> configuration;

	public DatabaseClassLoader(IbisContext ibisContext, String configurationName) throws ConfigurationException {
		super(DatabaseClassLoader.class.getClassLoader());
		this.ibisContext = ibisContext;
		this.configurationName = configurationName;
		reload();
	}

	@Override
	public void reload() throws ConfigurationException {
		super.reload();
		Map<String, Object> configuration = null;
		configuration = ConfigurationUtils.getConfigFromDatabase(ibisContext, configurationName, null);
		if (configuration == null) {
			throw new ConfigurationException("Could not get config '" + configurationName + "' from database");
		} else {
			byte[] jarBytes = (byte[]) configuration.get("CONFIG");
			configuration.remove("CONFIG");
			this.configuration = configuration;
			readResources(jarBytes, configurationName);
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
}
