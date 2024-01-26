/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.extensions.cmis;

import java.util.Map;

import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.bindings.cache.TypeDefinitionCache;
import org.apache.chemistry.opencmis.client.runtime.SessionImpl;
import org.apache.chemistry.opencmis.client.runtime.cache.Cache;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;

import lombok.extern.log4j.Log4j2;

/**
 * Subclass of CMIS {@link SessionImpl} that is closeable so it can clean up
 * closeable items in the session binding map.
 */
@Log4j2
public class CloseableCmisSession extends SessionImpl implements AutoCloseable {

	public CloseableCmisSession(Map<String, String> parameters, ObjectFactory objectFactory, AuthenticationProvider authenticationProvider, Cache cache, TypeDefinitionCache typeDefCache) {
		super(parameters, objectFactory, authenticationProvider, cache, typeDefCache);
	}

	@Override
	public void close() {
		log.debug("CMIS Session Close, SPI Binding class name: [{}]", getSessionParameters().get(SessionParameter.BINDING_SPI_CLASS));
		clear();
		CmisBinding binding = getBinding();
		if (binding != null) {
			log.debug("Closing CMIS Bindings instance [{}:{}]", binding.getClass().getSimpleName(), binding);
			binding.close();
		} else {
			log.debug("Session has no CMIS Bindings");
		}

	}
}
