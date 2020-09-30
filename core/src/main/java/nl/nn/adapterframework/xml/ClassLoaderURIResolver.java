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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Resolve URIs used in document(), xsl:import, and xsl:include.
 * 
 * @author Jaco de Groot
 * @author Gerrit van Brakel
 * @see ClassLoaderXmlEntityResolver
 */
public class ClassLoaderURIResolver implements URIResolver {
	protected Logger log = LogUtil.getLogger(this);
	
	private ClassLoader classLoader;

	public ClassLoaderURIResolver(ClassLoader classLoader) {
		if (log.isTraceEnabled()) log.trace("ClassLoaderURIResolver init with classloader ["+classLoader+"]");
		this.classLoader = classLoader;
	}

	public ClassLoaderURIResolver(Resource resource) {
		this(resource.getClassLoader());
	}

	public Resource resolveToResource(String href, String base) throws TransformerException {
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
			// href does not start with scheme/protocol, and does not start with a slash.
			// It must be relative to the base, or if that not exists, on the root of the classpath
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
		Resource resource = Resource.getResource(classLoader, ref, protocol);
		if (resource==null && ref2!=null) {
			if (log.isDebugEnabled()) log.debug("Could not resolve href ["+href+"] base ["+base+"] as ["+ref+"], now trying ref2 ["+ref2+"] protocol ["+protocol+"]");
			ref=ref2;
			resource = Resource.getResource(classLoader, ref, protocol);
		}
		if (resource==null) {
			String message = "Cannot get resource for href [" + href + "] with base [" + base + "] as ref ["+ref+"]" +(ref2==null?"":" nor as ref ["+ref1+"]")+" protocol ["+protocol+"] classloader ["+classLoader+"]";
			//log.warn(message); // TODO could log this message here, because Saxon does not log the details of the exception thrown. This will cause some duplicate messages, however. See for instance XsltSenderTest for example.
			throw new TransformerException(message);
		}
		if (log.isDebugEnabled()) log.debug("resolved href ["+href+"] base ["+base+"] to systemId ["+resource.getSystemId()+"] to url ["+resource.getURL()+"]");
		return resource;
	}
	
	@Override
	public Source resolve(String href, String base) throws TransformerException {
		Resource resource = resolveToResource(href, base);
		
		try {
			return resource.asSource();
		} catch (SAXException|IOException e) {
			throw new TransformerException(e);
		}
	}

	
}
