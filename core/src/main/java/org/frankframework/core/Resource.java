/*
   Copyright 2019-2021, 2024 WeAreFrank!

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
package org.frankframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.XmlUtils;

/**
 * Reference to a resource, for instance on the classpath. Can be accessed multiple times.
 *
 * @author Gerrit van Brakel
 */
public abstract class Resource implements IScopeProvider {
	protected IScopeProvider scopeProvider;

	protected Resource(IScopeProvider scopeProvider) {
		if(scopeProvider == null) {
			throw new IllegalStateException("a scopeProvider must be present");
		}

		this.scopeProvider = scopeProvider;
	}

	@Nullable
	public static Resource getResource(@Nonnull String resource) {
		return getResource(null, resource);
	}

	@Nullable
	public static Resource getResource(@Nullable IScopeProvider scopeProvider, @Nonnull String resource) {
		return getResource(scopeProvider, resource, null);
	}

	@Nullable
	public static Resource getResource(@Nullable IScopeProvider scopeProvider, @Nonnull String resource, String allowedProtocols) {
		if(scopeProvider == null) {
			scopeProvider = new GlobalScopeProvider(); // if no scope has been provided, assume to use the default 'global' scope.
		}
		String ref= resource.startsWith(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME) ? resource.substring(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME.length()) : resource;
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, ref, allowedProtocols);
		if (url==null) {
			return null;
		}

		String systemId;
		if (ref.indexOf(':')<0) {
			systemId= IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME+ref;
		} else {
			systemId=url.toExternalForm();
		}

		return new URLResource(scopeProvider, url, systemId);
	}

	public static class GlobalScopeProvider implements IScopeProvider {
		@Override
		public ClassLoader getConfigurationClassLoader() {
			return this.getClass().getClassLoader();
		}
	}

	/**
	 * @return Name of the resource
	 */
	public String getName() {
		return FilenameUtils.getName(getSystemId());
	}

	public Source asSource() throws SAXException, IOException {
		return XmlUtils.inputSourceToSAXSource(this);
	}

	public InputSource asInputSource() throws IOException {
		InputSource inputSource = new InputSource(openStream());
		inputSource.setSystemId(getSystemId());
		return inputSource;
	}

	public XMLInputSource asXMLInputSource() throws IOException {
		return new XMLInputSource(null, getSystemId(), null, openStream(), null);
	}

	/**
	 * @return Canonical path of the resource
	 */
	public abstract String getSystemId();

	public abstract InputStream openStream() throws IOException;

	@Override
	public final ClassLoader getConfigurationClassLoader() {
		return scopeProvider.getConfigurationClassLoader();
	}

	@Override
	public String toString() {
		return "ResourceHolder systemId ["+getSystemId()+"] scope ["+scopeProvider+"]";
	}
}
