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
package org.frankframework.console.runner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tomcat.websocket.server.WsContextListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletException;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
public class ConsoleStandaloneInitializer {

	/** Required to enable the use of WebSockets when starting as (Spring)Boot-able application. */
	public static class WsSciWrapper implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			WsContextListener sc = new WsContextListener();
			sc.contextInitialized(new ServletContextEvent(servletContext));
		}
	}

	// Should start a XmlServletWebServerApplicationContext.
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication();
		app.setWebApplicationType(WebApplicationType.SERVLET);
		Set<String> set = new HashSet<>();
		set.add("SpringBootContext.xml");
		app.setSources(set);
		app.addPrimarySources(List.of(WsSciWrapper.class));
		app.run(args);
	}

}
