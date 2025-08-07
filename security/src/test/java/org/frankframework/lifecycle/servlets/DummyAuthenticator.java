package org.frankframework.lifecycle.servlets;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test the basic Authenticator actions without any direct modifications
 * to the {@link HttpSecurity} / {@link SecurityFilterChain}.
 *
 * @see {@link DummyAuthenticatorTest}.
 */
public class DummyAuthenticator extends AbstractServletAuthenticator {

	@Override
	protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
		return http.build();
	}

}
