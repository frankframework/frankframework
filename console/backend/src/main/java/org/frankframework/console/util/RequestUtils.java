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
package org.frankframework.console.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.console.ApiException;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RequestUtils {

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
			return (T) Boolean.valueOf(str); // At the moment we allow null/empty -> FALSE
		}
		return ClassUtils.convertToType(clazz, str);
	}

	/**
	 * If present returns the value as String
	 * Else returns NULL
	 */
	public static @Nullable String getValue(Map<String, Object> json, String key) {
		Object val = json.get(key);
		if(val != null) {
			return val.toString();
		}
		return null;
	}

	public static @Nullable Integer getIntegerValue(Map<String, Object> json, String key) {
		String value = getValue(json, key);
		if(value != null) {
			return Integer.parseInt(value);
		}
		return null;
	}

	public static @Nullable Boolean getBooleanValue(Map<String, Object> json, String key) {
		String value = getValue(json, key);
		if(value != null) {
			return Boolean.parseBoolean(value);
		}
		return null;
	}

	@Nonnull
	public static <T> T resolveRequiredProperty(String key, T multiFormProperty, T defaultValue) throws ApiException {
		if (multiFormProperty != null) {
			return multiFormProperty;
		}
		if(defaultValue != null) {
			return defaultValue;
		}
		throw new ApiException("Key ["+key+"] not defined", 400);
	}

	public static String resolveStringWithEncoding(String key, MultipartFile message, String defaultEncoding, boolean nullOnEmpty) {
		if(message != null) {
			String encoding = StringUtils.isNotEmpty(defaultEncoding) ? defaultEncoding : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			String messageContentType = message.getContentType();
			if (messageContentType != null) {
				MediaType contentType = MediaType.valueOf(messageContentType);
				Charset charset = contentType.getCharset();
				if(charset != null) {
					encoding = charset.toString();
				}
			}

			try {
				InputStream is = message.getInputStream();
				String inputMessage = StreamUtil.streamToString(is, "\n", encoding, false);
				if(nullOnEmpty) {
					return StringUtils.isEmpty(inputMessage) ? null : inputMessage;
				}
				return inputMessage;
			} catch (UnsupportedEncodingException e) {
				throw new ApiException("unsupported file encoding ["+encoding+"]");
			} catch (IOException e) {
				throw new ApiException("error parsing value of key ["+key+"]", e);
			}
		}
		return null;
	}
}
