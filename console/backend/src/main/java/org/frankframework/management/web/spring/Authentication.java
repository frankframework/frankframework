package org.frankframework.management.web.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class Authentication extends FrankApiBase {

	protected Logger log = LogManager.getLogger(this);

	@PermitAll
	@GetMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

		if(httpServletRequest.getUserPrincipal() != null) {
			String user = httpServletRequest.getUserPrincipal().getName();
			try {
				httpServletRequest.logout();
				log.debug("successfully logged out user ["+user+"]");
			} catch (ServletException e) {
				throw new ApiException(e);
			}
		} else {
			log.debug("unable to log out user, not logged in");
		}

		httpServletResponse.setHeader("Refresh", "5");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

}
