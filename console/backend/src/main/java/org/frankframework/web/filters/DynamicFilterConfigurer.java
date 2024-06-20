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
package org.frankframework.web.filters;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.util.SpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Configures all {@link Dynamic} Filters through Spring and registers them in the ServletContext.
 */
@WebListener
public class DynamicFilterConfigurer implements ServletContextListener {
	private final Logger log = LogManager.getLogger(this);

	public enum DynamicFilters {
		CORS_FILTER(CorsFilter.class, "/iaf/api/*"),
		CACHE_CONTROL_FILTER(CacheControlFilter.class, "/iaf/api/*"),
		CSP_FILTER(CspFilter.class, "/iaf/gui/*"),
		ETAG_FILTER(WeakShallowEtagHeaderFilter.class, "/iaf/api/*"),
		CSRF_COOKIE_FILTER(CsrfCookieFilter.class, "/iaf/gui/*");

		private final @Getter Class<? extends Filter> filterClass;
		private final @Getter String endpoints;
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
