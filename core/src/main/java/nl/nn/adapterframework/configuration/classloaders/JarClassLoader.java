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

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import nl.nn.adapterframework.util.Misc;

public class JarClassLoader extends BytesClassLoader {

	public JarClassLoader(String jar, String configurationName) {
		super(JarClassLoader.class.getClassLoader());
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jar);
			Enumeration<JarEntry> enumeration = jarFile.entries();
			while (enumeration.hasMoreElements()) {
				JarEntry jarEntry = enumeration.nextElement();
				resources.put(jarEntry.getName(), Misc.streamToBytes(jarFile.getInputStream(jarEntry)));
			}
		} catch (IOException e) {
			log.error("Could not read resources from jar '" + jar
					+ "' for configuration '" + configurationName + "'");
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e) {
					log.warn("Could not close jar '" + jar
							+ "' for configuration '" + configurationName + "'", e);
				}
			}
		}
	}

}
