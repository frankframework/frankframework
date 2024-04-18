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
package org.frankframework.util;

import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.DigestUtils;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.message.MessageBase;

public class ResponseUtils {

	public static ResponseBuilder convertToJaxRsResponse(Message<?> message) {
		return convertToJaxRsResponse(message, null);
	}

	public static ResponseBuilder convertToJaxRsResponse(Message<?> message, StreamingOutput response) {
		int status = BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 200);
		String mimeType = BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY, null);
		ResponseBuilder builder = Response.status(status);

		if (mimeType != null) {
			builder.type(mimeType);
		}

		if (status == 200 || status > 204) {
			if(response != null) {
				builder.entity(response);
			}
			builder.entity(message.getPayload());
		}

		String contentDisposition = BusMessageUtils.getHeader(message, MessageBase.CONTENT_DISPOSITION_KEY, null);
		if (contentDisposition != null) {
			builder.header("Content-Disposition", contentDisposition);
		}

		return builder;
	}

	public static ResponseEntity<?> convertToSpringResponse(Message<?> message) {
		return convertToSpringResponse(message, null);
	}

	public static ResponseEntity<?> convertToSpringResponse(Message<?> message, StreamingOutput response) {
		int status = BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 200);
		String mimeType = BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY, null);
		ResponseEntity.BodyBuilder responseEntity = ResponseEntity.status(status);
		HttpHeaders httpHeaders = new HttpHeaders();

		if(mimeType != null) {
			httpHeaders.setContentType(MediaType.valueOf(mimeType));
		}

		String contentDisposition = BusMessageUtils.getHeader(message, MessageBase.CONTENT_DISPOSITION_KEY, null);
		if (contentDisposition != null) {
			httpHeaders.setContentDisposition(ContentDisposition.parse(contentDisposition));
		}

		responseEntity.headers(httpHeaders);

		if (status == 200 || status > 204) {
			if(response != null) {
				return responseEntity.body(response);
			}
			return responseEntity.body(message.getPayload());
		}

		return responseEntity.build();
	}

	/** Shallow eTag generation, saves bandwidth but not computing power */
	public static EntityTag generateETagHeaderValue(Message<?> response) {
		MessageHeaders headers = response.getHeaders();
		String mime = headers.get(MessageBase.MIMETYPE_KEY, String.class);
		if(MediaType.APPLICATION_JSON_VALUE.equals(mime)) {
			return generateETagHeaderValue(response.getPayload(), true);
		}
		return null;
	}

	private static EntityTag generateETagHeaderValue(Object payload, boolean isWeak) {
		byte[] bytes;
		if(payload instanceof String) {
			bytes = ((String)payload).getBytes(StandardCharsets.UTF_8);
		} else if (payload instanceof byte[]) {
			bytes = (byte[]) payload;
		} else {
			throw new NotImplementedException("return type ["+payload.getClass()+"] not implemented");
		}

		return new EntityTag(DigestUtils.md5DigestAsHex(bytes), isWeak);
	}
}
