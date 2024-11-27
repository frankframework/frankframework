package org.frankframework.lifecycle.servlets;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public class DummyAuthenticatorTest extends ServletAuthenticatorTest {

	public static class DummyAuthenticator extends AbstractServletAuthenticator {
		@Override
		protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
			return http.build();
		}
	}

	@Override
	protected DummyAuthenticator createAuthenticator() {
		return new DummyAuthenticator();
	}
}
