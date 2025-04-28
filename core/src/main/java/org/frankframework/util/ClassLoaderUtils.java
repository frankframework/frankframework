/*
   Copyright 2023 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.core.IScopeProvider;

public class ClassLoaderUtils {

	private ClassLoaderUtils() {
		throw new IllegalStateException("Don't construct utility class");
	}

	private static final Logger log = LogUtil.getLogger(ClassLoaderUtils.class);
	private static final String DEFAULT_ALLOWED_PROTOCOLS = AppConstants.getInstance().getString("classloader.allowed.protocols", null);

	/**
	 * Get a resource-URL directly from the ClassPath
	 * @param resource name of the resource you are trying to fetch the URL from
	 * @return URL of the resource or null if it can't be not found
	 */
	@Nullable
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
	public static URL getResourceURL(@Nullable IScopeProvider scopeProvider, @Nonnull String resource) {
		return getResourceURL(scopeProvider, resource, null);
	}

	/**
	 * Get a resource-URL from a ClassLoader, therefore the resource should not start with a leading slash
	 * @param scopeProvider to retrieve the file from, or NULL when you want to retrieve the resource directly from the ClassPath (using an absolute path)
	 * @param resource name of the resource you are trying to fetch the URL from
	 * @return URL of the resource or null if it can't be not found
	 */
	@Nullable
	public static URL getResourceURL(@Nullable IScopeProvider scopeProvider, @Nonnull String resource, @Nullable String allowedProtocols) {
		ClassLoader classLoader;
		if(scopeProvider == null) { // Used by ClassPath resources
			classLoader = Thread.currentThread().getContextClassLoader();
		} else {
			classLoader = scopeProvider.getConfigurationClassLoader();
		}

		String resourceToUse = resource; // Don't change the original resource name for logging purposes
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
				log.debug("Could not find resource [{}] in classloader [{}] now trying via native lookup", resource, classLoader);
				List<String> protocols = new ArrayList<>();
				protocols.addAll(getAllowedProtocols());
				protocols.addAll(toList(allowedProtocols));
				return getResourceNative(resourceToUse, protocols);
			} else {
				log.debug("Cannot lookup resource [{}] in classloader [{}] and no protocol to try as URL", resource, classLoader);
			}
		}

		return url;
	}

	/**
	 * Attempt to find the resource native (without ClassLoader).
	 * Protocol must be allowed by setting <code>classloader.allowed.protocols=file,ftp,protocol-name</code>
	 */
	private static @Nullable URL getResourceNative(String resource, List<String> allowedProtocols) {
		String protocol = resource.substring(0, resource.indexOf(":"));
		if (allowedProtocols.isEmpty()) {
			log.debug("Could not find resource as URL [{}] with protocol [{}], no allowedProtocols specified", resource, protocol);
			return null;
		}
		log.debug("determined protocol [{}] for resource [{}]", protocol, resource);

		if(!allowedProtocols.contains(protocol)) {
			log.debug("Cannot lookup resource [{}] protocol [{}] not in allowedProtocols {}", resource, protocol, allowedProtocols);
			return null;
		}

		String escapedURL = resource.replace(" ", "%20");
		try {
			return new URL(escapedURL);
		} catch(MalformedURLException e) {
			log.debug("Could not find resource [{}] as URL [{}]: {}", resource, escapedURL, e.getMessage());
			return null;
		}
	}

	@Nonnull
	public static List<String> getAllowedProtocols() {
		return toList(DEFAULT_ALLOWED_PROTOCOLS);
	}

	@Nonnull
	private static List<String> toList(@Nullable String protocolList) {
		if(StringUtils.isBlank(protocolList)) {
			return Collections.emptyList();
		}
		//Arrays.asList(..) won't return an empty List when empty.
		return Arrays.asList(protocolList.split(","));
	}
}
