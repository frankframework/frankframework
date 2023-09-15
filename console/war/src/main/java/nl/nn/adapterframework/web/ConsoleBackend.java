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
package nl.nn.adapterframework.web;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.DynamicRegistration.ServletWithParameters;
import nl.nn.adapterframework.lifecycle.servlets.ServletConfiguration;
import nl.nn.adapterframework.management.web.ServletDispatcher;
import nl.nn.adapterframework.util.SpringUtils;

@Configuration
public class ConsoleBackend implements ApplicationContextAware {
	private final Logger log = LogManager.getLogger(ConsoleBackend.class);

	private @Setter ApplicationContext applicationContext;

	@Bean
	public ServletRegistration backendServletBean() {
//		ServletRegistration servletRegistration = SpringUtils.createBean(applicationContext, ServletRegistration.class);
//		servletRegistration.setServlet(ServletDispatcher.class);
//		return servletRegistration;
		return new ServletRegistration(ServletDispatcher.class);
	}
}
