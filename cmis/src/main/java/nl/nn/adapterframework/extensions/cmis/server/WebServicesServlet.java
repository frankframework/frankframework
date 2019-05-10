/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.log4j.Logger;

/**
 * It is important that we register the correct CXF bus, or else JAX-RS won't work properly
 * 
 * @author Niels Meijer
 */
public class WebServicesServlet extends CmisWebServicesServlet {

	private static final long serialVersionUID = 1L;
	private final Logger log = LogUtil.getLogger(this);
	private IbisContext ibisContext = null;

	@Override
	public void init(ServletConfig sc) throws ServletException {

		AppConstants appConstants = AppConstants.getInstance();
		String ibisContextKey = appConstants.getResolvedProperty(ConfigurationServlet.KEY_CONTEXT);
		ibisContext = (IbisContext)sc.getServletContext().getAttribute(ibisContextKey);
		if(ibisContext.getIbisManager() == null)
			throw new ServletException("failed to retreive ibisContext from servletContext, did the IBIS start up correctly?");

		super.init(sc);
	}

	@Override
	protected void loadBus(ServletConfig sc) {
		log.debug("loading cxf bus for servlet["+sc.getServletName()+"]");

		Bus originalBus = (Bus)ibisContext.getBean("cxf");
		setBus(originalBus);

		//We have to call the original loadBus in order to register the CMIS endpoints. These will be added to a separate bus, which should not impact the IBIS.
		super.loadBus(sc);

		//We need to change the default bus back to the original SpringBus, else it will break config reloads
		BusFactory.setDefaultBus(originalBus);
	}

}
