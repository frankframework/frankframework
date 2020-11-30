/*
   Copyright 2018-2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis.servlets;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.lifecycle.ServletManager;
import org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * It is important that we register the correct CXF bus, or else JAX-RS won't work properly
 * 
 * @author Niels Meijer
 */
public abstract class WebServicesServletBase extends CmisWebServicesServlet implements DynamicRegistration.ServletWithParameters {

	private static final long serialVersionUID = 1L;

	protected abstract String getCmisVersion();

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> returnMap = new HashMap<String, String>();
		returnMap.put(PARAM_CMIS_VERSION, getCmisVersion());

		return returnMap;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int loadOnStartUp() {
		return 1;
	}

	@Override
	public HttpServlet getServlet() {
		return this;
	}

	@Override
	public String[] getRoles() {
		return "IbisWebService,IbisTester".split(",");
	}

	@Autowired
	public void setServletManager(ServletManager servletManager) {
		servletManager.register(this);
	}
}
