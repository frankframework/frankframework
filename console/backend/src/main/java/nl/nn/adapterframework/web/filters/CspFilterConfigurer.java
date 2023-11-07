/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.web.filters;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import nl.nn.adapterframework.util.SpringUtils;

@WebListener
public class CspFilterConfigurer implements ServletContextListener {
	private Logger log = LogManager.getLogger(this);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();

		try {
			Dynamic filter = createFilter(context);

			String[] urlMapping = new String[] {"/iaf/gui/*"};
			filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlMapping);
		} catch (Exception e) {
			log.fatal("unable to create CSP filter", e);
			context.log("unable to create CSP filter", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// nothing to destroy
	}

	private Dynamic createFilter(ServletContext context) {
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(context);
		if(wac != null) {
			log.info("creating CspFilter through Application Context [{}]", wac::getDisplayName);
			CspFilter filterInstance = SpringUtils.createBean(wac, CspFilter.class);
			return context.addFilter("CspFilter", filterInstance);
		} else {
			log.info("creating CspFilter without Application Context");
			return context.addFilter("CspFilter", CspFilter.class);
		}
	}
}
