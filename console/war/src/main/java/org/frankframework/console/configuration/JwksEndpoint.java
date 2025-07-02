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

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Setter;

import org.frankframework.management.security.JwtKeyGeneratorFactoryBean;
import org.frankframework.util.SpringUtils;

@Configuration
public class JwksEndpoint implements ApplicationContextAware {
	private final Logger log = LogManager.getLogger(JwksEndpoint.class);

	private @Setter ApplicationContext applicationContext;

	@Bean
	public ServletRegistrationBean<JwksServlet> createJkwsEndpoint() {
		JwksServlet servlet = SpringUtils.createBean(applicationContext);
		log.info("registering servlet [{}]", servlet::getName);

		ServletRegistrationBean<JwksServlet> bean = new ServletRegistrationBean<>(servlet);
		bean.setName(servlet.getName());
		bean.addUrlMappings("/iaf/management/jwks");

		log.info("created IAF API servlet endpoint {}", bean::getUrlMappings);

		return bean;
	}

	private static class JwksServlet extends HttpServlet implements ApplicationContextAware {
		private JwtKeyGeneratorFactoryBean keyGeneratorFactory;

		public String getName() {
			return "JwksServlet";
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			resp.getWriter().write(keyGeneratorFactory.getObject().getPublicJwkSet());
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			keyGeneratorFactory = applicationContext.getBean("JwtKeyGeneratorSupplier", JwtKeyGeneratorFactoryBean.class);
		}
	}
}
