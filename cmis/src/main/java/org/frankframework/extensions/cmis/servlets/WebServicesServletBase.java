/*
   Copyright 2018-2020 Nationale-Nederlanden, 2022-2024 WeAreFrank!

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet;

import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.lifecycle.DynamicRegistration;

/**
 * It is important that we register the correct CXF bus, or else
 * JAX-RS (IAF-API / WebServiceListener) won't work properly
 *
 * @author Niels Meijer
 */
public abstract class WebServicesServletBase extends AbstractHttpServlet implements DynamicRegistration.ServletWithParameters {

	private static final long serialVersionUID = 2L;

	private transient CmisWebServicesServlet servlet;
	protected abstract String getCmisVersion();

	@Override
	public void init(ServletConfig config) throws ServletException {
		servlet = new CmisWebServicesServlet();
		servlet.init(config);
		super.init(config);
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		servlet.service(req, res);
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> returnMap = new HashMap<>();
		returnMap.put(CmisWebServicesServlet.PARAM_CMIS_VERSION, getCmisVersion());

		return returnMap;
	}

	@Override
	public void destroy() {
		servlet.destroy();
		super.destroy();
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return IBIS_FULL_SERVICE_ACCESS_ROLES;
	}

	/** Disabled by default, set servlet.WebServices10.enabled=true or servlet.WebServices11.enabled=true to enable this endpoint. */
	@Override
	public boolean isEnabled() {
		return false;
	}
}
