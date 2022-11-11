package nl.nn.adapterframework.lifecycle.servlets;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import nl.nn.adapterframework.lifecycle.ServletManager;

public class NoOpAuthenticator implements IAuthenticator {
	private static final String ROLE_PREFIX = "ROLE_"; //see AuthorityAuthorizationManager#ROLE_PREFIX

	@Override
	public SecurityFilterChain configure(ServletConfiguration config, HttpSecurity http) throws Exception {
		http.anonymous().authorities(getAuthorities(config.getSecurityRoles()));
		return http.build();
	}

	private static List<GrantedAuthority> getAuthorities(List<String> securityRoles) {
		if(securityRoles == null || securityRoles.isEmpty()) {
			securityRoles = ServletManager.DEFAULT_IBIS_ROLES;
		}
		List<GrantedAuthority> grantedAuthorities = new ArrayList<>(securityRoles.size());
		for (String role : securityRoles) {
			grantedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
		}
		return grantedAuthorities;
	}
}
