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
package org.frankframework.console.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import lombok.Setter;

import org.frankframework.lifecycle.servlets.AuthenticatorUtils;
import org.frankframework.lifecycle.servlets.IAuthenticator;
import org.frankframework.lifecycle.servlets.SpaCsrfTokenRequestHandler;
import org.frankframework.util.ClassUtils;

@Configuration
@EnableWebSecurity // Enables Spring Security
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) // Enables JSR 250 (JAX-RS) annotations
@Order(Ordered.HIGHEST_PRECEDENCE+100)
public class SecurityChainConfigurer implements ApplicationContextAware, EnvironmentAware {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");
	private @Setter ApplicationContext applicationContext;
	private boolean csrfEnabled;
	private String csrfCookiePath;
	private boolean corsEnabled;

	@Override
	public void setEnvironment(Environment env) {
		csrfEnabled = env.getProperty("csrf.enabled", boolean.class, true);
		csrfCookiePath = env.getProperty("csrf.cookie.path", String.class);
		corsEnabled = env.getProperty("cors.enforced", boolean.class, false);
	}

	private SecurityFilterChain configureHttpSecurity(IAuthenticator authenticator, HttpSecurity http) throws Exception {
		// Apply defaults to disable bloated filters, see DefaultSecurityFilterChain.getFilters for the actual list.
		http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)); // Allow same origin iframe request
		if (csrfEnabled) {
			// HttpOnly needs to be false for Angular to read it
			CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
			if(!StringUtils.isEmpty(csrfCookiePath)) {
				csrfTokenRepository.setCookiePath(csrfCookiePath);
			}

			http.csrf(csrf -> csrf
					.csrfTokenRepository(csrfTokenRepository)
					.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
			);
		} else {
			http.csrf(CsrfConfigurer::disable);
		}
		http.formLogin(FormLoginConfigurer::disable); // Disable the form login filter

		if(!corsEnabled) {
			http.cors(CorsConfigurer::disable);
		}

		// logout automatically sets CookieClearingLogoutHandler, CsrfLogoutHandler and SecurityContextLogoutHandler.
		http.logout(t -> t.logoutRequestMatcher(this::requestMatcher).logoutSuccessHandler(new RedirectToServletRoot()));

		return authenticator.configureHttpSecurity(http);
	}

	// Match when the client matches '<servlet-path>/logout'.
	private boolean requestMatcher(HttpServletRequest request) {
		return ("GET".equals(request.getMethod()) && "/logout".equals(request.getPathInfo()));
	}

	private static class RedirectToServletRoot implements LogoutSuccessHandler {
		// force a 401 status to clear any www-authenticate cache.
		@Override
		public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setHeader("Location", determineTargetUrl(request));
		}

		// redirect the client to the servlet root
		private String determineTargetUrl(HttpServletRequest request) {
			String path = request.getServletPath();
			if (!path.endsWith("/")) path += "/";
			return path;
		}
	}

	@Bean
	public SecurityFilterChain createConsoleSecurityChain(HttpSecurity http) throws Exception {
		String properyPrefix = "application.security.console.authentication.";
		IAuthenticator authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, properyPrefix);
		APPLICATION_LOG.info("Securing Frank!Framework Console using {}", ClassUtils.classNameOf(authenticator));

		authenticator.registerServlet(applicationContext.getBean("backendServletBean", ServletRegistration.class).getServletConfiguration());
		authenticator.registerServlet(applicationContext.getBean("frontendServletBean", ServletRegistration.class).getServletConfiguration());

		return configureHttpSecurity(authenticator, http);
	}
}
