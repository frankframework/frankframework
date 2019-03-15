/*
   Copyright 2016, 2018 Nationale-Nederlanden

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

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.Misc;

public class JarFileClassLoader extends BytesClassLoader {
	private String jarFileName;

	public JarFileClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ConfigurationException {
		super.configure(ibisContext, configurationName);

		if(jarFileName == null)
			throw new ConfigurationException("jar file not set");

		reload();
	}

	public void setJar(String jar) {
		this.jarFileName = jar;
	}

	@Override
	public void reload() throws ConfigurationException {
		super.reload();
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> enumeration = jarFile.entries();
			while (enumeration.hasMoreElements()) {
				JarEntry jarEntry = enumeration.nextElement();
				resources.put(jarEntry.getName(), Misc.streamToBytes(jarFile.getInputStream(jarEntry)));
			}
		} catch (IOException e) {
			throw new ConfigurationException(
					"Could not read resources from jar '" + jarFileName
					+ "' for configuration '" + getConfigurationName() + "'");
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e) {
					log.warn("Could not close jar '" + jarFileName
							+ "' for configuration '" + getConfigurationName() + "'", e);
				}
			}
		}
	}

}
