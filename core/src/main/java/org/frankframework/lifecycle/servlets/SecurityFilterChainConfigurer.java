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
package org.frankframework.lifecycle.servlets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

import jakarta.servlet.ServletContext;

/**
 * Add the SpringSecurity filter to enable authentication.
 * Has a high precedence so it's loaded before the EnvironmentInitializer starts.
 * 
 * Spring Security provides a base class which provides all initialization.
 * 
 * @author Niels Meijer
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityFilterChainConfigurer extends AbstractSecurityWebApplicationInitializer {

	@Override
	public void afterSpringSecurityFilterChain(ServletContext servletContext) {
		servletContext.log("registered SpringSecurityFilter");
	}
}
