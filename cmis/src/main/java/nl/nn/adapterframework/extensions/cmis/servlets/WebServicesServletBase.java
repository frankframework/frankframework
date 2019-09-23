/*
   Copyright 2018, 2019 Nationale-Nederlanden

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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * It is important that we register the correct CXF bus, or else JAX-RS won't work properly
 * 
 * @author Niels Meijer
 */
public abstract class WebServicesServletBase extends CmisWebServicesServlet implements DynamicRegistration.ServletWithParameters, ApplicationContextAware {

	private static final long serialVersionUID = 1L;
	private final Logger log = LogUtil.getLogger(this);
	private ApplicationContext applicationContext = null;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	protected abstract String getCmisVersion();

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> returnMap = new HashMap<String, String>();
		returnMap.put(PARAM_CMIS_VERSION, getCmisVersion());

		return returnMap;
	}

	@Override
	protected void loadBus(ServletConfig sc) {
		log.debug("loading cxf bus for servlet["+sc.getServletName()+"]");

		Bus originalBus = (Bus) applicationContext.getBean("cxf");
		setBus(originalBus);

		//We have to call the original loadBus in order to register the CMIS endpoints. These will be added to a separate bus, which should not impact the IBIS.
		super.loadBus(sc);

		//We need to change the default bus back to the original SpringBus, else it will break config reloads
		BusFactory.setDefaultBus(originalBus);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int loadOnStartUp() {
		return -1;
	}

	@Override
	public HttpServlet getServlet() {
		return this;
	}

	@Override
	public String[] getRoles() {
		return "IbisWebService,IbisTester".split(",");
	}
}
