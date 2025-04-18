/*
   Copyright 2022 - 2024 WeAreFrank!

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
package org.frankframework.http.cxf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.IbisInitializer;

@IbisInitializer
public class SoapProviderServlet extends AbstractHttpServlet implements DynamicRegistration.ServletWithParameters {
	private transient FrankCXFServlet servlet;

	@Override
	public void init(ServletConfig config) throws ServletException {
		servlet = new FrankCXFServlet();
		servlet.init(config);
		super.init(config);
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		servlet.service(req, res);
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return IBIS_FULL_SERVICE_ACCESS_ROLES;
	}

	@Override
	public void destroy() {
		servlet.destroy();
		super.destroy();
	}

	@Override
	public String getUrlMapping() {
		return "/services/*,/servlet/*";
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("bus", "cxf"); // Default bus
		return parameters;
	}

}
