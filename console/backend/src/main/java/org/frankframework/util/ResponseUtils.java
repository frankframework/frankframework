/*
   Copyright 2023 - 2024 WeAreFrank!

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

import java.io.InputStream;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.message.MessageBase;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public class ResponseUtils {

	public static ResponseEntity<?> convertToSpringResponse(Message<?> message) {
		return convertToSpringResponse(message, null);
	}

	public static ResponseEntity<StreamingResponseBody> convertToSpringStreamingResponse(Message<InputStream> message) {
		StreamingResponseBody response = outputStream -> {
			InputStream inputStream = message.getPayload();
			int numberOfBytesToWrite;
			byte[] data = new byte[1024];
			while ((numberOfBytesToWrite = inputStream.read(data, 0, data.length)) != -1) {
				outputStream.write(data, 0, numberOfBytesToWrite);
			}
			inputStream.close();
		};
		return (ResponseEntity<StreamingResponseBody>) convertToSpringResponse(message, response);
	}

	public static ResponseEntity<?> convertToSpringResponse(Message<?> message, StreamingResponseBody response) {
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
}
