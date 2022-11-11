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
package nl.nn.adapterframework.lifecycle;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;


/**
 * Programmatic configuration of the spring security configuration: webSecurityConfig.xml
 * 
 * <pre><code>
 * 
 * <http use-expressions="true" realm="Frank" authentication-manager-ref="authenticationManager" entry-point-ref="403EntryPoint" pattern="/**">
 * <security:csrf disabled="true" />
 * <security:headers>
 * 	<security:frame-options policy="SAMEORIGIN" />
 * 	<security:content-type-options disabled="true" />
 * </security:headers>
 * <security:custom-filter position="PRE_AUTH_FILTER" ref="jeePreAuthenticatedFilter" />
 * <security:logout />
 * </http>
 * 
 * <authentication-manager alias="authenticationManager">
 * 	<security:authentication-provider ref="j2eeAuthenticationProvider" />
 * </authentication-manager>
 * 
 * </code></pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false)
public class HttpSecurityConfigurer {

	private UserDetailsManager test() {
		UserDetails user = User.withDefaultPasswordEncoder()
				.username("user")
				.password("password")
				.roles("IbisTester")
				.build();

		return new InMemoryUserDetailsManager(user);
	}
}
