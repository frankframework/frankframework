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
package org.frankframework.management.web;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class Authentication extends FrankApiBase {

	@PermitAll
	@GetMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

		if (httpServletRequest.getUserPrincipal() != null) {
			String user = httpServletRequest.getUserPrincipal().getName();
			try {
				httpServletRequest.logout();
				log.debug("successfully logged out user [{}]", user);
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
