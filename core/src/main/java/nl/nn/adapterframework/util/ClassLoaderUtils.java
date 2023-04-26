/*
   Copyright 2023 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;

public abstract class ClassLoaderUtils {
	private static Logger log = LogUtil.getLogger(ClassLoaderUtils.class);
	private static final String DEFAULT_ALLOWED_PROTOCOLS = AppConstants.getInstance().getString("classloader.allowed.protocols", null);

	/**
	 * Get a resource-URL directly from the ClassPath
	 * @param resource name of the resource you are trying to fetch the URL from
	 * @return URL of the resource or null if it can't be not found
	 */
	public static URL getResourceURL(String resource) {
		return getResourceURL(null, resource);
	}

	/**
	 * Get a resource-URL from a specific IConfigurationClassLoader. This should be used by
	 * classes which are part of the Ibis configuration (like pipes and senders)
	 * because the configuration might be loaded from outside the webapp
	 * ClassPath. Hence the Thread.currentThread().getContextClassLoader() at
	 * the time the class was instantiated should be used.
	 *
	 * @see IbisContext#init()
	 */
	public static URL getResourceURL(IScopeProvider scopeProvider, String resource) {
		return getResourceURL(scopeProvider, resource, null);
	}

	/**
	 * Get a resource-URL from a ClassLoader, therefore the resource should not start with a leading slash
	 * @param scopeProvider to retrieve the file from, or NULL when you want to retrieve the resource directly from the ClassPath (using an absolute path)
	 * @param resource name of the resource you are trying to fetch the URL from
	 * @return URL of the resource or null if it can't be not found
	 */
	public static URL getResourceURL(IScopeProvider scopeProvider, String resource, String allowedProtocols) {
		ClassLoader classLoader = null;
		if(scopeProvider == null) { // Used by ClassPath resources
			classLoader = Thread.currentThread().getContextClassLoader();
		} else {
			classLoader = scopeProvider.getConfigurationClassLoader();
		}

		String resourceToUse = resource; //Don't change the original resource name for logging purposes
		if (resource.startsWith(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME)) {
			resourceToUse = resource.substring(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME.length());
		}

		// Remove slash like Class.getResource(String name) is doing before delegation to ClassLoader.
		// Resources retrieved from ClassLoaders should never start with a leading slash
		if (resourceToUse.startsWith("/")) {
			resourceToUse = resourceToUse.substring(1);
		}
		URL url = classLoader.getResource(resourceToUse);

		// then try to get it as a URL
		if (url == null) {
			if (resourceToUse.contains(":")) {
				String protocol = resourceToUse.substring(0, resourceToUse.indexOf(":"));
				if (allowedProtocols==null) {
					allowedProtocols = DEFAULT_ALLOWED_PROTOCOLS;
				}
				if (StringUtils.isNotEmpty(allowedProtocols)) {
					//log.debug("Could not find resource ["+resource+"] in classloader ["+classLoader+"] now trying via protocol ["+protocol+"]");

					List<String> protocols = Arrays.asList(allowedProtocols.split(","));
					if(protocols.contains(protocol)) {
						try {
							url = new URL(StringUtil.replace(resourceToUse, " ", "%20"));
						} catch(MalformedURLException e) {
							log.debug("Could not find resource ["+resource+"] in classloader ["+nameOf(classLoader)+"] and not as URL [" + resource + "]: "+e.getMessage());
						}
					} else if(log.isDebugEnabled()) log.debug("Cannot lookup resource ["+resource+"] in classloader ["+nameOf(classLoader)+"], not allowed with protocol ["+protocol+"] allowedProtocols "+protocols.toString());
				} else {
					if(log.isDebugEnabled()) log.debug("Could not find resource as URL [" + resource + "] in classloader ["+nameOf(classLoader)+"], with protocol ["+protocol+"], no allowedProtocols");
				}
			} else {
				if(log.isDebugEnabled()) log.debug("Cannot lookup resource ["+resource+"] in classloader ["+nameOf(classLoader)+"] and no protocol to try as URL");
			}
		}

		return url;
	}

	public static List<String> getAllowedProtocols() {
		if(StringUtils.isEmpty(DEFAULT_ALLOWED_PROTOCOLS)) {
			return new ArrayList<>(); //Arrays.asList(..) won't return an empty List when empty.
		}
		return Arrays.asList(DEFAULT_ALLOWED_PROTOCOLS.split(","));
	}

	/**
	 * If the classLoader is derivable of IConfigurationClassLoader return the className + configurationName,
	 * else return the className of the object. Don't return the package name to avoid cluttering the logs.
	 */
	public static String nameOf(ClassLoader classLoader) {
		if(classLoader == null) {
			return "<null>";
		}

		String logPrefix = ClassUtils.nameOf(classLoader) + "@" + Integer.toHexString(classLoader.hashCode());
		if(classLoader instanceof IConfigurationClassLoader) {
			String configurationName = ((IConfigurationClassLoader) classLoader).getConfigurationName();
			if(StringUtils.isNotEmpty(configurationName)) {
				logPrefix += "["+configurationName+"]";
			}
		}
		return logPrefix;
	}
}
