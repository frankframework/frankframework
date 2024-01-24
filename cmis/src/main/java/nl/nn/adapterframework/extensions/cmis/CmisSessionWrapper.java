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
package nl.nn.adapterframework.extensions.cmis;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CmisSessionWrapper implements AutoCloseable {

	private final @Getter Session cmisSession;

	public CmisSessionWrapper(Session cmisSession) {
		this.cmisSession = cmisSession;
	}

	@Override
	public void close() {
		log.debug("CMIS Session Close, SPI Binding class name: [{}]", cmisSession.getSessionParameters().get(SessionParameter.BINDING_SPI_CLASS));
		cmisSession.clear();
		CmisBinding binding = cmisSession.getBinding();
		if (binding != null) {
			log.debug("Closing CMIS Bindings instance [{}:{}]", binding.getClass().getSimpleName(), binding);
			binding.close();
		} else {
			log.debug("Session has no CMIS Bindings");
		}

	}
}
