/*
   Copyright 2019-2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.lifecycle;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Start the Framework with a servlet so `application startup order` can be used.
 * 
 * @author Niels Meijer
 */
public class IbisApplicationServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	private final Logger log = LogUtil.getLogger(this);
	public static final String KEY_CONTEXT = "KEY_CONTEXT";
	private IbisContext ibisContext;
	private static final String KEY_EXCEPTION = "KEY_EXCEPTION";

	@Override
	public void init() throws ServletException {
		super.init();

		ServletContext servletContext = getServletContext();
		AppConstants appConstants = AppConstants.getInstance();

		String realPath = servletContext.getRealPath("/");
		if (realPath != null) {
			appConstants.put("webapp.realpath", realPath);
		} else {
			log.warn("Could not determine webapp.realpath");
		}
		String projectBaseDir = Misc.getProjectBaseDir();
		if (projectBaseDir != null) {
			appConstants.put("project.basedir", projectBaseDir);
		} else {
			log.info("Could not determine project.basedir");
		}

		ApplicationContext parentContext = null;
		try {
			parentContext = WebApplicationContextUtils.getWebApplicationContext(servletContext); //This can throw many different types of errors!
			if(parentContext == null) {
				throw new IllegalStateException("No IBIS WebApplicationInitializer found. Aborting launch...");
			}
		} catch (Throwable t) {
			servletContext.setAttribute(KEY_EXCEPTION, t);
			log.error("IBIS WebApplicationInitializer failed to initialize", t);
			throw t; //If the IBIS WebApplicationInitializer can't be found or initialized, throw the exception
		}

		servletContext.log("Starting IbisContext");
		ibisContext = new IbisContext();
		ibisContext.setParentContext(parentContext);
		ibisContext.init();

		if(ibisContext.getIbisManager() == null) { //Simplest check to see if the IbisContext has started up successfully is by retrieving the IbisManager bean.
			Exception ex = ibisContext.getBootState().getException();
			log.error("Servlet finished without successfully initializing the ibisContext", ex);
			String msg = "IbisContext failed to initialize, check ibis logs for more information!";
			if(ex != null) {
				msg += " ("+ex.getClass().getName()+") "+ex.getMessage();
			}
			servletContext.log(msg); //We can't log the stacktrace here as it will prevent the servlet from starting up
			servletContext.setAttribute(KEY_EXCEPTION, ex);
		} else {
			String attributeKey = appConstants.getResolvedProperty(KEY_CONTEXT);
			servletContext.setAttribute(attributeKey, ibisContext);
			log.debug("stored IbisContext [" + ClassUtils.nameOf(ibisContext) + "]["+ ibisContext + "] in ServletContext under key ["+ attributeKey + "]");

			log.debug("Servlet init finished");
		}
	}

	/**
	 * Retrieves the IbisContext from the ServletContext
	 * @param servletContext
	 * @return IbisContext or IllegalStateException when not found
	 */
	public static IbisContext getIbisContext(ServletContext servletContext) {
		AppConstants appConstants = AppConstants.getInstance();
		String ibisContextKey = appConstants.getResolvedProperty(KEY_CONTEXT);
		IbisContext ibisContext = (IbisContext)servletContext.getAttribute(ibisContextKey);

		if(ibisContext == null) {
			Throwable t = (Throwable) servletContext.getAttribute(KEY_EXCEPTION);
			throw new IllegalStateException("Unable to retrieve IbisContext from ServletContext attribute ["+KEY_CONTEXT+"]", t);
		}
		return ibisContext;
	}

	@Override
	public void destroy() {
		getServletContext().log("Shutting down IbisContext");
		if(ibisContext != null) {
			ibisContext.destroy();
		}

		super.destroy();
	}
}
