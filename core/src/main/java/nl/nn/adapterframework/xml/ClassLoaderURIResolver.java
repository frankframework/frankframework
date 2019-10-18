/*
   Copyright 2018, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.xml;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.validation.ClassLoaderXmlEntityResolver;

/**
 * Resolve URIs used in document(), xsl:import, and xsl:include.
 * 
 * @author Jaco de Groot
 * @author Gerrit van Brakel
 * @see ClassLoaderXmlEntityResolver
 */
public class ClassLoaderURIResolver implements URIResolver {
	private Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader;

	public ClassLoaderURIResolver(ClassLoader classLoader) {
		if (log.isDebugEnabled()) log.debug("ClassLoaderURIResolver init with classloader ["+classLoader+"]");
		this.classLoader = classLoader;
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		String ref1;
		String ref2=null;
		String protocol=null;
		if (href.startsWith("/") || href.contains(":")) {
			// href is absolute, search on the full classpath
			ref1=href;
			if (href.contains(":")) {
				protocol=href.substring(0,href.indexOf(":"));
			}
		} else {
			// href is relative, construct href from base
			if (base != null && base.contains("/")) {
				ref1 = base.substring(0, base.lastIndexOf("/") + 1) + href;
				ref2 = href; // if ref1 fails, try href on the global classpath
				if (base.contains(":")) {
					protocol=base.substring(0,base.indexOf(":"));
				}
			} else {
				// cannot use base to prefix href
				ref1=href;
			}
		}

		String ref=ref1;
		URL url = ClassUtils.getResourceURL(classLoader, ref, protocol);
		if (url==null && ref2!=null) {
			log.debug("Could not resolve href ["+href+"] base ["+base+"] as ["+ref+"], now trying ref2 ["+ref2+"] protocol ["+protocol+"]");
			ref=ref2;
			url = ClassUtils.getResourceURL(classLoader, ref, protocol);
		}
		if (url==null) {
			String message = "Cannot get resource for href [" + href + "] with base [" + base + "] as ref ["+ref+"]" +(ref2==null?"":" nor as ref ["+ref1+"]")+" protocol ["+protocol+"] classloader ["+classLoader+"]";
			log.warn(message);
			throw new TransformerException(message);
		}
		log.debug("resolved href ["+href+"] base ["+base+"] to ["+url+"]");
		
		try {
			StreamSource streamSource = new StreamSource(url.openStream(), ref);
			return streamSource;
		} catch (IOException e) {
			throw new TransformerException(e);
		}
	}

	
}
