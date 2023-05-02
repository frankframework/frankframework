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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.management.web.ApiException;

public abstract class RequestUtils {
	private static Logger LOG = LogManager.getLogger(RequestUtils.class);

	public static String resolveStringFromMap(MultipartBody inputDataMap, String key) throws ApiException {
		return resolveStringFromMap(inputDataMap, key, null);
	}

	public static String resolveStringFromMap(MultipartBody inputDataMap, String key, String defaultValue) throws ApiException {
		String result = resolveTypeFromMap(inputDataMap, key, String.class, null);
		if(StringUtils.isEmpty(result)) {
			if(defaultValue != null) {
				return defaultValue;
			}
			throw new ApiException("Key ["+key+"] may not be empty");
		}
		return result;
	}

	public static String resolveStringWithEncoding(MultipartBody inputDataMap, String key, String defaultEncoding) {
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

	public static <T> T resolveTypeFromMap(MultipartBody inputDataMap, String key, Class<T> clazz, T defaultValue) throws ApiException {
		try {
			Attachment attachment = inputDataMap.getAttachment(key);
			if(attachment != null) {
				return convert(clazz, attachment.getObject(InputStream.class));
			}
		} catch (Exception e) {
			LOG.debug("Failed to parse parameter ["+key+"]", e);
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
			return (T) Boolean.valueOf(str); //At the moment we allow null/empty -> FALSE
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
}
