/*
   Copyright 2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

public abstract class AbstractJarBytesClassLoader extends AbstractBytesClassLoader {

	protected AbstractJarBytesClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	protected final Map<String, byte[]> readResources(byte[] jar) throws ClassLoaderException {
		return readResources(new ByteArrayInputStream(jar));
	}

	protected final Map<String, byte[]> readResources(InputStream stream) throws ClassLoaderException {
		try (JarInputStream jarInputStream = new JarInputStream(stream)) {
			Map<String, byte[]> resources = new HashMap<>();
			JarEntry jarEntry;
			int skippedFiles = 0;

			while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
				String fileName = jarEntry.getName();
				if(getBasePath() != null) {
					// If the name ends with a slash, assume it's a folder.
					boolean isFolder = fileName.endsWith("/");

					if(isFolder || fileName.startsWith("META-INF/")) { // Ignore all folders and files in META-INF
						log.debug("ignoring {} [{}]", ( isFolder ? "folder" : "file" ), fileName);
						continue;
					}

					// By default, the basePath is the configuration name.
					// When it's not, it is still the functional demarcation between configurations,
					// even when multiple configurations are in the same archive.
					if(fileName.startsWith(getBasePath())) {
						// Remove BasePath from the filename
						fileName = fileName.substring(getBasePath().length());
					} else {
						// Found a file that's not in the BasePath folder
						if(!fileName.endsWith(".class")) { // Allow classes to be in the root path, but not resources
							skippedFiles++;

							// Don't add the file to the resources lists
							continue;
						}
					}
				}
				resources.put(fileName, StreamUtil.streamToBytes(CloseUtils.dontClose(jarInputStream)));
			}

			if (skippedFiles > 0) {
				log.warn("skipped [{}] files not located in configuration basePath [{}]", skippedFiles, getBasePath());
			}

			return resources;
		} catch (IOException e) {
			throw new ClassLoaderException("Could not read resources from jar input stream for configuration '" + getConfigurationName() + "'", e);
		}
	}
}
