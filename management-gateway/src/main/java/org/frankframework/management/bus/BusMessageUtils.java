/*
   Copyright 2022-2026 WeAreFrank!

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
package org.frankframework.management.bus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.frankframework.management.bus.message.AbstractMessage;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;

public class BusMessageUtils {
	public static final String HEADER_DATASOURCE_NAME_KEY = "datasourceName";
	public static final String HEADER_CONNECTION_FACTORY_NAME_KEY = "connectionFactory";
	public static final String HEADER_CONFIGURATION_NAME_KEY = "configuration";
	public static final String HEADER_ADAPTER_NAME_KEY = "adapter";
	public static final String HEADER_RECEIVER_NAME_KEY = "receiver";
	public static final String HEADER_PIPE_NAME_KEY = "pipe";
	public static final String HEADER_PROCESSSTATE_KEY = "processState";
	public static final String HEADER_TARGET_KEY = "target";

	private static final Logger LOG = LogManager.getLogger(BusMessageUtils.class);

	public static final String HEADER_PREFIX = "meta-";
	public static final String HEADER_PREFIX_PATTERN = "meta-*";

	public static final String ALL_CONFIGS_KEY = "*ALL*";

	private BusMessageUtils() {
		// NO OP
	}

	@SuppressWarnings("unchecked")
	private static @Nullable <T> T getHeader(Message<?> message, String headerName, Class<T> type) {
		MessageHeaders headers = message.getHeaders();
		if (!contains(headers, headerName)) {
			return null;
		}
		Object rawValue = headers.get(HEADER_PREFIX + headerName);
		if (rawValue == null) {
			return null;
		}

		if (type.isAssignableFrom(rawValue.getClass())) {
			return (T) rawValue;
		}
		if (rawValue instanceof String) {
			try {
				return ClassUtils.convertToType(type, String.valueOf(rawValue));
			} catch (IllegalArgumentException e) {// Unable to convert something, fall back to the default value
				LOG.warn("unable to convert header to required type", e);
			}
		} else {
			LOG.warn("conversion of type [{}] not implemented", rawValue::getClass);
		}
		return null;
	}

	private static boolean contains(MessageHeaders headers, String headerName) {
		return headers.get(HEADER_PREFIX + headerName) != null;
	}

	public static boolean containsHeader(Message<?> message, String headerName) {
		return contains(message.getHeaders(), headerName);
	}

	public static @Nullable String getPayloadAsString(Message<?> message) {
		int status = BusMessageUtils.getIntHeader(message, AbstractMessage.STATUS_KEY, 200);
		if (!hasPayload(status)) {
			return null;
		}

		String mimeType = BusMessageUtils.getHeader(message, AbstractMessage.MIMETYPE_KEY, (String)null);
		if (mimeType != null) {
			MediaType mime = MediaType.valueOf(mimeType);
			if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mime) || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(mime)) {
				return (String) message.getPayload();
			}
		}
		return convertPayload(message.getPayload());
	}


	@NonNull
	public static String convertPayload(Object payload) {
		if (payload instanceof String string) {
			return string;
		} else if (payload instanceof byte[] bytes) {
			return new String(bytes);
		} else if (payload instanceof InputStream stream) {
			try {
				// Convert line endings to \n to show them in the browser as real line feeds
				return StreamUtil.streamToString(stream, "\n", false);
			} catch (IOException e) {
				throw new BusException("unable to read message payload", e);
			}
		}
		throw new BusException("unexpected message payload type [" + payload.getClass().getCanonicalName() + "]");
	}

	/**
	 * Extracted method so it's in one place.
	 * See {@link EmptyMessage} for more info.
	 */
	public static boolean hasPayload(int status) {
		return (status == 200 || status > 204);
	}

	public static @Nullable String getHeader(Message<?> message, String headerName) {
		return getHeader(message, headerName, String.class);
	}

	public static String getHeader(Message<?> message, String headerName, @Nullable String defaultValue) {
		String headerValue = getHeader(message, headerName, String.class);
		return StringUtils.isNotBlank(headerValue) ? headerValue : defaultValue;
	}

	public static Integer getIntHeader(Message<?> message, String headerName, Integer defaultValue) {
		Integer headerValue = getHeader(message, headerName, Integer.class);
		return headerValue != null ? headerValue : defaultValue;
	}

	public static Boolean getBooleanHeader(Message<?> message, String headerName, Boolean defaultValue) {
		Boolean headerValue = getHeader(message, headerName, Boolean.class);
		return headerValue != null ? headerValue : defaultValue;
	}

	public static <E extends Enum<E>> E getEnumHeader(Message<?> message, String headerName, Class<E> enumClazz) {
		return getEnumHeader(message, headerName, enumClazz, null);
	}

	public static <E extends Enum<E>> E getEnumHeader(Message<?> message, String headerName, Class<E> enumClazz, E defaultValue) {
		E headerValue = getHeader(message, headerName, enumClazz);
		return headerValue != null ? headerValue : defaultValue;
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

	@NonNull
	public static Collection<? extends GrantedAuthority> getAuthorities() {
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
			String authorityName = grantedAuthority.getAuthority().substring(5); // Chomp off the AuthorityAuthorizationManager#ROLE_PREFIX
			granted = authorityName.equals(role);
			if(granted) {
				return true;
			}
		}
		return granted;
	}
}
