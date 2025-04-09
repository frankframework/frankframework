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
package org.frankframework.ladybug.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.Setter;

import org.frankframework.lifecycle.servlets.AuthenticatorUtils;
import org.frankframework.lifecycle.servlets.IAuthenticator;
import org.frankframework.util.ClassUtils;

/**
 * Adds the {@link IAuthenticator}, so the {@link LadybugSecurityChainConfigurer}
 * can use it to secure the ladybug in a traditional WAR deployment.
 */
@Configuration
public class LadybugSecurityConfiguration implements ApplicationContextAware, EnvironmentAware {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");

	private @Setter ApplicationContext applicationContext;
	private @Setter Environment environment;

	private static final String STANDALONE_PROPERTY_PREFIX = "application.security.testtool.authentication.";
	private static final String CONSOLE_PROPERTY_PREFIX = "application.security.console.authentication.";

	@Bean
	public IAuthenticator ladybugAuthenticator() {
		final IAuthenticator authenticator;
		if(StringUtils.isNotBlank(environment.getProperty(STANDALONE_PROPERTY_PREFIX+"type"))) {
			authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, STANDALONE_PROPERTY_PREFIX);
		} else {
			authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, CONSOLE_PROPERTY_PREFIX);
		}

		APPLICATION_LOG.info("Created Ladybug TestTool Authenticator {}", ClassUtils.classNameOf(authenticator));
		return authenticator;
	}

}
