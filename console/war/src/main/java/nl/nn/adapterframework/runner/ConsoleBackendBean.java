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
package nl.nn.adapterframework.runner;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.DynamicRegistration.ServletWithParameters;
import nl.nn.adapterframework.management.web.ServletDispatcher;
import nl.nn.adapterframework.util.SpringUtils;

public class ConsoleBackendBean implements ApplicationContextAware {
	private final Logger log = LogManager.getLogger(ConsoleBackendBean.class);

	private @Setter ApplicationContext applicationContext;

	@Bean
	public ServletRegistrationBean<ServletWithParameters> createBackendServletBean() {
		ServletWithParameters servlet = SpringUtils.createBean(applicationContext, ServletDispatcher.class);
		log.info("registering servlet [{}]", servlet::getName);

		ServletRegistrationBean<ServletWithParameters> bean = new ServletRegistrationBean<>(servlet);
		Map<String, String> initParams = servlet.getParameters();
		for(Map.Entry<String, String> entry : initParams.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			bean.addInitParameter(key, val);
		}
		bean.setName(servlet.getName());
		bean.addUrlMappings("/iaf/api/*");

		log.info("created IAF API servlet endpoint {}", bean::getUrlMappings);

		return bean;
	}
}
