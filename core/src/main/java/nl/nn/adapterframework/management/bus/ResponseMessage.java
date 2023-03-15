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
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.nn.adapterframework.core.Resource;

public class ResponseMessage {
	public static final String STATUS_KEY = "meta-status";
	public static final String MIMETYPE_KEY = "meta-type";
	public static final String CONTENT_DISPOSITION_KEY = "meta-contentdisposition";

	public static class Builder {
		private Object payload;
		private Map<String, Object> headers = new HashMap<>();

		private Builder() {
			headers.put(STATUS_KEY, 200);
		}

		public static Builder create() {
			return new Builder();
		}

		public Builder withPayload(Object payload) {
			this.payload = payload;
			return this;
		}

		public Builder withStatus(int status) {
			headers.put(STATUS_KEY, status);
			return this;
		}

		public Builder withFilename(String filename) {
			headers.put(CONTENT_DISPOSITION_KEY, "attachment; filename=\""+filename+"\"");
			return this;
		}

		public Builder withMimeType(MimeType mimeType) {
			if(mimeType != null) {
				headers.put(MIMETYPE_KEY, mimeType.toString());
			}
			return this;
		}

		public Builder setHeader(String key, String value) {
			headers.put(key, value);
			return this;
		}

		public Message<String> toJson() {
			String json = null;
			if(payload instanceof nl.nn.adapterframework.stream.Message) {
				try {
					String stringInput = ((nl.nn.adapterframework.stream.Message) payload).asString();
					json = toJson(stringInput);
				} catch (IOException e) {
					throw new BusException("unable to convert payload to message", e);
				}
			} else {
				json = toJson(payload);
			}
			headers.put(MIMETYPE_KEY, MediaType.APPLICATION_JSON_VALUE);
			return new GenericMessage<>(json, headers);
		}

		private static String toJson(Object payload) {
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				return objectMapper.writeValueAsString(payload);
			} catch (JacksonException e) {
				throw new BusException("unable to convert response to JSON", e);
			}
		}

		public Message<Object> raw() {
			if(payload instanceof nl.nn.adapterframework.stream.Message) {
				nl.nn.adapterframework.stream.Message message = (nl.nn.adapterframework.stream.Message) payload;
				Map<String, Object> messageContext = message.getContext();
				this.headers.putAll(messageContext);
				try {
					if(message.isBinary()) {
						return new GenericMessage<>(message.asInputStream(), headers);
					}
					return new GenericMessage<>(message.asString(), headers);
				} catch (IOException e) {
					throw new BusException("unable to convert payload to Message", e);
				}
			} else if(payload instanceof Resource) {
				try {
					return new GenericMessage<>(((Resource)payload).openStream(), headers);
				} catch (IOException e) {
					throw new BusException("unable to convert payload to Message", e);
				}
			}
			headers.computeIfAbsent(MIMETYPE_KEY, e->MediaType.APPLICATION_OCTET_STREAM_VALUE);
			return new GenericMessage<>(payload, headers);
		}
	}

	/** Payload is converted to JSON + status code 200 */
	public static Message<String> ok(Object payload) {
		return Builder.create().withPayload(payload).toJson();
	}

	private static Message<String> createNoContentResponse(int statuscode) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, statuscode);
		return new GenericMessage<>("no-content", headers);
	}

	public static Message<String> created() {
		return createNoContentResponse(201);
	}

	public static Message<String> accepted() {
		return createNoContentResponse(202);
	}

	public static Message<String> noContent() {
		return createNoContentResponse(204);
	}
}
