/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.validation.xsd;

import java.io.IOException;
import java.io.Reader;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.util.StreamUtil;
import org.frankframework.validation.IXSD;
import org.frankframework.validation.AbstractXSD;

/**
 * XSD based on a reference to a resource on the classPath.
 *
 * @author Gerrit van Brakel
 */
public class ResourceXsd extends AbstractXSD {

	private String resourceRef;
	private Resource resource;

	@Override
	public void initNoNamespace(IScopeProvider scopeProvider, String resourceRef) throws ConfigurationException {
		super.initNoNamespace(scopeProvider, resourceRef);
	}

	@Override
	public void initNamespace(String namespace, IScopeProvider scopeProvider, String resourceRef) throws ConfigurationException {
		this.resourceRef = resourceRef.replace("%20", " ");

		resource = Resource.getResource(scopeProvider, this.resourceRef);
		if (resource == null) {
			throw new ConfigurationException("Cannot find resource [" + resourceRef + "] in scope [" + scopeProvider + "]");
		}

		super.initNamespace(namespace, scopeProvider, resourceRef);
	}

	@Override
	public Reader getReader() throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(resource.openStream());
	}

	@Override
	public String getResourceBase() {
		return resourceRef.substring(0, resourceRef.lastIndexOf('/') + 1);
	}

	@Override
	public String getSystemId() {
		return resource.getSystemId();
	}

	@Override
	public int compareToByReferenceOrContents(IXSD other) {
		if (other instanceof ResourceXsd) {
			return getSystemId().compareTo(other.getSystemId());
		}
		return compareToByContents(other);
	}
}
