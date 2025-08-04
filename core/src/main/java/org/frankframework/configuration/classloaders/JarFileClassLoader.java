/*
   Copyright 2016, 2018-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.util.ConfigurationUtils;

public class JarFileClassLoader extends AbstractJarBytesClassLoader {
	private String jarFileName;

	public JarFileClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected Map<String, byte[]> loadResources() throws ClassLoaderException {
		Path jarLocation = locateJarFile();
		if (!isJarFile(jarLocation)) {
			throw new ClassLoaderException("invalid jar file location [%s]".formatted(jarLocation));
		}

		try {
			return readResources(Files.newInputStream(jarLocation));
		} catch (IOException e) {
			throw new ClassLoaderException("unable to open JarFile", e);
		}
	}

	@Nonnull
	private Path locateJarFile() throws ClassLoaderException {
		// Name has been, set is it an absolute path?
		if(jarFileName != null) {
			return new File(jarFileName).toPath();
		}

		// Attempt to load 'configuration-name'.jar as sub-path of 'configurations.directory'.
		try {
			Path configDir = ConfigurationUtils.getConfigurationDirectory();
			return configDir.resolve("%s.jar".formatted(getConfigurationName()));
		} catch (IOException e) {
			throw new ClassLoaderException(e);
		}
	}

	public static boolean isJarFile(Path path) {
		return Files.isRegularFile(path) && FilenameUtils.isExtension(path.getFileName().toString(), "zip", "jar");
	}

	public void setJar(String jar) {
		this.jarFileName = jar;
	}
}
