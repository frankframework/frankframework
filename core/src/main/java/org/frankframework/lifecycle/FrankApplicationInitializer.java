/*
   Copyright 2023-2024 WeAreFrank!

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
package org.frankframework.lifecycle;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisContext;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;

/**
 * Spring WebApplicationInitializer that should start after the {@link FrankEnvironmentInitializer} has been configured.
 *
 * TODO: IbisContext should be directly wired under the EnvironmentContext.
 *
 * @author Niels Meijer
 */
@Log4j2
@Order(Ordered.LOWEST_PRECEDENCE)
public class FrankApplicationInitializer implements WebApplicationInitializer {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	public static final String CONTEXT_KEY = "IbisContext";
	private static final String EXCEPTION_KEY = "StartupException";

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		APPLICATION_LOG.debug("Starting Frank ApplicationContext");
		servletContext.addListener(new ContextCloseEventListener());
		AppConstants appConstants = AppConstants.getInstance();

		String realPath = servletContext.getRealPath("/");
		if (realPath != null) {
			appConstants.put("webapp.realpath", realPath);
		} else {
			log.warn("Could not determine webapp.realpath");
		}

		ApplicationContext parentContext = null;
		try {
			parentContext = WebApplicationContextUtils.getWebApplicationContext(servletContext); //This can throw many different types of errors!
			if(parentContext == null) {
				throw new IllegalStateException("No Frank EnvironmentContext found. Aborting launch...");
			}
		} catch (Throwable t) {
			servletContext.setAttribute(EXCEPTION_KEY, t);
			APPLICATION_LOG.fatal("Frank EnvironmentContext failed to initialize. Aborting launch...", t);
			throw t; //If the IBIS WebApplicationInitializer can't be found or initialized, throw the exception
		}

		IbisContext ibisContext = new IbisContext();
		ibisContext.setParentContext(parentContext);
		ibisContext.init();

		// save the IbisContext in the ServletContext
		servletContext.setAttribute(CONTEXT_KEY, ibisContext);
		log.debug("stored IbisContext [{}][{}] in ServletContext under key [{}]", ClassUtils.nameOf(ibisContext), ibisContext, CONTEXT_KEY);
		APPLICATION_LOG.info("Initialized {}", ClassUtils.classNameOf(this));
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

	private static class ContextCloseEventListener implements ServletContextListener {

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			// We don't need to initialize anything, just listen to the close event.
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			ServletContext servletContext = sce.getServletContext();
			IbisContext ibisContext = (IbisContext)servletContext.getAttribute(CONTEXT_KEY);
			if(ibisContext != null) {
				APPLICATION_LOG.info("Shutting down {}", ClassUtils.classNameOf(ibisContext));
				ibisContext.close();
			}

			servletContext.removeAttribute(CONTEXT_KEY);
			servletContext.removeAttribute(EXCEPTION_KEY);
		}
	}
}
