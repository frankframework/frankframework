/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.console.configuration;

import jakarta.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.console.filters.CacheControlFilter;
import org.frankframework.console.filters.CorsFilter;
import org.frankframework.console.filters.CspFilter;
import org.frankframework.console.filters.CsrfCookieFilter;
import org.frankframework.console.filters.SecurityLogFilter;
import org.frankframework.console.filters.WeakShallowEtagHeaderFilter;
import org.frankframework.security.config.ServletRegistration;
import org.frankframework.util.SpringUtils;

/**
 * Configuration class (without annotation so it's not picked up automatically) to register all Filters.
 */
@Configuration
public class RegisterServletFilters implements ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;

	private enum DynamicFilters {
		CORS_FILTER(CorsFilter.class, "backendServletBean"),
		CACHE_CONTROL_FILTER(CacheControlFilter.class, "backendServletBean"),
		ETAG_FILTER(WeakShallowEtagHeaderFilter.class, "backendServletBean"),
		SECURITY_LOG_FILTER(SecurityLogFilter.class, "backendServletBean"),
		CSP_FILTER(CspFilter.class, "frontendServletBean"),
		CSRF_COOKIE_FILTER(CsrfCookieFilter.class, "frontendServletBean");

		private final @Getter Class<? extends Filter> filterClass;
		private final @Getter String servletBeanName;
		DynamicFilters(Class<? extends Filter> clazz, String servletBeanName) {
			this.filterClass = clazz;
			this.servletBeanName = servletBeanName;
		}
	}

	@Bean
	public FilterRegistrationBean<Filter> createCorsFilter() {
		return createFilter(DynamicFilters.CORS_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createCSPFilter() {
		return createFilter(DynamicFilters.CSP_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createCacheControlFilter() {
		return createFilter(DynamicFilters.CACHE_CONTROL_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createWeakShallowEtagFilter() {
		return createFilter(DynamicFilters.ETAG_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createCsrfCookieFilter() {
		return createFilter(DynamicFilters.CSRF_COOKIE_FILTER);
	}

	@Bean
	public FilterRegistrationBean<Filter> createSecurityLogFilter() {
		return createFilter(DynamicFilters.SECURITY_LOG_FILTER);
	}

	private FilterRegistrationBean<Filter> createFilter(DynamicFilters df) {
		Class<? extends Filter> filter = df.getFilterClass();

		Filter filterInstance = SpringUtils.createBean(applicationContext, filter);
		ServletRegistrationBean<?> servletInstance = applicationContext.getBean(df.getServletBeanName(), ServletRegistration.class);
		return new FilterRegistrationBean<>(filterInstance, servletInstance);
	}
}
