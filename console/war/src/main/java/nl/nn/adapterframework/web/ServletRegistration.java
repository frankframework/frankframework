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

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.lifecycle.DynamicRegistration.Servlet;
import nl.nn.adapterframework.lifecycle.servlets.SecuritySettings;
import nl.nn.adapterframework.lifecycle.servlets.ServletConfiguration;
import nl.nn.adapterframework.util.SpringUtils;

@Log4j2
public class ServletRegistration extends ServletRegistrationBean<Servlet> implements ApplicationContextAware, InitializingBean {
	private @Setter ApplicationContext applicationContext;
	private @Getter ServletConfiguration servletConfiguration;
	private final Class<?> servletClass;

	public <T extends Servlet> ServletRegistration(Class<T> servletClass) {
		this.servletClass = servletClass;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SecuritySettings.setupDefaultSecuritySettings(applicationContext.getEnvironment());
		Servlet servlet = (Servlet) SpringUtils.createBean(applicationContext, servletClass);
		servletConfiguration = SpringUtils.createBean(applicationContext, ServletConfiguration.class);
		log.info("registering servlet [{}]", servlet::getName);
		servletConfiguration.fromServlet(servlet);

		Map<String, String> initParams = servletConfiguration.getInitParameters();
		for(Map.Entry<String, String> entry : initParams.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			addInitParameter(key, val);
		}
		setName(servletConfiguration.getName());
		addUrlMappings(servletConfiguration.getUrlMapping());
		super.setServlet(servlet);

		log.info("created servlet {} endpoint {}", this::getServletName, this::getUrlMappings);
	}

	private void addUrlMappings(List<String> urlMapping) {
		urlMapping.stream().forEach(this::addUrlMappings);
	}
}
