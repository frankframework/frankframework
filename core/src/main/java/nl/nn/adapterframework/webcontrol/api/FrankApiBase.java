/*
Copyright 2016-2022 WeAreFrank!

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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import lombok.Getter;
import nl.nn.adapterframework.lifecycle.Gateway;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Baseclass to fetch ibisContext + ibisManager
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

public abstract class FrankApiBase implements ApplicationContextAware, InitializingBean {
	public static final String HEADER_DATASOURCE_NAME_KEY = "datasourceName";

	@Context protected ServletConfig servletConfig;
	@Context protected @Getter SecurityContext securityContext;
	@Context protected @Getter HttpServletRequest servletRequest;
	private @Getter ApplicationContext applicationContext;
	@Context protected @Getter UriInfo uriInfo;

	private JAXRSServiceFactoryBean serviceFactory = null;

	protected Logger log = LogUtil.getLogger(this);
	protected static String HATEOASImplementation = AppConstants.getInstance().getString("ibis-api.hateoasImplementation", "default");

	public Response callSyncGateway(RequestMessageBuilder input) throws ApiException {
		Gateway gateway = getApplicationContext().getBean("gateway", Gateway.class);
		Message<?> response = gateway.sendSyncMessage(input.build());
		if(response != null) {
			return BusMessageUtils.convertToJaxRsResponse(response);
		}
		StringBuilder errorMessage = new StringBuilder("did not receive a reply while sending message to topic ["+input.getTopic()+"]");
		if(input.getAction() != null) {
			errorMessage.append(" with action [");
			errorMessage.append(input.getAction());
			errorMessage.append("]");
		}
		throw new ApiException(errorMessage.toString());
	}

	public Response callAsyncGateway(RequestMessageBuilder input) throws ApiException {
		Gateway gateway = getApplicationContext().getBean("gateway", Gateway.class);
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
			String encoding = (StringUtils.isNotEmpty(defaultEncoding)) ? defaultEncoding : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			if(msg.getContentType().getParameters() != null) { //Encoding has explicitly been set on the multipart bodypart
				String charset = msg.getContentType().getParameters().get("charset");
				if(StringUtils.isNotEmpty(charset)) {
					encoding = charset;
				}
			}
			InputStream is = msg.getObject(InputStream.class);

			try {
				String inputMessage = Misc.streamToString(is, "\n", encoding, false);
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
		String str = Misc.streamToString(is);
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
	protected String getValue(Map<String, Object> json, String key) {
		Object val = json.get(key);
		if(val != null) {
			return val.toString();
		}
		return null;
	}

	protected Integer getIntegerValue(Map<String, Object> json, String key) {
		String value = getValue(json, key);
		if(value != null) {
			return Integer.parseInt(value);
		}
		return null;
	}

	protected Boolean getBooleanValue(Map<String, Object> json, String key) {
		String value = getValue(json, key);
		if(value != null) {
			return Boolean.parseBoolean(value);
		}
		return null;
	}
}
