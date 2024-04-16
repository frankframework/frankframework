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

	public static ResponseBuilder convertToJaxRsResponse(Message<?> response) {
		int status = BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 200);
		String mimeType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		ResponseBuilder builder = Response.status(status);

		if(mimeType != null) {
			builder.type(mimeType);
		}

		if(status == 200 || status > 204) {
			builder.entity(response.getPayload());
		}

		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		if(contentDisposition != null) {
			builder.header("Content-Disposition", contentDisposition);
		}

		return builder;
	}

	public static <T> ResponseEntity.BodyBuilder convertToSpringResponse (Message<T> response) {
		int status = BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 200);
		String mimeType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		ResponseEntity.BodyBuilder responseEntity = ResponseEntity.status(status);
		HttpHeaders entityHeaders = new HttpHeaders();

		if(mimeType != null) {
			entityHeaders.setContentType(MediaType.valueOf(mimeType));
		}

		if (status == 200 || status > 204) {
			responseEntity.body(response.getPayload());
		}

		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		if (contentDisposition != null) {
			entityHeaders.setContentDisposition(ContentDisposition.parse(contentDisposition));
		}

		responseEntity.headers(entityHeaders);
		return responseEntity;
	}

	public static ResponseBuilder convertToJaxRsStreamingResponse(Message<?> message, StreamingOutput response){
		int status = BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 200);
		String mimeType = BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY, null);
		ResponseBuilder builder = Response.status(status);

		if(mimeType != null) {
			builder.type(mimeType);
		}

		if(status == 200 || status > 204) {
			builder.entity(response);
		}

		String contentDisposition = BusMessageUtils.getHeader(message, MessageBase.CONTENT_DISPOSITION_KEY, null);
		if(contentDisposition != null) {
			builder.header("Content-Disposition", contentDisposition);
		}

		return builder;
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

	private static String generateETagHeaderValueString(Object payload) {
		byte[] bytes;
		if(payload instanceof String) {
			bytes = ((String)payload).getBytes(StandardCharsets.UTF_8);
		} else if (payload instanceof byte[]) {
			bytes = (byte[]) payload;
		} else {
			throw new NotImplementedException("return type ["+payload.getClass()+"] not implemented");
		}

		return DigestUtils.md5DigestAsHex(bytes);
	}
}
