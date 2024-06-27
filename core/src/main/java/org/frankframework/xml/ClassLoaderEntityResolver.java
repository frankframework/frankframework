/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.apache.logging.log4j.Logger;
import org.frankframework.util.LogUtil;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;

/**
 * @see org.xml.sax.EntityResolver
 * This class offers the resolveEntity method to resolve a systemId to a resource on the classpath.
 *
 * It's important that the XMLEntityResolver does not return NULL, when it cannot find a resource.
 * Returning NULL will cause the XmlReader to fall back to it's built in EntityResolver.
 *
 * @since 4.1.1
 */

public class ClassLoaderEntityResolver implements EntityResolver {
	protected Logger log = LogUtil.getLogger(this);
	private final IScopeProvider scopeProvider;

	public ClassLoaderEntityResolver(IScopeProvider scopeProvider) {
		this.scopeProvider = scopeProvider;
	}

	/**
	 * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

		if (log.isDebugEnabled())
			log.debug("Resolving publicId [{}] systemId [{}] in scope [{}]", publicId, systemId, scopeProvider);
		Resource resource = Resource.getResource(scopeProvider, systemId);
		if(resource != null) {
			return resource.asInputSource();
		}

		// If nothing found, return a SAXException. If NULL is returned it can trigger default (fallback) behaviour using the internal EntityResolver.
		String message = "Cannot get resource for publicId [" + publicId + "] with systemId [" + systemId + "] in scope ["+scopeProvider+"]";
		throw new SAXException(message);
	}
}
