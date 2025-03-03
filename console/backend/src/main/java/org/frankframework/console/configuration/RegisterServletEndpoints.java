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

import jakarta.servlet.MultipartConfigElement;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import org.frankframework.console.ConsoleFrontend;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.servlets.SecuritySettings;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.security.config.ServletRegistration;
import org.frankframework.util.SpringUtils;

@Configuration
public class RegisterServletEndpoints implements ApplicationContextAware {
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		SecuritySettings.setupDefaultSecuritySettings(applicationContext.getEnvironment());
	}

	@Bean
	public ServletRegistration<DispatcherServlet> backendServletBean() {
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext, ServletConfiguration.class);
		servletConfiguration.setName("IAF-API");
		servletConfiguration.setUrlMapping("iaf/api/*");
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
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext, ServletConfiguration.class);
		servletConfiguration.setName("IAF-GUI");
		servletConfiguration.setUrlMapping("iaf/gui/*");
		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setLoadOnStartup(1);
		servletConfiguration.loadProperties();

		return new ServletRegistration<>(ConsoleFrontend.class, servletConfiguration);
	}
}
