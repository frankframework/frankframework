/*
Copyright 2016 Integration Partners B.V.

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

import javax.servlet.ServletConfig;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.log4j.Logger;

/**
* Baseclass to fetch ibisContext + ibisManager
* 
* @author	Niels Meijer
*/

public abstract class Base {
	protected Logger log = LogUtil.getLogger(this);
	protected IbisContext ibisContext = null;
	protected IbisManager ibisManager = null;
	
	protected void initBase(ServletConfig servletConfig) {
		String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
		ibisContext = (IbisContext) servletConfig.getServletContext().getAttribute(attributeKey);
		ibisManager = null;
		if (ibisContext != null) {
			ibisManager = ibisContext.getIbisManager();
		}
		if (ibisManager==null) {
			log.warn("Could not retrieve ibisManager from context");
		} else {
			log.debug("retrieved ibisManager ["+ClassUtils.nameOf(ibisManager)+"]["+ibisManager+"] from servlet context attribute ["+attributeKey+"]");
		}
	}
}
