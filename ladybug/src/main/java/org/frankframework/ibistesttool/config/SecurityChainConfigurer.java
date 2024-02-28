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
package org.frankframework.ibistesttool.config;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.servlets.AuthenticationType;
import org.frankframework.lifecycle.servlets.IAuthenticator;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;

@Configuration
@EnableWebSecurity //Enables Spring Security (classpath)
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) //Enables JSR 250 (JAX-RS) annotations
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityChainConfigurer implements ApplicationContextAware, EnvironmentAware {
	private @Setter ApplicationContext applicationContext;
	private @Setter Environment environment;

	private IAuthenticator createAuthenticator() {
		String properyPrefix = "application.security.testtool.authentication.";
		String type = environment.getProperty(properyPrefix+"type", "NONE");
		AuthenticationType auth = null;
		try {
			auth = EnumUtils.parse(AuthenticationType.class, type);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("invalid authenticator type", e);
		}
		Class<? extends IAuthenticator> clazz = auth.getAuthenticator();
		IAuthenticator authenticator = SpringUtils.createBean(applicationContext, clazz);

		for(Method method: clazz.getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			String setter = StringUtil.lcFirst(method.getName().substring(3));
			String value = environment.getProperty(properyPrefix+setter);
			if(StringUtils.isEmpty(value))
				continue;

			ClassUtils.invokeSetter(authenticator, method, value);
		}

		return authenticator;
	}

	@Bean
	public SecurityFilterChain configureChain(HttpSecurity http) {
		IAuthenticator authenticator = createAuthenticator();


		authenticator.registerServlet(createServletConfig("backendServletBean"));
		authenticator.registerServlet(createServletConfig("frontendServletBean"));
		authenticator.registerServlet(createServletConfig("testtoolServletBean"));

		return authenticator.configureHttpSecurity(http);
	}

	private ServletConfiguration createServletConfig(String servletBeanName) {
		ServletRegistrationBean<?> bean = applicationContext.getBean(servletBeanName, ServletRegistrationBean.class);
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext, ServletConfiguration.class);

		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setUrlMapping(bean.getUrlMappings().stream().collect(Collectors.joining(",")));

		return servletConfiguration;
	}
}
