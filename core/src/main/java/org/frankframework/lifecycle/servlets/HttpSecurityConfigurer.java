/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.lifecycle.servlets;

import java.util.Map;
import java.util.Map.Entry;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.WebApplicationContext;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.lifecycle.ServletManager;

/**
 * The default SecurityFilterChain will be added by {@link SecurityFilterChainConfigurer}.
 * All {@link SecurityFilterChain} are added to the root {@link WebApplicationContext} which is stored under
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}.
 * 
 * In newer versions of Spring Boot the boot context overwrites this key, using the 'old' context as a parent.
 * By not unregistering the {@link SecurityFilterChain} they can be picked up by the new context (which may
 * add their own filters). This indirectly means that the default 'FilterChainProxy' (which collects all 
 * SecurityFilters) will not NOT be used. Instead a new one, created in the sub-context, will be used.
 * 
 * @author Niels Meijer
 */
@Log4j2
@Order(Ordered.LOWEST_PRECEDENCE)
@Configuration
@EnableWebSecurity // Enables Spring Security (classpath)
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) // Enables JSR 250 (JAX-RS) annotations
public class HttpSecurityConfigurer implements WebSecurityConfigurer<WebSecurity>, InitializingBean {

	private @Setter @Autowired ServletManager servletManager;
	private @Setter @Autowired BeanFactory beanFactory;
	private @Setter @Autowired ServletContext servletContext;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(servletManager == null) {
			throw new IllegalStateException("unable to initialize Spring Security, ServletManager not set");
		}

		if(!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalStateException("beanFactory not set or not instanceof ConfigurableListableBeanFactory");
		}
	}

	@Override
	public void init(WebSecurity webSecurity) {
		if(servletContext != null) servletContext.log("Enabling Spring WebSecurity");
		servletManager.startAuthenticators();
	}

	@Override
	public void configure(WebSecurity webSecurity) throws Exception {
		ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) this.beanFactory;
		Map<String, SecurityFilterChain> filters = factory.getBeansOfType(SecurityFilterChain.class);
		webSecurity.debug(log.isTraceEnabled());

		for(Entry<String, SecurityFilterChain> chain : filters.entrySet()) {
			log.info("adding SecurityFilterChain [{}] to WebSecurity", chain::getKey);
			webSecurity.addSecurityFilterChainBuilder(chain::getValue);
		}
	}
}
