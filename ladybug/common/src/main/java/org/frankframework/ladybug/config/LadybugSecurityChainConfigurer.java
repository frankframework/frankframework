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

import jakarta.servlet.Filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import lombok.Setter;

import org.frankframework.lifecycle.servlets.IAuthenticator;
import org.frankframework.security.config.ServletRegistration;
import org.frankframework.util.ClassUtils;

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
 * <p>
 * NB: This configuration has the highest order so the {@link SecurityFilterChain} is used before any other.
 * </p>
 */
@Configuration
@EnableWebSecurity // Enables Spring Security (classpath)
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) // Enables JSR 250 (JAX-RS) annotations
@Order(Ordered.HIGHEST_PRECEDENCE) // Higher then the Frank!Console so this `SecurityFilterChain` is used before the Console `SecurityChainConfigurer`.
public class LadybugSecurityChainConfigurer implements ApplicationContextAware, EnvironmentAware {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");

	private @Setter ApplicationContext applicationContext;
	private @Setter Environment environment;

	@Bean
	public SecurityFilterChain createLadybugSecurityChain(HttpSecurity http, IAuthenticator ladybugAuthenticator) throws Exception {
		APPLICATION_LOG.info("Securing Ladybug TestTool using {}", ClassUtils.classNameOf(ladybugAuthenticator));

		registerServletWhenBeanExists(ladybugAuthenticator, "ladybugApiServletBean");
		registerServletWhenBeanExists(ladybugAuthenticator, "ladybugFrontendServletBean");
		registerServletWhenBeanExists(ladybugAuthenticator, "testtoolServletBean");

		http.csrf(CsrfConfigurer::disable); // Disable CSRF, should be configured in the Ladybug
		http.formLogin(FormLoginConfigurer::disable); // Disable the form login filter
		http.logout(LogoutConfigurer::disable); // Disable the logout filter
		http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)); // Allow same origin iframe request
		return ladybugAuthenticator.configureHttpSecurity(http);
	}

	private void registerServletWhenBeanExists(IAuthenticator ladybugAuthenticator, String servletBeanName) {
		if (applicationContext.containsBean(servletBeanName)) {
			ServletRegistration<?> bean = applicationContext.getBean(servletBeanName, ServletRegistration.class);
			ladybugAuthenticator.registerServlet(bean.getServletConfiguration());
		}
	}
}
