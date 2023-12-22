/*
   Copyright 2016-2023 WeAreFrank!

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

import java.io.IOException;
import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

import org.frankframework.management.bus.BusMessageUtils;

/**
 * Manages authorization per resource/collection.
 * A more fine-grained version of the CXF SecureAnnotationsInterceptor.
 *
 * Default JAX-RS provider and is automatically picked-up by the FF!API Spring context because of the package (component) scanner.
 *
 * @since   7.0-B1
 * @author  Niels Meijer
 */

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

	private static final Response FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if(requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
			//Preflight in here should not be possible?
			return;
		}

		Message message = JAXRSUtils.getCurrentMessage();
		Method method = MessageUtils.getTargetMethod(message).orElseThrow(() -> new AccessDeniedException("Unauthorized") );

		if(method.isAnnotationPresent(DenyAll.class)) {
			//Functionality has been disallowed.
			requestContext.abortWith(FORBIDDEN);
			return;
		}

		//Presume `PermitAll` when RolesAllowed annotation is not set
		if(method.isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
			if(!BusMessageUtils.hasAnyRole(rolesAnnotation.value())) {
				requestContext.abortWith(FORBIDDEN);
			}
		}
	}
}
