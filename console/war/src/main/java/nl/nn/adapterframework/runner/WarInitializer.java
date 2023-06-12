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

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;

/**
 * Spring Boot entrypoint when running as a normal WAR application.
 * 
 * @author Niels Meijer
 */
public class WarInitializer extends SpringBootServletInitializer {

	@Configuration
	public static class WarConfiguration {
		// NO OP required for Spring Boot. Used when running an Annotation Based config, which we are not, see setSources(...) in run(SpringApplication).
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		builder.sources(WarConfiguration.class);
		return super.configure(builder);
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		Set<String> set = new HashSet<>();
		set.add("FrankConsoleContext.xml");
		application.setSources(set);

		return super.run(application);
	}
}
