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
package org.frankframework.web;

import javax.servlet.Filter;

import org.frankframework.console.ConsoleFrontend;
import org.frankframework.management.web.ServletDispatcher;
import org.frankframework.util.SpringUtils;
import org.frankframework.web.filters.DynamicFilterConfigurer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegisterServletEndpoints {

	@Bean
	public ServletRegistration backendServletBean() {
		return new ServletRegistration(ServletDispatcher.class);
	}

	@Bean
	public ServletRegistration frontendServletBean() {
		ServletRegistration registration = new ServletRegistration(ConsoleFrontend.class);
		registration.addUrlMappings("/*"); //Also host the console on the ROOT
		return registration;
	}

	@Bean
	public FilterRegistrationBean<Filter> createCorsFilter(ApplicationContext ac) {
		return createFilter(ac, DynamicFilterConfigurer.DynamicFilters.CORS_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createCSPFilter(ApplicationContext ac) {
		return createFilter(ac, DynamicFilterConfigurer.DynamicFilters.CSP_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createCacheControlFilter(ApplicationContext ac) {
		return createFilter(ac, DynamicFilterConfigurer.DynamicFilters.CACHE_CONTROL_FILTER);
	}

	private FilterRegistrationBean<Filter> createFilter(ApplicationContext ac, DynamicFilterConfigurer.DynamicFilters df) {
		Class<? extends Filter> filter = df.getFilterClass();
		Filter filterInstance = SpringUtils.createBean(ac, filter);
		FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filterInstance);
		registration.addUrlPatterns(df.getEndpoints());
		return registration;
	}
}
