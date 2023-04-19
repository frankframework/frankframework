/*
Copyright 2016-2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.management.web.ApiException;
import nl.nn.adapterframework.management.web.FrankApiBase;

/**
 * Baseclass to fetch ibisContext + ibisManager
 * @deprecated should not be used anymore in favor of the FrankApiBase (which does not rely on the IbisContext).
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */
@Deprecated
public abstract class Base extends FrankApiBase {

	private IbisContext ibisContext = null;

	/**
	 * Retrieves the IbisContext from <code>servletConfig</code>.
	 */
	private void retrieveIbisContextFromServlet() {
		if(servletConfig == null) {
			throw new ApiException("no ServletConfig found to retrieve IbisContext from");
		}

		try {
			ibisContext = IbisApplicationServlet.getIbisContext(servletConfig.getServletContext());
		} catch (IllegalStateException e) {
			throw new ApiException(e);
		}
	}

	public IbisContext getIbisContext() {
		if(ibisContext == null) {
			retrieveIbisContextFromServlet();
		}

		if(ibisContext.getStartupException() != null) {
			throw new ApiException(ibisContext.getStartupException());
		}

		return ibisContext;
	}

	/**
	 * Retrieves the IbisManager from the IbisContext
	 */
	public IbisManager getIbisManager() {
		IbisManager ibisManager = getIbisContext().getIbisManager();

		if (ibisManager==null) {
			throw new ApiException("Could not retrieve ibisManager from IbisContext");
		}

		return ibisManager;
	}
}
