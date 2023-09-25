/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.web.filters;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import lombok.Setter;

/**
 * Manages deprecations per resource/collection.
 * 
 * Default JAX-RS provider and is automatically picked-up by the FF!API Spring context because of the package (component) scanner.
 * 
 * @since   7.8.1
 * @author  Niels Meijer
 */

@Provider
@Priority(Priorities.USER)
public class DeprecationFilter implements ContainerRequestFilter, EnvironmentAware {

	public static final String ALLOW_DEPRECATED_ENDPOINTS_KEY = "iaf-api.allowDeprecated";
	private static final Response DEPRECATION_ERROR = Response.status(Response.Status.BAD_REQUEST).build();
	private static final Response SERVER_ERROR = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

	private Logger log = LogManager.getLogger(this);
	private @Setter Environment environment;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		Message message = JAXRSUtils.getCurrentMessage();
		Method method = (Method)message.get("org.apache.cxf.resource.method");
		if(method == null) {
			log.error("unable to fetch resource method from CXF Message");
			requestContext.abortWith(SERVER_ERROR);
			return;
		}

		if(method.isAnnotationPresent(Deprecated.class)) {
			if(!allowDeprecatedEndpoints()) {
				requestContext.abortWith(DEPRECATION_ERROR);
			}
			log.warn("endpoint [{}] has been deprecated, set property [{}=true] to restore functionality", getFullPath(method), ALLOW_DEPRECATED_ENDPOINTS_KEY);
		}
	}

	// The basepath is usually a '/', but path may also start with a slash.
	// Ensure a valid path is returned.
	private String getFullPath(Method method) {
		Path classPath = method.getDeclaringClass().getAnnotation(Path.class);
		final String basePath = (classPath != null) ? classPath.value() : "/";

		StringBuilder pathToUse = new StringBuilder();
		if(!basePath.startsWith("/")) {
			pathToUse.append("/");
		}
		pathToUse.append(basePath);

		Path methodPath = method.getAnnotation(Path.class);
		if(methodPath != null) {
			final String path = methodPath.value();
			pathToUse.append( (basePath.endsWith("/") && path.startsWith("/")) ? path.substring(1) : path);
		}
		return pathToUse.toString();
	}

	/** Get a property from the Spring Environment. */
	@SuppressWarnings("unchecked")
	private <T> T getProperty(String key, T defaultValue) {
		return environment.getProperty(key, (Class<T>) defaultValue.getClass(), defaultValue);
	}

	private boolean allowDeprecatedEndpoints() {
		return getProperty(ALLOW_DEPRECATED_ENDPOINTS_KEY, false);
	}
}
