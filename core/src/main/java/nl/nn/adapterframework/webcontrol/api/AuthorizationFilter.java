/*
Copyright 2016-2017, 2020 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

/**
 * Manages authorization per resource/collection.
 * 
 * @since   7.0-B1
 * @author  Niels Meijer
 */

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

	private static final Response FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();
	private static final Response SERVER_ERROR = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	protected Logger log = LogUtil.getLogger(this);

	@Context private HttpServletRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		SecurityContext securityContext = requestContext.getSecurityContext();
		if(securityContext.getUserPrincipal() == null) {
			return; //No userPrincipal, authentication is disabled.
		}

		if(requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
			//Preflight in here?
			return;
		}

		Message message = JAXRSUtils.getCurrentMessage();
		Method method = (Method)message.get("org.apache.cxf.resource.method");
		if(method == null) {
			log.error("unable to fetch resource method from CXF Message");
			requestContext.abortWith(SERVER_ERROR);
			return;
		}

		if(method.isAnnotationPresent(DenyAll.class)) {
			//Functionality has been disallowed.
			requestContext.abortWith(FORBIDDEN);
			return;
		}
		if(method.isAnnotationPresent(PermitAll.class)) {
			//No authorization required.
			return;
		}

		//Presume `PermitAll` when RolesAllowed annotation is not set
		if(method.isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
			Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
			log.info("checking authorisation for user ["+securityContext.getUserPrincipal().getName()+"] on uri ["+method.getAnnotation(javax.ws.rs.Path.class).value()+"] required roles " + rolesSet.toString());

			if(!doAuth(securityContext, rolesSet)) {
				requestContext.abortWith(FORBIDDEN);
				return;
			}
		}
	}

	/**
	 * validate if the user has the required role to access the resource
	 */
	private boolean doAuth(SecurityContext securityContext, final Set<String> rolesSet) {
		for (String role : rolesSet) {
			if(securityContext.isUserInRole(role)) {
				return true;
			}
		}

		return false;
	}
}
