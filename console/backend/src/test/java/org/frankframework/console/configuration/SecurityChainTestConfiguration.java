package org.frankframework.console.configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;

import org.springframework.beans.BeansException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.servlet.DispatcherServlet;

import org.frankframework.console.ConsoleFrontend;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.servlets.SecuritySettings;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.security.config.ServletRegistration;
import org.frankframework.util.SpringUtils;

public class SecurityChainTestConfiguration implements ApplicationContextAware {
	private ApplicationContext applicationContext;

	/**
	 * For MockMvc testing, we need to register the SpringSecurityFilterChain as a Filter in a Bean manually.
	 * This is used by mockMvc configuration with `SecurityMockMvcConfigurers#springSecurity()`.
	 */
	@Bean
	public FilterRegistrationBean<Filter> getFilterRegistrationBean() {
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
		Filter filter = applicationContext.getBean(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME, Filter.class);
		bean.setFilter(filter);
		return bean;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		SecuritySettings.setupDefaultSecuritySettings(applicationContext.getEnvironment());
	}

	/**
	 * Needed to configure the backend endpoint for the IAF API to /* - we're missing the default /iaf/api/* mapping which is configured
	 * in the RegisterServletEndpoints class on 'normal' startup.
	 *
	 * @see RegisterServletEndpoints
	 */
	@Bean
	public ServletRegistration<DispatcherServlet> backendServletBean() {
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext);
		servletConfiguration.setName("IAF-API");
		servletConfiguration.setUrlMapping("/*");
		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setLoadOnStartup(1);
		servletConfiguration.loadProperties();

		ServletRegistration<DispatcherServlet> servlet = new ServletRegistration<>(DispatcherServlet.class, servletConfiguration);
		servlet.setMultipartConfig(new MultipartConfigElement(""));
		servlet.setAsyncSupported(true);
		return servlet;
	}

	@Bean
	public ServletRegistration<ConsoleFrontend> frontendServletBean() {
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext);
		servletConfiguration.setName("IAF-GUI");
		servletConfiguration.setUrlMapping("iaf/gui/*");
		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setLoadOnStartup(1);
		servletConfiguration.loadProperties();

		return new ServletRegistration<>(ConsoleFrontend.class, servletConfiguration);
	}
}
