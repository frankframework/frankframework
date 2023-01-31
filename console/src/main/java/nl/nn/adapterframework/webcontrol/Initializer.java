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

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.XmlServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

//@SpringBootApplication
public class Initializer {
//public class Initializer extends SpringBootServletInitializer {

	public static void main(String[] args) throws Exception {
		SpringApplication app = new SpringApplication() {
			@Override
			protected ConfigurableApplicationContext createApplicationContext() {
				ConfigurableApplicationContext context = super.createApplicationContext();
				System.err.println(context);
//				XmlServletWebServerApplicationContext context = new XmlServletWebServerApplicationContext("SpringRootContext.xml");
				return context;
			}
		};
		app.setWebApplicationType(WebApplicationType.SERVLET);
		Set<String> set = new HashSet<>();
		set.add("SpringRootContext.xml");
//		XmlServletWebServerApplicationContext
		app.setSources(set);
		app.run(args);
//		XmlServletWebServerApplicationContext context = new XmlServletWebServerApplicationContext("SpringRootContext.xml");
//		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("SpringRootContext.xml");
//		context.setDisplayName("");
//		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory("/", 80);
//		ServletWebServerFactory
//		context.getBeanFactory().registerSingleton("ServletWebServerFactory", webServerFactory);
//		ServletWebServerFactoryAutoConfiguration.class);
//		context.refresh();
	}

}
