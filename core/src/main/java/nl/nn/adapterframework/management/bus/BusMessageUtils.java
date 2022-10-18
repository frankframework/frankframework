/*
   Copyright 2022 WeAreFrank!

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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

public class BusMessageUtils {
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

	public static Response convertToJaxRsResponse(Message<?> response) {
		MessageHeaders headers = response.getHeaders();
		int status = (int) headers.get(ResponseMessage.STATUS_KEY);
		String mimeType = (String) headers.get(ResponseMessage.MIMETYPE_KEY);

		ResponseBuilder builder = Response.status(status).entity(response.getPayload()).type(mimeType);

		String contentDisposition = (String) headers.get(ResponseMessage.CONTENT_DISPOSITION_KEY);
		if(contentDisposition != null) {
			builder.header("Content-Disposition", contentDisposition);
		}

		return builder.build();
	}

	public static UserDetails getUserDetails() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication != null && authentication.getPrincipal() instanceof UserDetails) {
			return (UserDetails) authentication.getPrincipal();
		}
		return null;
	}

	/**
	 * See AuthorityAuthorizationManager#ROLE_PREFIX
	 */
	public static boolean hasAuthority(String authority) {
		UserDetails userDetails = getUserDetails();
		boolean granted = false;
		if(userDetails != null) {
			for(GrantedAuthority grantedAuthority : userDetails.getAuthorities()) {
				String authorityName = grantedAuthority.getAuthority().substring(5); //chomp off the AuthorityAuthorizationManager#ROLE_PREFIX
				granted = authorityName.equals(authority);
				if(granted) {
					return true;
				}
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
			return EnumUtils.parse(enumClazz, value);
		}
		return defaultValue;
	}
}
