/*
   Copyright 2022 - 2023 WeAreFrank!

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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;

/*
	<authentication-manager alias="inMemoryAuthManager">
		<security:authentication-provider>
			<security:user-service>
				<security:user name="IbisTester123" password="IbisTester" authorities="IbisTester"/>
			</security:user-service>
			<security:password-encoder ref="passwordEncoder" />
		</security:authentication-provider>
	</authentication-manager>
	<beans:bean id="passwordEncoder" class = "org.springframework.security.crypto.password.NoOpPasswordEncoder" factory-method = "getInstance"/>
 */
public class InMemoryAuthenticator extends ServletAuthenticatorBase {
	private @Setter String username = null;
	private @Setter String password = null;

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http.httpBasic().realmName("Frank"); //BasicAuthenticationEntryPoint
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.authorizeHttpRequests().anyRequest().authenticated();

		UserDetails user = User.builder()
				.username(username)
				.password("{noop}"+password)
				.roles(getSecurityRoles().toArray(new String[0]))
				.build();

		InMemoryUserDetailsManager udm = new InMemoryUserDetailsManager(user);
		http.userDetailsService(udm);

		return http.build();
	}

}
