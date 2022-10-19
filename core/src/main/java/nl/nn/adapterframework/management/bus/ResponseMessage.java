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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseMessage {
	public static final String STATUS_KEY = "meta:status";
	public static final String MIMETYPE_KEY = "meta:type";
	public static final String CONTENT_DISPOSITION_KEY = "meta:contentdisposition";
	public static final String NO_CONTENT_PAYLOAD = "no-content";

	public class Headers {
		private int status = 200;

		public Headers withStatus(int status) {
			this.status = status;
			return this;
		}
	}

	public static Message<String> ok(String payload) {
		return ok(payload, null);
	}

	public static Message<String> ok(String payload, MimeType mediaType) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 200);
		if(mediaType != null) {
			headers.put(MIMETYPE_KEY, mediaType.toString());
		}
		return new GenericMessage<>(payload, headers);
	}

	public static Message<InputStream> ok(InputStream stream, MimeType mediaType) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 200);
		if(mediaType != null) {
			headers.put(MIMETYPE_KEY, mediaType.toString());
		}
		return new GenericMessage<>(stream, headers);
	}

	public static Message<Object> ok(nl.nn.adapterframework.stream.Message message) throws IOException {
		Map<String, Object> headers = message.getContext();
		if(message.isBinary()) {
			return new GenericMessage<>(message.asInputStream(), headers);
		}
		return new GenericMessage<>(message.asString(), headers);
	}

	public static Message<String> noContent() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 204);
		return new GenericMessage<>(NO_CONTENT_PAYLOAD, headers);
	}

	public static Message<String> accepted() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 202);
		return new GenericMessage<>(NO_CONTENT_PAYLOAD, headers);
	}

	public static Message<String> badRequest() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 400);
		return new GenericMessage<>(NO_CONTENT_PAYLOAD, headers);
	}

	// This method should be removed at some point, every argument should/must be a DOA and not a map...
	public static Message<String> ok(Object configs) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String result = objectMapper.writeValueAsString(configs);

			Map<String, Object> headers = new HashMap<>();
			headers.put(STATUS_KEY, 200);
			headers.put(MIMETYPE_KEY, MediaType.APPLICATION_JSON.toString());
			return new GenericMessage<>(result, headers);
		} catch (JacksonException e) {
			throw new IllegalStateException();
		}
	}
}
