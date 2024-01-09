/*
   Copyright 2019-2020 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.extensions.cmis.servlets;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;

import org.frankframework.lifecycle.DynamicRegistration;

/**
 * It is important that we register the correct (CMIS) CXF bus, or else
 * JAX-RS (IAF-API / WebServiceListener) won't work properly
 *
 * @author Niels Meijer
 */
public abstract class AtomPubServletBase extends CmisAtomPubServlet implements DynamicRegistration.ServletWithParameters {

	private static final long serialVersionUID = 1L;
	protected abstract String getCmisVersionStr();

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> returnMap = new HashMap<>();
		returnMap.put(PARAM_CMIS_VERSION, getCmisVersionStr());
		returnMap.put(PARAM_CALL_CONTEXT_HANDLER, "org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler");

		return returnMap;
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return IBIS_FULL_SERVICE_ACCESS_ROLES;
	}

	/** Disabled by default, set servlet.AtomPub10.enabled=true or servlet.AtomPub11.enabled=true to enable this endpoint. */
	@Override
	public boolean isEnabled() {
		return false;
	}
}
