/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle.servlets;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.Logger;
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

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.LogUtil;

@Order(Ordered.LOWEST_PRECEDENCE)
@Configuration
@EnableWebSecurity //Enables Spring Security (classpath)
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) //Enables JSR 250 (JAX-RS) annotations
public class HttpSecurityConfigurer implements WebSecurityConfigurer<WebSecurity>, InitializingBean {

	private Logger log = LogUtil.getLogger(this);
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
	public void init(WebSecurity webSecurity) throws Exception {
		if(servletContext != null) servletContext.log("Enabling Spring WebSecurity");
		servletManager.startAuthenticators();
	}

	@Override
	public void configure(WebSecurity webSecurity) throws Exception {
		ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) this.beanFactory;
		Map<String, SecurityFilterChain> filters = factory.getBeansOfType(SecurityFilterChain.class);
		webSecurity.debug(log.isTraceEnabled());

		for(SecurityFilterChain chain : filters.values()) {
			log.info("adding SecurityFilterChain [{}] to WebSecurity", chain);
			webSecurity.addSecurityFilterChainBuilder(() -> chain);
		}
	}
}
