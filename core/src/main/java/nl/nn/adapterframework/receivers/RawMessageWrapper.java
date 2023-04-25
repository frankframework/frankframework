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
package nl.nn.adapterframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.Getter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.stream.Message;

public class RawMessageWrapper<M> {

	protected @Getter M rawMessage;
	protected @Getter String id;
	protected @Getter Map<String,Object> context = new LinkedHashMap<>();
	@Getter protected String correlationId;

	public RawMessageWrapper() {
		this(null, null, (String) null);
	}

	public RawMessageWrapper(M rawMessage) {
		this(rawMessage, null, (String) null);
	}

	public RawMessageWrapper(M rawMessage, String id, String correlationId) {
		this.rawMessage = rawMessage;
		this.id = id;
		this.correlationId = correlationId;
		if (id != null) {
			this.context.put("mid", id);
		}
		if (correlationId != null) {
			this.context.put("cid", correlationId);
		}
	}

	public RawMessageWrapper(M rawMessage, @Nonnull Map<String, Object> context, @Nonnull IListener<M> listener) throws ListenerException {
		// ILister.getIdFromRawMessage() may extract the correlation-id and add it into the context.
		this(rawMessage, listener.getIdFromRawMessage(rawMessage, context), (String) context.get("cid"), context);
	}

	public RawMessageWrapper(M rawMessage, String id, String correlationId, Map<String, Object> context) {
		this(rawMessage, id, correlationId);
		this.context.putAll(context);
	}

	@Deprecated
	public void setId(String string) {
		id = string;
	}

	@Deprecated
	void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public Message getMessage() {
		Message message = Message.asMessage(rawMessage);
		message.getContext().putAll(this.context);
		return message;
	}
}
