/*
   Copyright 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;

public abstract class JarBytesClassLoader extends BytesClassLoader {

	public JarBytesClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	protected final Map<String, byte[]> readResources(byte[] jar) throws ConfigurationException {
		return readResources(new ByteArrayInputStream(jar));
	}

	protected final Map<String, byte[]> readResources(InputStream stream) throws ConfigurationException {
		try (JarInputStream jarInputStream = new JarInputStream(stream)) {
			Map<String, byte[]> resources = new HashMap<String, byte[]>();
			JarEntry jarEntry;
			while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
				String fileName = jarEntry.getName();
				if(getBasePath() != null) {
					if(fileName.startsWith(getBasePath())) { //Remove BasePath from the filename
						fileName = fileName.substring(getBasePath().length());
					} else {
						if(!fileName.endsWith(".class")) { //Allow classes to be in the root path, but not resources
							log.error("invalid file ["+fileName+"] not in folder ["+getBasePath()+"]");
							continue; //Don't add the file to the resources lists
						}
					}
				}
				resources.put(fileName, Misc.streamToBytes(StreamUtil.dontClose(jarInputStream)));
			}
			return resources;
		} catch (IOException e) {
			throw new ConfigurationException("Could not read resources from jar input stream for configuration '" + getConfigurationName() + "'", e);
		}
	}
}
