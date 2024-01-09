/*
   Copyright 2016, 2018-2020 Nationale-Nederlanden

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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.frankframework.configuration.ClassLoaderException;

public class JarFileClassLoader extends JarBytesClassLoader {
	private String jarFileName;

	public JarFileClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected Map<String, byte[]> loadResources() throws ClassLoaderException {
		if(jarFileName == null)
			throw new ClassLoaderException("jar file not set");

		try {
			FileInputStream jarFile = new FileInputStream(jarFileName);
			return readResources(jarFile);
		} catch (FileNotFoundException fnfe) {
			throw new ClassLoaderException("jar file not found");
		}
	}

	public void setJar(String jar) {
		this.jarFileName = jar;
	}
}
