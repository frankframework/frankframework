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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Misc;

public abstract class JarBytesClassLoader extends BytesClassLoader {

	public JarBytesClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	protected Map<String, byte[]> readResources(byte[] jar) throws ConfigurationException {
		JarInputStream jarInputStream = null;
		try {
			Map<String, byte[]> resources = new HashMap<String, byte[]>();
			jarInputStream = new JarInputStream(new ByteArrayInputStream(jar));
			JarEntry jarEntry;
			while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
				resources.put(jarEntry.getName(), Misc.streamToBytes(jarInputStream));
			}
			return resources;
		} catch (IOException e) {
			throw new ConfigurationException(
					"Could not read resources from jar input stream for configuration '"
					+ getConfigurationName() + "'", e);
		} finally {
			if (jarInputStream != null) {
				try {
					jarInputStream.close();
				} catch (IOException e) {
					log.warn("Could not close jar input stream for configuration '"
							+ getConfigurationName() + "'", e);
				}
			}
		}
	}

}
