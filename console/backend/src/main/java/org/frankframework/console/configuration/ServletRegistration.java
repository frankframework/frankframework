/*
   Copyright 2023-2024 WeAreFrank!

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

import java.util.List;
import java.util.Map;

import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.util.SpringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import jakarta.servlet.http.HttpServlet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServletRegistration<T extends HttpServlet> extends ServletRegistrationBean<T> implements ApplicationContextAware, InitializingBean {
	private @Setter ApplicationContext applicationContext;
	private final @Getter ServletConfiguration servletConfiguration;
	private final Class<T> servletClass;

	public ServletRegistration(Class<T> servletClass, ServletConfiguration config) {
		this.servletClass = servletClass;
		this.servletConfiguration = config;
	}

	@Override
	public void afterPropertiesSet() {
		T servlet = SpringUtils.createBean(applicationContext, servletClass);
		log.info("registering servlet [{}]", servletConfiguration::getName);

		Map<String, String> initParams = servletConfiguration.getInitParameters();
		for(Map.Entry<String, String> entry : initParams.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			addInitParameter(key, val);
		}
		setName(servletConfiguration.getName());
		addUrlMappings(servletConfiguration.getUrlMapping());
		setEnabled(servletConfiguration.isEnabled());
		setLoadOnStartup(servletConfiguration.getLoadOnStartup());
		super.setServlet(servlet);

		log.info("created servlet {} endpoint {}", this::getServletName, this::getUrlMappings);
	}

	private void addUrlMappings(List<String> urlMapping) {
		String[] mapping = urlMapping.stream()
			.filter(e -> !e.startsWith("!"))
			.toArray(String[]::new);
		addUrlMappings(mapping);
	}
}
