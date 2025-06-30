/*
   Copyright 2025 WeAreFrank!

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

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NonNull;

public class GenericMessage<T> implements Message<T> {
	private final T payload;
	private final Map<String, Object> headersMap;

	@JsonCreator
	public GenericMessage(
			@JsonProperty("payload") T payload,
			@JsonProperty("headers") Map<String, Object> headersMap
	) {
		this.payload = payload;
		this.headersMap = headersMap != null ? headersMap : Map.of();
	}

	@Override
	public @NonNull T getPayload() {
		return payload;
	}

	@Override
	public @NonNull MessageHeaders getHeaders() {
		// Lazily wrap the raw map into Spring’s MessageHeaders
		return new MessageHeaders(headersMap);
	}
}
