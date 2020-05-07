/*
Copyright 2016-2017 Integration Partners B.V.

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

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;

/**
 * Manages authorization per resource/collection.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class AuthorizationFilter implements ContainerRequestFilter {

	private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse(null, 403, new Headers<Object>());
	private static final ServerResponse SERVER_ERROR = new ServerResponse(null, 500, new Headers<Object>());
	protected Logger log = LogUtil.getLogger(this);

	public void filter(ContainerRequestContext requestContext) throws IOException {
		if(requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
			//Preflight in here?
			return;
		}

		SecurityContext securityContext = requestContext.getSecurityContext();

		if(securityContext.getUserPrincipal() == null) {
			//Not logged in. Auth restriction should be done via web.xml, if no userPrincipal is set it uses anonymous login
			return;
		}

		ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
		if(methodInvoker == null) {
			log.error("Unable to fetch method from ResourceMethodInvoker");
			requestContext.abortWith(SERVER_ERROR);
		}
		Method method = methodInvoker.getMethod();

		if(method.isAnnotationPresent(DenyAll.class)) {
			//Functionality has been disallowed.
			requestContext.abortWith(ACCESS_FORBIDDEN);
			return;
		}
		if(method.isAnnotationPresent(PermitAll.class)) {
			//No authorization required.
			return;
		}

		if(method.isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
			Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
//			System.out.println("Checking authentication for user ["+securityContext.getUserPrincipal().getName()+"] method ["+method.getAnnotation(javax.ws.rs.Path.class).value()+"] roles " + rolesSet.toString());

			//Verifying username and password
			if(!doAuth(securityContext, rolesSet)) {
				requestContext.abortWith(ACCESS_FORBIDDEN);
				return;
			}
		}
		//Don't do anything if RolesAllowed annotation is not set
	}

	private boolean doAuth(SecurityContext securityContext, final Set<String> rolesSet) {
//		Principal userPrincipal = securityContext.getUserPrincipal();

		for (String role : rolesSet) {
			if(securityContext.isUserInRole(role) == true)
				return true;
		}

		return false;
	}
}
