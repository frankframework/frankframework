/*
   Copyright 2018, 2020 Nationale-Nederlanden

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
package nl.nn.ibistesttool;

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.webcontainer.WebContainerServlet;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.testtool.echo2.Echo2Application;

/**
 * @author Jaco de Groot
 */
public class Servlet extends WebContainerServlet {
//	Draaien buiten Ibis:
//	private WebApplicationContext webApplicationContext;
//	public void init(ServletConfig servletConfig) throws ServletException {
//		super.init(servletConfig);
//		webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletConfig.getServletContext());
//	}
//	public ApplicationInstance newApplicationInstance() {
//		return (Echo2Application)webApplicationContext. getBean("echo2Application");
//	}

// werkt niet, waarschijnlijk wel als deze servlet voor ibis servlet wordt geladen
//	// TODO moet anders
//	public static String rootRealPath;
//	public void init(ServletConfig servletConfig) throws ServletException {
//		super.init(servletConfig);
//		rootRealPath = servletConfig.getServletContext().	getRealPath("/");
////		AppConstants appConstants = AppConstants.getInstance();
////		appConstants.put("rootRealPath", rootRealPath);
//	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@Override
	public ApplicationInstance newApplicationInstance() {
		IbisContext ibisContext = IbisApplicationServlet.getIbisContext(getServletContext());
		return ibisContext.getBean("echo2Application", Echo2Application.class);
	}

}
