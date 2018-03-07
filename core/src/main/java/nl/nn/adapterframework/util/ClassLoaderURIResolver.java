/*
   Copyright 2018 Nationale-Nederlanden

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

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import nl.nn.adapterframework.configuration.classloaders.BytesClassLoader;
import nl.nn.adapterframework.validation.ClassLoaderXmlEntityResolver;

import org.apache.log4j.Logger;

/**
 * Resolve URIs used in document(), xsl:import, and xsl:include.
 * 
 * @author Jaco de Groot
 * @see ClassLoaderXmlEntityResolver
 */
public class ClassLoaderURIResolver implements URIResolver {
	private Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader;

	ClassLoaderURIResolver(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		String absoluteHref = href;
		if (base != null && base.startsWith(BytesClassLoader.PROTOCOL + ":")
				&& base.contains("/")) {
			absoluteHref = base.substring(BytesClassLoader.PROTOCOL.length()
					+ 1, base.lastIndexOf("/") + 1) + href;
		}
		try {
			URL url = ClassUtils.getResourceURL(classLoader, absoluteHref);
			if (url == null) {
				String message = "Cannot resolve href '" + href + "' with base '" + base + "'";
				log.warn(message);
				throw new TransformerException(message);
			}
			StreamSource streamSource = new StreamSource(url.openStream(), absoluteHref);
			return streamSource;
		} catch (IOException e) {
			throw new TransformerException(e);
		}
	}

}
