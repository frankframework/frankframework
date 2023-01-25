/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.bus;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;

import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

public class BusMessageUtils {
	public static final String HEADER_DATASOURCE_NAME_KEY = "datasourceName";
	public static final String HEADER_CONNECTION_FACTORY_NAME_KEY = "connectionFactory";
	public static final String HEADER_CONFIGURATION_NAME_KEY = "configuration";
	public static final String HEADER_ADAPTER_NAME_KEY = "adapter";
	public static final String HEADER_RECEIVER_NAME_KEY = "receiver";

	private static final Logger LOG = LogUtil.getLogger(BusMessageUtils.class);

	public static String getHeader(Message<?> message, String headerName) {
		MessageHeaders headers = message.getHeaders();
		if(headers.containsKey(headerName)) {
			return headers.get(headerName, String.class);
		}
		return null;
	}

	public static boolean containsHeader(Message<?> message, String headerName) {
		return message.getHeaders().get(headerName) != null;
	}

	public static String getHeader(Message<?> message, String headerName, String defaultValue) {
		MessageHeaders headers = message.getHeaders();
		if(headers.containsKey(headerName)) {
			String value = headers.get(headerName, String.class);
			if(StringUtils.isNotEmpty(value)) {
				return value;
			}
		}
		return defaultValue;
	}

	public static Integer getIntHeader(Message<?> message, String headerName, Integer defaultValue) {
		MessageHeaders headers = message.getHeaders();
		if(headers.containsKey(headerName)) {
			try {
				return headers.get(headerName, Integer.class);
			} catch (IllegalArgumentException e) {
				Object header = headers.get(headerName);
				LOG.info("unable to parse header as integer", e);
				return Integer.parseInt(""+header);
			}
		}
		return defaultValue;
	}

	public static Boolean getBooleanHeader(Message<?> message, String headerName, Boolean defaultValue) {
		MessageHeaders headers = message.getHeaders();
		if(headers.containsKey(headerName)) {
			try {
				return headers.get(headerName, Boolean.class);
			} catch (IllegalArgumentException e) {
				Object header = headers.get(headerName);
				LOG.info("unable to parse header as boolean", e);
				return Boolean.parseBoolean(""+header);
			}
		}
		return defaultValue;
	}

	public static ResponseBuilder convertToJaxRsResponse(Message<?> response) {
		MessageHeaders headers = response.getHeaders();
		int status = (int) headers.get(ResponseMessage.STATUS_KEY);
		String mimeType = (String) headers.get(ResponseMessage.MIMETYPE_KEY);
		ResponseBuilder builder = Response.status(status);

		if(mimeType != null) {
			builder.type(mimeType);
		}

		if(status == 200 || status > 204) {
			builder.entity(response.getPayload());
		}

		String contentDisposition = (String) headers.get(ResponseMessage.CONTENT_DISPOSITION_KEY);
		if(contentDisposition != null) {
			builder.header("Content-Disposition", contentDisposition);
		}

		return builder;
	}

	/** Shallow eTag generation, saves bandwidth but not computing power */
	public static EntityTag generateETagHeaderValue(Message<?> response) {
		MessageHeaders headers = response.getHeaders();
		String mime = headers.get(ResponseMessage.MIMETYPE_KEY, String.class);
		if(MediaType.APPLICATION_JSON_VALUE.equals(mime)) {
			String json = (String) response.getPayload();
			return generateETagHeaderValue(json, true);
		}
		return null;
	}

	private static EntityTag generateETagHeaderValue(String json, boolean isWeak) {
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		return new EntityTag(DigestUtils.md5DigestAsHex(bytes), isWeak);
	}

	/** May be anonymousUser, or a string representation of the currently logged in user. */
	@Nullable
	public static String getUserPrincipalName() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication != null) {
			return authentication.getName();
		}
		return null;
	}

	@Nonnull
	private static Collection<? extends GrantedAuthority> getAuthorities() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication != null) {
			return authentication.getAuthorities();
		}
		return Collections.emptyList();
	}

	public static boolean hasAnyRole(String... roles) {
		for(String role : roles) {
			if(hasRole(role)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * See AuthorityAuthorizationManager#ROLE_PREFIX
	 */
	public static boolean hasRole(String role) {
		boolean granted = false;
		for(GrantedAuthority grantedAuthority : getAuthorities()) {
			String authorityName = grantedAuthority.getAuthority().substring(5); //chomp off the AuthorityAuthorizationManager#ROLE_PREFIX
			granted = authorityName.equals(role);
			if(granted) {
				return true;
			}
		}
		return granted;
	}

	public static <E extends Enum<E>> E getEnumHeader(Message<?> message, String headerName, Class<E> enumClazz) {
		return getEnumHeader(message, headerName, enumClazz, null);
	}

	public static <E extends Enum<E>> E getEnumHeader(Message<?> message, String headerName, Class<E> enumClazz, E defaultValue) {
		String value = getHeader(message, headerName);
		if(StringUtils.isNotEmpty(value)) {
			try {
				return EnumUtils.parse(enumClazz, value);
			} catch (IllegalArgumentException e) {
				throw new BusException("unable to parse value ["+value+"]", e);
			}
		}
		return defaultValue;
	}
}
