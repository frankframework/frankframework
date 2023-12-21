/*
   Copyright 2020 WeAreFrank!

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

import javax.annotation.security.PermitAll;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/")
public class Authentication {
	@Context HttpServletRequest httpServletRequest;
	@Context HttpServletResponse httpServletResponse;

	protected Logger log = LogManager.getLogger(this);

	@GET
	@PermitAll
	@Path("/logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logout() {

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
		return Response.status(Status.UNAUTHORIZED).build();
	}
}
