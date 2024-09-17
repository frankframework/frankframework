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
package org.frankframework.ladybug;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.context.ServletContextAware;

import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.servlets.AuthenticationType;
import org.frankframework.lifecycle.servlets.IAuthenticator;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;

/**
 * Enables WebSecurity, still depends on the existence of the {@link AbstractSecurityWebApplicationInitializer#DEFAULT_FILTER_NAME}.
 * Spring Boot's auto creation of the Filter has been disabled (see springIbisTestTool.xml) because the {@link Filter} may not exist twice.
 * 
 * When running standalone this Filter will need to be added manually, by either a bean definition or by using the {@link AbstractSecurityWebApplicationInitializer}.
 * 
 * <pre>{@code
 * public FilterRegistrationBean<DelegatingFilterProxy> securityFilterChainRegistration() {
 * 	   DelegatingFilterProxy delegatingFilterProxy = new DelegatingFilterProxy();
 * 	   delegatingFilterProxy.setTargetBeanName(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
 * 	   FilterRegistrationBean<DelegatingFilterProxy> registrationBean = new FilterRegistrationBean<>(delegatingFilterProxy);
 * 	   registrationBean.addUrlPatterns("/*");
 *
 * 	   return registrationBean;
 * }
 * }</pre>
 */
@Configuration
@EnableWebSecurity //Enables Spring Security (classpath)
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) //Enables JSR 250 (JAX-RS) annotations
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LadybugSecurityChainConfigurer implements ApplicationContextAware, EnvironmentAware, ServletContextAware {
	private static final Logger log = LoggerFactory.getLogger("ladybug");
	private static final String HTTP_SECURITY_BEAN_NAME = "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity";

	private @Setter ApplicationContext applicationContext;
	private @Setter Environment environment;
	private @Setter ServletContext servletContext;

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
			if(StringUtils.isNotEmpty(value)) {
				ClassUtils.invokeSetter(authenticator, method, value);
			}
		}

		return authenticator;
	}

	@Bean
	public SecurityFilterChain createLadybugSecurityChain(HttpSecurity http) throws Exception {
		return configureChain();
	}

	private SecurityFilterChain configureChain() throws Exception {
		IAuthenticator authenticator = createAuthenticator();

		authenticator.registerServlet(createServletConfig("ladybugApiServletBean"));
		authenticator.registerServlet(createServletConfig("ladybugFrontendServletBean"));
		authenticator.registerServlet(createServletConfig("testtoolServletBean"));

		HttpSecurity httpSecurity = applicationContext.getBean(HTTP_SECURITY_BEAN_NAME, HttpSecurity.class);

		httpSecurity.csrf(CsrfConfigurer::disable); //Disable CSRF, should be configured in the Ladybug
		httpSecurity.formLogin(FormLoginConfigurer::disable); //Disable the form login filter
		httpSecurity.logout(LogoutConfigurer::disable); //Disable the logout filter
		httpSecurity.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)); //Allow same origin iframe request
		return authenticator.configureHttpSecurity(httpSecurity);
	}

	/**
	 * Create a dummy servletConfig wrapper to determine the default url-mapping and roles, and allow users to overwrite these using properties.
	 */
	private ServletConfiguration createServletConfig(String servletBeanName) {
		ServletRegistrationBean<?> bean = applicationContext.getBean(servletBeanName, ServletRegistrationBean.class);
		ServletConfiguration servletConfiguration = SpringUtils.createBean(applicationContext, ServletConfiguration.class);

		servletConfiguration.setSecurityRoles(DynamicRegistration.ALL_IBIS_USER_ROLES);
		servletConfiguration.setUrlMapping(bean.getUrlMappings().stream().collect(Collectors.joining(",")));

		return servletConfiguration;
	}
}
