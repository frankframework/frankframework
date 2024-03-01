/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.ibistesttool.config;

import org.frankframework.ibistesttool.web.TesttoolServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.nn.testtool.web.ApiServlet;
import nl.nn.testtool.web.FrontendServlet;

@Configuration
public class LadybugConfiguration {

	@Bean
	public ServletRegistrationBean<ApiServlet> backendServletBean() {
		ServletRegistrationBean<ApiServlet> registrationBean = new ServletRegistrationBean<>();
		ApiServlet servlet = new ApiServlet();
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/iaf" + ApiServlet.getDefaultMapping());
		registrationBean.setInitParameters(ApiServlet.getDefaultInitParameters());
		registrationBean.setName("Ladybug-" + servlet.getClass().getSimpleName());
		registrationBean.setLoadOnStartup(0);

		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<FrontendServlet> frontendServletBean() {
		ServletRegistrationBean<FrontendServlet> registrationBean = new ServletRegistrationBean<>();
		FrontendServlet servlet = new FrontendServlet();
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/iaf" + FrontendServlet.getDefaultMapping());
		registrationBean.setName("Ladybug-" + servlet.getClass().getSimpleName());
		registrationBean.setLoadOnStartup(0);

		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<TesttoolServlet> testtoolServletBean() {
		ServletRegistrationBean<TesttoolServlet> registrationBean = new ServletRegistrationBean<>();
		TesttoolServlet servlet = new TesttoolServlet();
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/iaf/testtool");
		registrationBean.setName("TestTool");
		registrationBean.setLoadOnStartup(0);

		return registrationBean;
	}
}
