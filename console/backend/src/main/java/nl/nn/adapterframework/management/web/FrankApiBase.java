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
package nl.nn.adapterframework.management.web;

import java.security.Principal;

import javax.annotation.Nonnull;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;

import lombok.Getter;
import nl.nn.adapterframework.management.bus.OutboundGateway;
import nl.nn.adapterframework.util.ResponseUtils;
import nl.nn.adapterframework.web.filters.DeprecationFilter;

/**
 * Base class for API endpoints.
 * Contains helper methods to read JAX-RS multiparts and handle message conversions to JAX-RS Responses.
 * @author	Niels Meijer
 */

public abstract class FrankApiBase implements ApplicationContextAware, InitializingBean {

	@Context protected ServletConfig servletConfig;
	@Context protected @Getter SecurityContext securityContext;
	@Context protected @Getter HttpServletRequest servletRequest;
	private @Getter ApplicationContext applicationContext;
	@Context protected @Getter UriInfo uriInfo;
	@Context private Request rsRequest;
	private @Getter Environment environment;

	private JAXRSServiceFactoryBean serviceFactory = null;

	protected Logger log = LogManager.getLogger(this);

	protected final OutboundGateway getGateway() {
		return getApplicationContext().getBean("outboundGateway", OutboundGateway.class);
	}

	@Nonnull
	protected Message<?> sendSyncMessage(RequestMessageBuilder input) {
		Message<?> message = getGateway().sendSyncMessage(input.build());
		if(message == null) {
			StringBuilder errorMessage = new StringBuilder("did not receive a reply while sending message to topic ["+input.getTopic()+"]");
			if(input.getAction() != null) {
				errorMessage.append(" with action [");
				errorMessage.append(input.getAction());
				errorMessage.append("]");
			}
			throw new ApiException(errorMessage.toString());
		}
		return message;
	}

	public Response callSyncGateway(RequestMessageBuilder input) throws ApiException {
		return callSyncGateway(input, false);
	}

	public Response callSyncGateway(RequestMessageBuilder input, boolean evaluateEtag) throws ApiException {
		Message<?> response = sendSyncMessage(input);
		EntityTag eTag = null;
		if(evaluateEtag) {
			eTag = ResponseUtils.generateETagHeaderValue(response);
		}
		if(eTag != null) {
			ResponseBuilder builder = rsRequest.evaluatePreconditions(eTag);
			if(builder != null) { //If the eTag matches the response will be non-null
				return builder.tag(eTag).build(); //Append the tag and force a 304 (Not Modified) or 412 (Precondition Failed)
			}
		}
		return ResponseUtils.convertToJaxRsResponse(response).tag(eTag).build();
	}

	public Response callAsyncGateway(RequestMessageBuilder input) {
		OutboundGateway gateway = getGateway();
		gateway.sendAsyncMessage(input.build());
		return Response.ok().build();
	}

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public final void afterPropertiesSet() throws Exception {
		SpringJAXRSServerFactoryBean server = (SpringJAXRSServerFactoryBean) applicationContext.getBean("JAXRS-IAF-API");
		serviceFactory = server.getServiceFactory();
		environment = applicationContext.getEnvironment();
	}

	/** Get a property from the Spring Environment. */
	protected <T> T getProperty(String key, T defaultValue) {
		return environment.getProperty(key, (Class<T>) defaultValue.getClass(), defaultValue);
	}

	protected final boolean allowDeprecatedEndpoints() {
		return getProperty(DeprecationFilter.ALLOW_DEPRECATED_ENDPOINTS_KEY, false);
	}

	protected JAXRSServiceFactoryBean getJAXRSService() {
		return serviceFactory;
	}

	protected String getUserPrincipalName() {
		Principal principal = securityContext.getUserPrincipal();
		if(principal != null && StringUtils.isNotEmpty(principal.getName())) {
			return principal.getName();
		}
		return null;
	}
}
