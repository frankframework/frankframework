/*
   Copyright 2023, 2026 WeAreFrank!

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
package org.frankframework.management.gateway;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

public class InputStreamHttpMessageConverter extends AbstractHttpMessageConverter<InputStream> {

	public InputStreamHttpMessageConverter() {
		super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
	}

	@Override
	protected boolean supports(@NonNull Class<?> clazz) {
		return InputStream.class.isAssignableFrom(clazz);
	}

	@NonNull
	@Override
	protected InputStream readInternal(@NonNull Class<? extends InputStream> clazz, @NonNull HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
		throw new NotImplementedException("messages should not be read directly as InputStream");
	}

	@Override
	protected void writeInternal(@NonNull InputStream is, @NonNull HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		StreamUtils.copy(is, outputMessage.getBody());
	}
}
