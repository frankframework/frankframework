/*
   Copyright 2016-2017, 2019-2020 Nationale-Nederlanden

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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

public abstract class BytesClassLoader extends ClassLoaderBase {

	protected Logger log = LogUtil.getLogger(this);
	private Map<String, byte[]> resources = new HashMap<String, byte[]>();

	public BytesClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public final void configure(IbisContext ibisContext, String configurationName) throws ConfigurationException {
		super.configure(ibisContext, configurationName);
		resources = loadResources();
	}

	@Override
	public URL getLocalResource(String name) {
		byte[] resource = resources.get(name);
		if (resource != null) {
			try {
				return createURL(name, resource);
			} catch (MalformedURLException e) {
				log.error("Could not create url", e);
			}
		}

		return null;
	}

	/**
	 * Strip BasePath of the local resource and return a valid, relative to the configuration directory, URL
	 */
	private URL createURL(String name, byte[] resource) throws MalformedURLException {
		String URLname = CLASSPATH_RESOURCE_SCHEME + name;
		if(getBasePath() != null && name.startsWith(getBasePath())) {
			URLname = CLASSPATH_RESOURCE_SCHEME + name.substring(getBasePath().length());
		}
		URLStreamHandler urlStreamHandler = new BytesURLStreamHandler(resource);
		return new URL(null, URLname, urlStreamHandler);
	}

	/**
	 * Tries to load new resources, upon success, clears all resources, calls it's super.reload() and sets the new resources
	 */
	@Override
	public final void reload() throws ConfigurationException {
		Map<String, byte[]> newResources = loadResources();
		if (newResources != null) {
			clearResources();
			super.reload();
			resources = newResources;
		}
	}

	/**
	 * Called during a reload for a green/blue deployment, and after the classloader has been configured to load new resources
	 */
	protected abstract Map<String, byte[]> loadResources() throws ConfigurationException;

	/**
	 * Clears all resources
	 */
	public final void clearResources() throws ConfigurationException {
		log.debug("cleaned up classloader resources for configuration ["+getConfigurationName()+"]");
		resources.clear();
	}
}
