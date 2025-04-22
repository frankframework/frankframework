/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.ladybug.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.nn.testtool.web.ApiServlet;
import nl.nn.testtool.web.FrontendServlet;

import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.servlets.SecuritySettings;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.security.config.ServletRegistration;
import org.frankframework.util.SpringUtils;

@Configuration
public class LadybugServletConfiguration implements ApplicationContextAware {
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		SecuritySettings.setupDefaultSecuritySettings(applicationContext.getEnvironment());
	}

	@Bean
	public ServletRegistration<ApiServlet> ladybugApiServletBean() {
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext);
		servletConfiguration.setUrlMapping("/iaf" + ApiServlet.getDefaultMapping());
		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		ApiServlet.getDefaultInitParameters().forEach(servletConfiguration::addInitParameter);
		servletConfiguration.setName("Ladybug-ApiServlet");
		servletConfiguration.setLoadOnStartup(0);
		servletConfiguration.loadProperties();

		return new ServletRegistration<>(ApiServlet.class, servletConfiguration);
	}

	@Bean
	public ServletRegistration<FrontendServlet> ladybugFrontendServletBean() {
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext);
		servletConfiguration.setUrlMapping("/iaf" + FrontendServlet.getDefaultMapping());
		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setName("Ladybug-FrontendServlet");
		servletConfiguration.setLoadOnStartup(0);
		servletConfiguration.loadProperties();

		return new ServletRegistration<>(FrontendServlet.class, servletConfiguration);
	}

	@Bean
	public ServletRegistration<TesttoolServlet> testtoolServletBean() {
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext);
		servletConfiguration.setUrlMapping("/iaf/testtool");
		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setName("TestTool");
		servletConfiguration.setLoadOnStartup(0);
		servletConfiguration.loadProperties();

		return new ServletRegistration<>(TesttoolServlet.class, servletConfiguration);
	}
}
