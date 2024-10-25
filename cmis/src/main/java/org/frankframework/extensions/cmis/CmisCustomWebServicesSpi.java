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

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.webservices.CmisWebServicesSpi;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CmisCustomWebServicesSpi extends CmisWebServicesSpi {
	private final BindingSession bindingSession;

	/**
	 * Constructor.
	 *
	 * @param session
	 */
	public CmisCustomWebServicesSpi(BindingSession session) {
		super(session);
		log.debug("Creating instance of {}", getClass().getSimpleName());
		this.bindingSession = session;
	}

	@Override
	public void close() {
		CmisUtils.closeBindingSession(this, bindingSession);
		super.close();
	}
}
