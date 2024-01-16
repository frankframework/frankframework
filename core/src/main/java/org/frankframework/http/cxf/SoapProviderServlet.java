/*
   Copyright 2022 WeAreFrank!

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

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;

import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.LogUtil;

@IbisInitializer
public class SoapProviderServlet extends CXFServlet implements DynamicRegistration.ServletWithParameters {
	private Logger log = LogUtil.getLogger(this);


	@Override
	public void setBus(Bus bus) {
		if(bus != null) {
			String busInfo = String.format("Successfully created %s with SpringBus [%s]", getName(), bus.getId());
			log.info(busInfo);
			getServletContext().log(busInfo);
		}

		super.setBus(bus);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// This event listens to all Spring refresh events.
		// When adding new Spring contexts (with this as a parent) refresh events originating from other contexts will also trigger this method.
		// Since we never want to reinitialize this servlet, we can ignore the 'refresh' event completely!
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return IBIS_FULL_SERVICE_ACCESS_ROLES;
	}

	@Override
	public String getUrlMapping() {
		return "/services/*,/servlet/*";
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<>();
//		parameters.put("config-location", "/WEB-INF/cxf-servlet.xml"); //Default CXF Spring config
		parameters.put("bus", "cxf"); //Default bus
		return parameters;
	}
}
