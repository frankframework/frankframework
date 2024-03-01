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
package org.frankframework.ibistesttool.runner;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.logging.log4j.Logger;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Spring Boot entrypoint when running as a normal WAR application.
 *
 * @author Niels Meijer
 */
@Order(500)
public class WarInitializer extends SpringBootServletInitializer {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	@Configuration
	public static class WarConfiguration {
		// NO OP required for Spring Boot. Used when running an Annotation Based config, which we are not, see setSources(...) in run(SpringApplication).
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		builder.sources(WarConfiguration.class);
		builder.bannerMode(Mode.OFF);
		return super.configure(builder);
	}

	// Purely here for some debug info
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		APPLICATION_LOG.debug("Starting Ladybug TestTool");
		final long start = System.currentTimeMillis();

		try {
			super.onStartup(servletContext);
			APPLICATION_LOG.fatal("Created Ladybug TestTool in {} ms", () -> (System.currentTimeMillis() - start));
		} catch (Exception e) {
			APPLICATION_LOG.fatal("Unable to start Ladybug TestTool", e);
			throw e;
		}
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		Set<String> set = new HashSet<>();
		set.add(getConfigFile());
		application.setSources(set);
		application.setDefaultProperties(AppConstants.getInstance());
		return super.run(application);
	}

	private String getConfigFile() {
		String file = AppConstants.getInstance().getProperty("ibistesttool.springConfigFile", "springIbisTestTool.xml");
		ClassLoader classLoader = this.getClass().getClassLoader();
		URL fileURL = classLoader.getResource(file);
		if(fileURL == null) {
			APPLICATION_LOG.warn("unable to locate TestTool configuration [{}] using classloader [{}]", file, classLoader);
			return null;
		} else {
			if(file.indexOf(":") == -1) {
				file = ResourceUtils.CLASSPATH_URL_PREFIX+file;
			}

			APPLICATION_LOG.info("loading TestTool configuration [{}]", file);
			return file;
		}
	}
}
