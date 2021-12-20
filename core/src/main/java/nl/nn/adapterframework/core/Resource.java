/*
   Copyright 2019-2021 WeAreFrank!

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
package nl.nn.adapterframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Source;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.FilenameUtils;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Reference to a resource, for instance on the classpath. Can be accessed multiple times.
 * 
 * @author Gerrit van Brakel
 *
 */
public class Resource implements IScopeProvider {

	private IScopeProvider scopeProvider;
	private URL url;
	private String systemId;

	private Resource(IScopeProvider scopeProvider, URL url, String systemId) {
		this.scopeProvider=scopeProvider;
		this.url=url;
		this.systemId=systemId;
	}

	public static Resource getResource(String resource) {
		return getResource(null, resource);
	}

	public static Resource getResource(String resource, InputStream inputStream) {
		return getResource(null, resource);
	}

	public static Resource getResource(IScopeProvider scopeProvider, String resource) {
		return getResource(scopeProvider, resource, null);
	}

	public static Resource getResource(IScopeProvider scopeProvider, String resource, String allowedProtocols) {
		if(scopeProvider == null) {
			scopeProvider = new GlobalScopeProvider(); // if no scope has been provided, assume to use the default 'global' scope.
		}
		String ref=resource.startsWith(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME)?resource.substring(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME.length()):resource;
		URL url = ClassUtils.getResourceURL(scopeProvider, ref, allowedProtocols);
		if (url==null) {
			return null;
		}

		String systemId;
		if (ref.indexOf(':')<0) {
			systemId=ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME+ref;
		} else {
			systemId=url.toExternalForm();
		}
		return new Resource(scopeProvider, url, systemId);
	}

	private static class GlobalScopeProvider implements IScopeProvider {
		@Override
		public ClassLoader getConfigurationClassLoader() {
			return this.getClass().getClassLoader();
		}
	}

	public String getName() {
		return FilenameUtils.getName(systemId);
	}

	public InputStream openStream() throws IOException {
		return url.openStream();
	}

	public InputSource asInputSource() throws IOException {
		InputSource inputSource = new InputSource(openStream());
		inputSource.setSystemId(systemId);
		return inputSource;
	}

	public Source asSource() throws SAXException, IOException {
		return XmlUtils.inputSourceToSAXSource(this);
	}

	public String getSystemId() {
		return systemId;
	}

	@Override
	public ClassLoader getConfigurationClassLoader() {
		return scopeProvider.getConfigurationClassLoader();
	}

	@Override
	public String toString() {
		return "ResourceHolder url ["+url+"] systemId ["+systemId+"] scope ["+scopeProvider+"]";
	}
}
