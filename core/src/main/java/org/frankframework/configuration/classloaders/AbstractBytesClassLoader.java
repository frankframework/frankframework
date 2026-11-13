/*
   Copyright 2016-2017, 2019-2020 Nationale-Nederlanden, 2021-2026 WeAreFrank!

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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;

public abstract class AbstractBytesClassLoader extends AbstractClassLoader {

	private Map<String, byte[]> resources = new HashMap<>();

	protected AbstractBytesClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public final void configure(@Nullable IbisContext ibisContext, @NonNull String configurationName) throws ClassLoaderException {
		super.configure(ibisContext, configurationName);
		resources = loadResources();
	}

	@Override
	public final URL getLocalResource(@NonNull String name) {
		byte[] resource = resources.get(name);
		if (resource != null) {
			try {
				URLStreamHandler urlStreamHandler = new BytesURLStreamHandler(resource);
				URI uri = URI.create(CLASSPATH_RESOURCE_SCHEME + name);
				return URL.of(uri, urlStreamHandler);
			} catch (MalformedURLException e) {
				log.error("Could not create url", e);
			}
		}

		return null;
	}

	@Override
	public final void destroy() {
		clearResources();
		super.destroy();
	}

	/**
	 * Called during a reload for a green/blue deployment, and after the classloader has been configured to load new resources
	 */
	protected abstract Map<String, byte[]> loadResources() throws ClassLoaderException;

	/**
	 * Clears all resources
	 */
	public final void clearResources() {
		resources.clear();
		log.debug("cleaned up classloader resources for configuration [{}]", getConfigurationName());
	}
}
