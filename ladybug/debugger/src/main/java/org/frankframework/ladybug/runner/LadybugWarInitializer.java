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
package org.frankframework.ladybug.runner;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;

import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * Spring Boot entrypoint when running as a normal WAR application.
 *
 * Has an order of 500 because it should start after the EnvironmentContext and before the ApplicationContext.
 * 
 * @author Niels Meijer
 */
@Order(500)
public class LadybugWarInitializer extends SpringBootServletInitializer {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

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

	// Purely here for some debug info
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		APPLICATION_LOG.debug("Starting Ladybug TestTool");
		final long start = System.currentTimeMillis();

		try {
			super.onStartup(servletContext);
			APPLICATION_LOG.fatal("Started Ladybug TestTool in {} ms", () -> (System.currentTimeMillis() - start));
		} catch (Exception e) {
			APPLICATION_LOG.fatal("Unable to start Ladybug TestTool", e);
			throw e;
		}
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		AppConstants properties = AppConstants.getInstance();
		String file = properties.getProperty("ibistesttool.springConfigFile", "springIbisTestTool.xml");

		List<String> profiles = determineStorageProfiles(properties);
		application.setAdditionalProfiles(profiles.toArray(new String[0]));

		// Only allow this (by default) for this context, application.properties may be overwritten.
		application.setAllowBeanDefinitionOverriding(true);
		Set<String> set = new HashSet<>();
		set.add(getConfigFile(file));
		application.setSources(set);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setDefaultProperties(properties);
		return super.run(application);
	}

	private List<String> determineStorageProfiles(AppConstants properties) {
		String dataSourceName = properties.getProperty("ladybug.jdbc.datasource");

		List<String> profiles = new ArrayList<>(2);
		if(StringUtils.isBlank(dataSourceName)) {
			profiles.add("ladybug-file");
		} else {
			profiles.add("ladybug-database");
		}
		if(dtapIsLoc(properties)) {
			profiles.add("ladybug-xml");
		}
		APPLICATION_LOG.debug("Using Ladybug profiles {}", profiles);
		return profiles;
	}

	/**
	 * When using a local environment the Test-tab storage location can/should be read from the project.
	 * This allows users to store their test results in Git for easy compares (rerun).
	 */
	private boolean dtapIsLoc(AppConstants properties) {
		String dtapStage = properties.getProperty("dtap.stage", "").toLowerCase();
		return "loc".equals(dtapStage) || "xxx".equals(dtapStage) || dtapStage.isEmpty();
	}

	private String getConfigFile(String file) {
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
