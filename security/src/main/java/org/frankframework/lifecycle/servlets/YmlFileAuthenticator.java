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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.yaml.snakeyaml.Yaml;

import lombok.Setter;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;

/**
 * users [array] Set localUsers who can log in on the Frank!
 * users.username [string] Set the username of the user
 * users.password [string] Set the password of the user
 * users.roles [array] Set the roles of the user. Options: `IbisTester`, `IbisDataAdmin`, `IbisAdmin`, `IbisWebService`, `IbisObserver`
 * e.g.
 * <pre>{@code
 * users:
 *   - username: Tester
 *     password: ChangeMe!
 *     roles:
 *       - IbisTester
 *       - IbisObserver
 * }</pre>
 */
public class YmlFileAuthenticator extends AbstractServletAuthenticator {

	private @Setter String file = "localUsers.yml";
	private URL ymlFileURL = null;

	private void configure() throws FileNotFoundException {
		ymlFileURL = ClassUtils.getResourceURL(file);
		if (ymlFileURL == null) {
			throw new FileNotFoundException("unable to find yml file ["+file+"]");
		}
		log.info("found rolemapping file [{}]", ymlFileURL);
	}

	public static class LocalUsers {
		private @Setter List<LocalUser> users;

		public List<UserDetails> getUserDetails() {
			return users.stream().map(LocalUser::toUserDetails).toList();
		}
	}

	public static class LocalUser {
		private @Setter String username;
		private @Setter String password;
		private List<String> roles;

		@SuppressWarnings("unchecked")
		public void setRoles(Object role) {
			if (role instanceof String roleString) {
				roles = new ArrayList<>();
				roles.add(roleString);
			} else if (role instanceof List<?> roleList) {
				roles = (List<String>) roleList;
			}
		}

		public UserDetails toUserDetails() {
			return User.builder()
					.username(username)
					.password("{noop}"+password)
					.roles(roles.toArray(new String[0]))
					.build();
		}
	}

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		configure();
		http.httpBasic(basic -> basic.realmName("Frank")); // Uses a BasicAuthenticationEntryPoint

		LocalUsers localUsers;
		try (InputStream is = ymlFileURL.openStream(); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(is)) {
			Yaml yaml = new Yaml();

			localUsers = yaml.loadAs(reader, LocalUsers.class);
		} catch (Exception e) {
			throw new IllegalStateException("unable to parse YAML file ["+ymlFileURL+"]", e);
		}

		InMemoryUserDetailsManager udm = new InMemoryUserDetailsManager(localUsers.getUserDetails());
		http.userDetailsService(udm);

		return http.build();
	}
}
