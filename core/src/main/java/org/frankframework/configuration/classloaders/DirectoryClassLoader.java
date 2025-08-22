/*
   Copyright 2016-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.util.ConfigurationUtils;

public class DirectoryClassLoader extends AbstractClassLoader {
	private File directory = null;

	public DirectoryClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ClassLoaderException {
		super.configure(ibisContext, configurationName);

		if (directory == null) {
			try {
				Path configDir = ConfigurationUtils.getConfigurationDirectory();
				setDirectory(configDir.toString());
			} catch (IOException e) {
				throw new ClassLoaderException(e);
			}
		}

		if(getBasePath() != null) { // Append BasePath, because legacy
			log.debug("appending basepath [{}] to directory [{}]", getBasePath(), directory);
			directory = new File(directory, getBasePath()); // Append BasePath, because legacy
		}

		if (!this.directory.isDirectory()) {
			throw new ClassLoaderException("Could not find directory to load configuration from: " + this.directory);
		}
	}

	/**
	 * Never allow custom code when using this classloader.
	 * Classes should be placed on the classpath (eg. src/main/resources).
	 */
	@Override
	protected boolean getAllowCustomClasses() {
		return false;
	}

	/**
	 * Set the directory from which the configuration files should be loaded
	 * @throws ClassLoaderException if the directory can't be found
	 */
	public void setDirectory(String directory) throws ClassLoaderException {
		File dir = new File(directory);
		if(!dir.isDirectory())
			throw new ClassLoaderException("directory ["+directory+"] not found");

		this.directory = dir;
	}

	public File getDirectory() {
		return this.directory;
	}

	@Override
	public URL getLocalResource(String name) {
		File file = new File(directory, name);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				log.error("could not create url for [{}]", name, e);
			}
		}

		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		if(getDirectory() != null) builder.append(" directory [").append(getDirectory()).append("]");
		return builder.toString();
	}
}
