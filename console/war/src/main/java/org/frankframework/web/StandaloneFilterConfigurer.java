/*
   Copyright 2023 - 2024 WeAreFrank!

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

import org.frankframework.util.SpringUtils;
import org.frankframework.web.filters.DynamicFilterConfigurer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebListener;

/**
 * Configuration class (without annotation so it's not picked up automatically) to register all Filters.
 * Should only be used in Standalone mode (executable jar) when the {@link DynamicFilterConfigurer}'s {@link WebListener} is used.
 * Else Filters will be configured twice.
 */
public class StandaloneFilterConfigurer {

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

	@Bean
	public FilterRegistrationBean<Filter> createWeakShallowEtagFilter(ApplicationContext ac) {
		return createFilter(ac, DynamicFilterConfigurer.DynamicFilters.ETAG_FILTER);
	}

	private FilterRegistrationBean<Filter> createFilter(ApplicationContext ac, DynamicFilterConfigurer.DynamicFilters df) {
		Class<? extends Filter> filter = df.getFilterClass();
		Filter filterInstance = SpringUtils.createBean(ac, filter);
		FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filterInstance);
		registration.addUrlPatterns(df.getEndpoints());
		return registration;
	}
}
