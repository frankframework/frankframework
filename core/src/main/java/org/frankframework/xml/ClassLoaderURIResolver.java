/*
   Copyright 2018, 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.xml;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;

/**
 * Resolve URIs used in document(), xsl:import, and xsl:include.
 *
 * @author Jaco de Groot
 * @author Gerrit van Brakel
 */
public class ClassLoaderURIResolver implements URIResolver {
	protected Logger log = LogUtil.getLogger(this);
	private final IScopeProvider scopeProvider;
	private final List<String> allowedProtocols = ClassLoaderUtils.getAllowedProtocols();

	public ClassLoaderURIResolver(IScopeProvider scopeProvider) {
		if (log.isTraceEnabled()) log.trace("ClassLoaderURIResolver init with scopeProvider [{}]", scopeProvider);
		this.scopeProvider = scopeProvider;
	}

	public Resource resolveToResource(String href, String base) throws TransformerException {
		String absoluteOrRelativeRef;
		String globalClasspathRef=null;
		String protocol=null;

		if (href.startsWith("/") || href.contains(":")) {
			// href is absolute, search on the full classpath
			absoluteOrRelativeRef=href;
			if (href.contains(":")) {
				protocol=href.substring(0,href.indexOf(":"));
			}
			if (StringUtils.isNotEmpty(protocol)) { //if href contains a protocol, verify that it's allowed to look it up
				if(allowedProtocols.isEmpty()) {
					throw new TransformerException("Cannot lookup resource ["+href+"] with protocol ["+protocol+"], no allowedProtocols");
				} else if(!allowedProtocols.contains(protocol)) {
					throw new TransformerException("Cannot lookup resource ["+href+"] not allowed with protocol ["+protocol+"] allowedProtocols "+ allowedProtocols);
				}
			}
		} else {
			// href does not start with scheme/protocol, and does not start with a slash.
			// It must be relative to the base, or if that not exists, on the root of the classpath
			if (base != null && base.contains("/")) {
				absoluteOrRelativeRef = base.substring(0, base.lastIndexOf("/") + 1) + href;
				globalClasspathRef = href; // if ref1 fails, try href on the global classpath
				if (base.contains(":")) {
					protocol=base.substring(0,base.indexOf(":"));
				}
			} else {
				// cannot use base to prefix href
				absoluteOrRelativeRef=href;
			}
		}

		String ref=absoluteOrRelativeRef;
		Resource resource = Resource.getResource(scopeProvider, ref, protocol);
		if (resource==null && globalClasspathRef!=null) {
			if (log.isDebugEnabled())
				log.debug("Could not resolve href [{}] base [{}] as [{}], now trying ref2 [{}] protocol [{}]", href, base, ref, globalClasspathRef, protocol);
			ref=globalClasspathRef;
			resource = Resource.getResource(scopeProvider, ref, null);
		}

		if (resource==null) {
			String message = "Cannot get resource for href [" + href + "] with base [" + base + "] as ref ["+ref+"]" +(globalClasspathRef==null?"":" nor as ref ["+absoluteOrRelativeRef+"]")+" protocol ["+protocol+"] in scope ["+scopeProvider+"]";
			//log.warn(message); // TODO could log this message here, because Saxon does not log the details of the exception thrown. This will cause some duplicate messages, however. See for instance XsltSenderTest for example.
			throw new TransformerException(message);
		}
		if (log.isDebugEnabled()) log.debug("resolved href [{}] base [{}] to resource [{}]", href, base, resource);
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
