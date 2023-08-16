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

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * Start the Framework with a servlet so `application startup order` can be used.
 * 
 * @author Niels Meijer
 */
public class IbisApplicationServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	private final Logger log = LogUtil.getLogger(this);
	private final Logger applicationLog = LogUtil.getLogger("APPLICATION");
	public static final String CONTEXT_KEY = "IbisContext";
	private IbisContext ibisContext;
	private static final String EXCEPTION_KEY = "StartupException";

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
			servletContext.setAttribute(EXCEPTION_KEY, t);
			applicationLog.fatal("IBIS WebApplicationInitializer failed to initialize", t);
			throw t; //If the IBIS WebApplicationInitializer can't be found or initialized, throw the exception
		}

		servletContext.log("Starting IbisContext");
		ibisContext = new IbisContext();
		ibisContext.setParentContext(parentContext);
		ibisContext.init();

		Exception startupException = ibisContext.getStartupException();
		if(startupException != null) { //Check if there are any problems initializing Spring
			String msg = this.getClass().getSimpleName()+" finished without successfully initializing the IbisContext";
			applicationLog.fatal(msg, startupException);

			//We can't call servletContext.log(message, Exception) as it will prevent the servlet from starting up
			servletContext.log(String.format("%s, check ibis logs for more information! (%s) %s", msg, startupException.getClass().getName(), startupException.getMessage()));
		}

		// save the IbisContext in the ServletContext
		servletContext.setAttribute(CONTEXT_KEY, ibisContext);
		log.debug("stored IbisContext [" + ClassUtils.nameOf(ibisContext) + "]["+ ibisContext + "] in ServletContext under key ["+ CONTEXT_KEY + "]");
		applicationLog.fatal("Initialized IbisContext: {}", ClassUtils.nameOf(ibisContext));
	}

	/**
	 * Retrieves the IbisContext from the ServletContext
	 * @param servletContext
	 * @return IbisContext or IllegalStateException when not found
	 */
	public static IbisContext getIbisContext(ServletContext servletContext) {
		Throwable t = (Throwable) servletContext.getAttribute(EXCEPTION_KEY); // non-recoverable startup error
		if(t != null) {
			throw new IllegalStateException("Could not initialize IbisContext", t);
		}

		IbisContext ibisContext = (IbisContext)servletContext.getAttribute(CONTEXT_KEY);
		if(ibisContext == null) {
			throw new IllegalStateException("IbisContext not found in ServletContext");
		}

		return ibisContext;
	}

	@Override
	public void destroy() {
		applicationLog.fatal("shutting down IbisContext");
		getServletContext().log("Shutting down IbisContext");
		if(ibisContext != null) {
			ibisContext.close();
		}

		super.destroy();
	}
}
