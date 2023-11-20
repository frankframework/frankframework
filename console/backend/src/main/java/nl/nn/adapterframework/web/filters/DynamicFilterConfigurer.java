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
import javax.servlet.Filter;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.Getter;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Configures all {@link Dynamic} Filters through Spring and registers them in the ServletContext.
 */
@WebListener
public class DynamicFilterConfigurer implements ServletContextListener {
	private Logger log = LogManager.getLogger(this);

	public enum DynamicFilters {
		CORS_FILTER(CorsFilter.class, "/iaf/api/*"),
		CACHE_CONTROL_FILTER(CacheControlFilter.class, "/iaf/api/*"),
		CSP_FILTER(CspFilter.class, "/iaf/gui/*");

		private @Getter Class<? extends Filter> filterClass;
		private @Getter String endpoints;
		DynamicFilters(Class<? extends Filter> clazz, String endpoints) {
			this.filterClass = clazz;
			this.endpoints = endpoints;
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();

		for(DynamicFilters dynamicFilter : DynamicFilters.values()) {
			try {
				Dynamic filter = createFilter(context, dynamicFilter.getFilterClass());
				String[] urlMapping = new String[] { dynamicFilter.getEndpoints() };

				filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlMapping);
			} catch (Exception e) {
				log.fatal("unable to create [{}]", dynamicFilter, e);
				context.log("unable to create ["+dynamicFilter+"]", e);
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// nothing to destroy
	}

	private Dynamic createFilter(ServletContext context, Class<? extends Filter> filter) {
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(context);
		if(wac != null) {
			log.info("creating [{}] through Application Context [{}]", filter::getSimpleName, wac::getDisplayName);
			Filter filterInstance = SpringUtils.createBean(wac, filter);
			return context.addFilter(filter.getSimpleName(), filterInstance);
		} else {
			log.info("creating [{}] without Application Context", filter::getSimpleName);
			return context.addFilter(filter.getSimpleName(), filter);
		}
	}
}
