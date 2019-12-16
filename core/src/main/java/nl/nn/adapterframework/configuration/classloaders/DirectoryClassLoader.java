/*
   Copyright 2016, 2018-2019 Nationale-Nederlanden

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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;

public class DirectoryClassLoader extends ClassLoaderBase {
	private File directory = null;

	public DirectoryClassLoader(ClassLoader parent) throws ConfigurationException {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ConfigurationException {
		super.configure(ibisContext, configurationName);

		if (directory == null) {
			AppConstants appConstants = AppConstants.getInstance();
			String configurationsDirectory = appConstants.getResolvedProperty("configurations.directory");
			if (configurationsDirectory == null) {
				throw new ConfigurationException("Could not find property configurations.directory");
			}

			setDirectory(configurationsDirectory);
		}

		if (!this.directory.isDirectory()) {
			throw new ConfigurationException("Could not find directory to load configuration from: " + this.directory);
		}
	}

	/**
	 * Set the directory from which the configuration files should be loaded
	 * @throws ConfigurationException if the directory can't be found
	 */
	public void setDirectory(String directory) throws ConfigurationException {
		File dir = new File(directory);
		if(!dir.isDirectory())
			throw new ConfigurationException("directory ["+directory+"] not found");

		this.directory = dir;
	}

	protected File getDirectory() {
		return this.directory;
	}

	@Override
	public URL getLocalResource(String name) {
		File file = new File(directory, name);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				log.error("Could not create url for '" + name + "'", e);
			}
		}

		return null;
	}
}