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
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class RawMessageWrapper<M> {

	protected @Getter M rawMessage;
	protected @Getter String id;
	protected @Getter String correlationId;
	protected @Getter Map<String,Object> context = new LinkedHashMap<>();

	protected RawMessageWrapper() {
		// For Serialisation
		this(null, null, null);
	}

	public RawMessageWrapper(M rawMessage) {
		this(rawMessage, null, null);
	}

	public RawMessageWrapper(M rawMessage, String id, String correlationId) {
		this.rawMessage = rawMessage;
		this.id = id;
		this.correlationId = correlationId;
		if (id != null) {
			this.context.put(PipeLineSession.messageIdKey, id);
		}
		if (correlationId != null) {
			this.context.put(PipeLineSession.correlationIdKey, correlationId);
		}
	}

	public RawMessageWrapper(M rawMessage, String id, String correlationId, @Nonnull Map<String, Object> context) {
		this(rawMessage, id, correlationId);
		this.context.putAll(context);
		if (context.get(PipeLineSession.messageIdKey) != null) {
			this.id = (String) context.get(PipeLineSession.messageIdKey);
		}
		if (context.get(PipeLineSession.correlationIdKey) != null) {
			this.correlationId = (String) context.get(PipeLineSession.correlationIdKey);
		}
	}

	protected void updateOrRemoveValue(String key, String value) {
		if (value != null) {
			this.context.put(key, value);
		} else {
			this.context.remove(key);
		}
	}

	public Message getMessage() {
		Message message = Message.asMessage(rawMessage);
		message.getContext().putAll(this.context);
		return message;
	}
}
