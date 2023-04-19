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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import lombok.Getter;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.IntegrationGateway;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Base class for API endpoints.
 * Contains helper methods to read JAX-RS multiparts and handle message conversions to JAX-RS Responses.
 * @author	Niels Meijer
 */

public abstract class FrankApiBase implements ApplicationContextAware, InitializingBean {
	public static final String HEADER_DATASOURCE_NAME_KEY = "datasourceName";
	public static final String HEADER_CONNECTION_FACTORY_NAME_KEY = "connectionFactory";
	public static final String HEADER_CONFIGURATION_NAME_KEY = "configuration";
	public static final String HEADER_ADAPTER_NAME_KEY = "adapter";
	public static final String HEADER_RECEIVER_NAME_KEY = "receiver";

	@Context protected ServletConfig servletConfig;
	@Context protected @Getter SecurityContext securityContext;
	@Context protected @Getter HttpServletRequest servletRequest;
	private @Getter ApplicationContext applicationContext;
	@Context protected @Getter UriInfo uriInfo;
	@Context private Request rsRequest;

	private JAXRSServiceFactoryBean serviceFactory = null;

	public static final String DEFAULT_CHARSET = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	protected Logger log = LogManager.getLogger(this);

	protected final IntegrationGateway getGateway() {
		return getApplicationContext().getBean("outboundGateway", IntegrationGateway.class);
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
			eTag = BusMessageUtils.generateETagHeaderValue(response);
		}
		if(eTag != null) {
			ResponseBuilder builder = rsRequest.evaluatePreconditions(eTag);
			if(builder != null) { //If the eTag matches the response will be non-null
				return builder.tag(eTag).build(); //Append the tag and force a 304 (Not Modified) or 412 (Precondition Failed)
			}
		}
		return BusMessageUtils.convertToJaxRsResponse(response).tag(eTag).build();
	}

	public Response callAsyncGateway(RequestMessageBuilder input) {
		IntegrationGateway gateway = getGateway();
		gateway.sendAsyncMessage(input.build());
		return Response.ok().build();
	}

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SpringJAXRSServerFactoryBean server = (SpringJAXRSServerFactoryBean) applicationContext.getBean("IAF-API");
		serviceFactory = server.getServiceFactory();
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

	protected String resolveStringFromMap(MultipartBody inputDataMap, String key) throws ApiException {
		return resolveStringFromMap(inputDataMap, key, null);
	}

	protected String resolveStringFromMap(MultipartBody inputDataMap, String key, String defaultValue) throws ApiException {
		String result = resolveTypeFromMap(inputDataMap, key, String.class, null);
		if(StringUtils.isEmpty(result)) {
			if(defaultValue != null) {
				return defaultValue;
			}
			throw new ApiException("Key ["+key+"] may not be empty");
		}
		return result;
	}

	protected String resolveStringWithEncoding(MultipartBody inputDataMap, String key, String defaultEncoding) {
		Attachment msg = inputDataMap.getAttachment(key);
		if(msg != null) {
			String encoding = (StringUtils.isNotEmpty(defaultEncoding)) ? defaultEncoding : DEFAULT_CHARSET;
			if(msg.getContentType().getParameters() != null) { //Encoding has explicitly been set on the multipart bodypart
				String charset = msg.getContentType().getParameters().get("charset");
				if(StringUtils.isNotEmpty(charset)) {
					encoding = charset;
				}
			}
			InputStream is = msg.getObject(InputStream.class);

			try {
				String inputMessage = StreamUtil.streamToString(is, "\n", encoding, false);
				return StringUtils.isEmpty(inputMessage) ? null : inputMessage;
			} catch (UnsupportedEncodingException e) {
				throw new ApiException("unsupported file encoding ["+encoding+"]");
			} catch (IOException e) {
				throw new ApiException("error parsing value of key ["+key+"]", e);
			}
		}
		return null;
	}

	protected <T> T resolveTypeFromMap(MultipartBody inputDataMap, String key, Class<T> clazz, T defaultValue) throws ApiException {
		try {
			Attachment attachment = inputDataMap.getAttachment(key);
			if(attachment != null) {
				return convert(clazz, attachment.getObject(InputStream.class));
			}
		} catch (Exception e) {
			log.debug("Failed to parse parameter ["+key+"]", e);
		}
		if(defaultValue != null) {
			return defaultValue;
		}
		throw new ApiException("Key ["+key+"] not defined", 400);
	}

	@SuppressWarnings("unchecked")
	protected static <T> T convert(Class<T> clazz, InputStream is) throws IOException {
		if(clazz.isAssignableFrom(InputStream.class)) {
			return (T) is;
		}
		String str = StreamUtil.streamToString(is);
		if(str == null) {
			return null;
		}
		if(clazz.isAssignableFrom(boolean.class) || clazz.isAssignableFrom(Boolean.class)) {
			return (T) Boolean.valueOf(str);
		} else if(clazz.isAssignableFrom(int.class) || clazz.isAssignableFrom(Integer.class)) {
			return (T) Integer.valueOf(str);
		} else if(clazz.isAssignableFrom(String.class)) {
			return (T) str;
		}
		throw new IllegalArgumentException("cannot convert to class ["+clazz+"]");
	}

	/**
	 * If present returns the value as String
	 * Else returns NULL
	 */
	protected @Nullable String getValue(Map<String, Object> json, String key) {
		Object val = json.get(key);
		if(val != null) {
			return val.toString();
		}
		return null;
	}

	protected @Nullable Integer getIntegerValue(Map<String, Object> json, String key) {
		String value = getValue(json, key);
		if(value != null) {
			return Integer.parseInt(value);
		}
		return null;
	}

	protected @Nullable Boolean getBooleanValue(Map<String, Object> json, String key) {
		String value = getValue(json, key);
		if(value != null) {
			return Boolean.parseBoolean(value);
		}
		return null;
	}
}
