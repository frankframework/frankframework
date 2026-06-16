/*
   Copyright 2023-2026 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import lombok.NoArgsConstructor;

import org.frankframework.console.ApiException;
import org.frankframework.util.StreamUtil;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RequestUtils {

	@NonNull
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
