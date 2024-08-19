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
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.WebApplicationContext;

/**
 * Spring Boot entrypoint when running as a normal WAR application.
 *
 * @author Niels Meijer
 */
@Order(500)
public class ConsoleWarInitializer extends SpringBootServletInitializer {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");

	@Configuration
	public static class WarConfiguration {
		// NO OP required for Spring Boot. Used when running an Annotation Based config, which we are not, see setSources(...) in run(SpringApplication).
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		builder.sources(WarConfiguration.class);
		setRegisterErrorPageFilter(false);
		builder.bannerMode(Mode.OFF);
		return super.configure(builder);
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		APPLICATION_LOG.debug("Starting Frank!Framework Console");
		Set<String> set = new HashSet<>();
		set.add("FrankConsoleContext.xml");
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setSources(set);

		return super.run(application);
	}
}
