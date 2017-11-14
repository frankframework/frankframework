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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

public class DirectoryClassLoader extends ClassLoader {
	private Logger log = LogUtil.getLogger(this);
	private List<File> directories;

	public DirectoryClassLoader(String directory) throws ConfigurationException {
		super(DirectoryClassLoader.class.getClassLoader());
		if (directory == null) {
			AppConstants appConstants = AppConstants.getInstance();
			String configurationsDirectory = appConstants.getResolvedProperty("configurations.directory");
			if (configurationsDirectory == null) {
				throw new ConfigurationException("Could not find property configurations.directory");
			}
			retrieveDirectories(configurationsDirectory);
		} else {
			retrieveDirectories(directory);
		}
	}

	private void retrieveDirectories(String directoriesString) throws ConfigurationException {
		directories = new ArrayList<File>();
		List<String> directoriesStringAsList = Arrays.asList(directoriesString.split(","));
		boolean existingDir = false;
		for (String directoryString : directoriesStringAsList) {
			File directory = new File(directoryString);
			if (directory.isDirectory()) {
				existingDir = true;
			}
			directories.add(directory);
		}
		if (!existingDir) {
			throw new ConfigurationException("Could not find directory to load configuration from: " + directoriesString);
		}
	}
	
	@Override
	public URL getResource(String name) {
		for (File directory: directories) {
			File file = new File(directory, name);
			if (file.exists()) {
				try {
					return file.toURI().toURL();
				} catch (MalformedURLException e) {
					log.error("Could not create url for '" + name + "'", e);
				}
			}
		}
		return super.getResource(name);
	}
}