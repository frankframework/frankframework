package nl.nn.adapterframework.lifecycle.servlets;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public interface IAuthenticator {

	SecurityFilterChain configure(ServletConfiguration config, HttpSecurity http) throws Exception;
}
