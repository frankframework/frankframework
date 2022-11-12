package nl.nn.adapterframework.lifecycle.servlets;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

//SecurityContextHolder.getContext().getAuthentication(); can be used to retrieve the username (when available)
public interface IAuthenticator {

	SecurityFilterChain configure(ServletConfiguration config, HttpSecurity http) throws Exception;
}
