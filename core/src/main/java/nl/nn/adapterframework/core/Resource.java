/*
   Copyright 2019 Integration Partners

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
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Reference to a resource, for instance on the classpath. Can be accessed multiple times.
 * 
 * @author Gerrit van Brakel
 *
 */
public class Resource {

	private ClassLoader classLoader;
	private URL url;
	private String systemId;
	
	private Resource(ClassLoader classLoader, URL url, String systemId) {
		this.classLoader=classLoader;
		this.url=url;
		this.systemId=systemId;
	}
	
	public static Resource getResource(String resource) {
		return getResource(null, resource);
	}

	public static Resource getResource(ClassLoader classLoader, String resource) {
		return getResource(classLoader, resource, null);
	}
	
	public static Resource getResource(ClassLoader classLoader, String resource, String allowedProtocols) {
		if(classLoader == null) {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		String ref=resource.startsWith(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME)?resource.substring(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME.length()):resource;
		URL url = ClassUtils.getResourceURL(classLoader, ref, allowedProtocols);
		if (url==null) {
			return null;
		}
		String systemId;
		if (ref.indexOf(':')<0) {
			systemId=ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME+ref;
		} else {
			systemId=url.toExternalForm();
		}
		return new Resource(classLoader, url, systemId);
	}

	public String getCacheKey() {
		return url.toExternalForm();
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

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public String getSystemId() {
		return systemId;
	}
	
	public URL getURL() {
		return url;
	}


}
