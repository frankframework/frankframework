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
package nl.nn.adapterframework.webcontrol;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.WebApplicationInitializer;

import nl.nn.adapterframework.management.web.ServletDispatcher;

@WebListener
public class WebAppInitializer implements ServletContextListener {

	@Bean
	public ServletRegistrationBean<ServletDispatcher> createBean() {
		System.err.println("on ServletRegistrationBean");
		ServletDispatcher servlet = new ServletDispatcher();
		ServletRegistrationBean<ServletDispatcher> bean = new ServletRegistrationBean<>(servlet);
		Map<String, String> initParams = servlet.getParameters();
		for(Map.Entry<String, String> entry : initParams.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			bean.addInitParameter(key, val);
		}
		bean.setName(servlet.getName());
		return bean;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		System.err.println("contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.err.println("contextDestroyed");
	}
}
