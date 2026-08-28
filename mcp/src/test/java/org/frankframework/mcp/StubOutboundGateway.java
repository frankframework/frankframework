/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.StringMessage;

/**
 * A simple {@link OutboundGateway} test double that records the last message it received and returns a canned response,
 * so tools can be tested without a running Frank!Framework.
 */
public class StubOutboundGateway implements OutboundGateway {

	private Message<?> lastSentMessage;
	private boolean lastCallWasAsync;
	private String responsePayload = "OK";
	private final Map<String, Object> responseHeaders = new HashMap<>();
	private List<ClusterMember> members = new ArrayList<>();

	@Override
	@NonNull
	@SuppressWarnings("unchecked")
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		this.lastSentMessage = in;
		this.lastCallWasAsync = false;
		if (responseHeaders.isEmpty()) {
			return (Message<O>) new StringMessage(responsePayload);
		}

		Map<String, Object> headers = new HashMap<>();
		headers.put(BusMessageUtils.HEADER_PREFIX + "type", MediaType.TEXT_PLAIN_VALUE);
		responseHeaders.forEach((key, value) -> headers.put(BusMessageUtils.HEADER_PREFIX + key, value));
		return (Message<O>) new GenericMessage<>(responsePayload, headers);
	}

	@Override
	public <I> void sendAsyncMessage(Message<I> in) {
		this.lastSentMessage = in;
		this.lastCallWasAsync = true;
	}

	@Override
	@NonNull
	public List<ClusterMember> getMembers() {
		return members;
	}

	public Message<?> getLastSentMessage() {
		return lastSentMessage;
	}

	public boolean wasLastCallAsync() {
		return lastCallWasAsync;
	}

	public void setResponsePayload(String responsePayload) {
		this.responsePayload = responsePayload;
	}

	public void setResponseHeader(String key, Object value) {
		this.responseHeaders.put(key, value);
	}

	public void setMembers(List<ClusterMember> members) {
		this.members = members;
	}
}
