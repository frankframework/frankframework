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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.HttpRequestHandlerServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import lombok.Setter;
import nl.nn.adapterframework.management.web.ServletDispatcher;
import nl.nn.adapterframework.util.SpringUtils;

public class CreateApiEndpointBean implements ApplicationContextAware {
	private final Logger log = LogManager.getLogger(CreateApiEndpointBean.class);
	private static final String WELCOME_FILE = "/index.html";

	private @Setter ApplicationContext applicationContext;
	//TODO scan for components instead of hardcoded ServletDispatcher
	

	@Bean
	public ServletRegistrationBean<ServletDispatcher> createBackendServletBean() {
		ServletDispatcher servlet = SpringUtils.createBean(applicationContext, ServletDispatcher.class);
		log.info("registering servlet [{}]", servlet::getName);

		ServletRegistrationBean<ServletDispatcher> bean = new ServletRegistrationBean<>(servlet);
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

	@Bean
	public ResourceHttpRequestHandler resourceHttpRequestHandler() {
		ResourceHttpRequestHandler requestHandler = new ResourceHttpRequestHandler() {
			@Override
			protected Resource getResource(HttpServletRequest request) throws IOException {
				String path = request.getPathInfo();
				if(StringUtils.isBlank(path) || "/".equals(path)) path = WELCOME_FILE;
				request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);

				return super.getResource(request);
			}
		};
		SpringUtils.autowireByName(applicationContext, requestHandler);
		requestHandler.setLocationValues(Arrays.asList("classpath:/META-INF/resources/iaf/gui/"));
		return requestHandler;
	}

	@Bean
	public ServletRegistrationBean<HttpRequestHandlerServlet> createFrontendServletBean() {
		HttpRequestHandlerServlet servlet = new HttpRequestHandlerServlet();

		ServletRegistrationBean<HttpRequestHandlerServlet> bean = new ServletRegistrationBean<>(servlet);

		bean.setName("resourceHttpRequestHandler");
		bean.addUrlMappings("/*");

		log.info("created IAF GUI servlet endpoint {}", bean::getUrlMappings);

		return bean;
	}

	@Bean
	public SimpleUrlHandlerMapping sampleServletMapping() {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(Integer.MAX_VALUE - 2);
		Properties urlProperties = new Properties();
		urlProperties.put("/**", "resourceHttpRequestHandler");
		mapping.setMappings(urlProperties);
		return mapping;
	}
}
