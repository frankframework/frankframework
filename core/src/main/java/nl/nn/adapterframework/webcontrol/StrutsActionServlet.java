/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.struts.action.ActionServlet;
import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.AppConstants;

@IbisInitializer
public class StrutsActionServlet extends ActionServlet implements DynamicRegistration.ServletWithParameters {

	@Override
	public void init() throws ServletException {
		getServletContext().addFilter("ParamWrapperFilter", ParamWrapperFilter.class);

		super.init();
	}

	@Override
	public HttpServlet getServlet() {
		return this;
	}

	@Override
	public String getUrlMapping() {
		return "*.do";
	}

	@Override
	public String[] getRoles() {
		return null;
	}

	@Override
	public String getName() {
		return "GUI 1.2 (Struts Action Servlet)";
	}

	@Override
	public int loadOnStartUp() {
		return 0;
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> params = new HashMap<String, String>();

		params.put("application", "ApplicationResources");
		params.put("config", "/WEB-INF/struts-config.xml");
		params.put("debug", "2");
		params.put("detail", "2");
		params.put("validate", "true");

		return params;
	}

	@Autowired
	public void setServletManager(ServletManager servletManager) {
		boolean enabled = AppConstants.getInstance().getBoolean("strutsConsole.enabled", false);
		if(enabled) {
			servletManager.register(this);
		}
	}
}
