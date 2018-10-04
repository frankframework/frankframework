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
package nl.nn.adapterframework.extensions.cxf;

import javax.servlet.ServletConfig;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.cxf.Bus;
import org.apache.log4j.Logger;

/**
 * It is important that we register the correct CXF bus, or else JAX-RS won't work properly
 * 
 * @author Niels Meijer
 * @author Jaco de Groot
 */
public class CXFServlet extends org.apache.cxf.transport.servlet.CXFServlet {

	private static final long serialVersionUID = 1L;
	private final Logger log = LogUtil.getLogger(this);

	@Override
	protected void loadBus(ServletConfig sc) {
		log.debug("loading cxf bus for servlet["+sc.getServletName()+"]");

		AppConstants appConstants = AppConstants.getInstance();
		String ibisContextKey = appConstants.getResolvedProperty(ConfigurationServlet.KEY_CONTEXT);
		IbisContext ibisContext = (IbisContext)getServletContext().getAttribute(ibisContextKey);
		setBus((Bus)ibisContext.getBean("cxf"));
	}
}
