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

import lombok.Getter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ListenerException;

public class RawMessageWrapper<M> {

	protected @Getter transient M rawMessage;
	protected @Getter String id;
	protected @Getter Map<String,Object> context = new LinkedHashMap<>();

	public RawMessageWrapper() {
		this(null, null);
	}

	public RawMessageWrapper(M rawMessage) {
		this(rawMessage, null);
	}

	public RawMessageWrapper(M rawMessage, String id) {
		this.rawMessage = rawMessage;
		this.id = id;
	}

	public RawMessageWrapper(M rawMessage, Map<String, Object> context, IListener<M> listener) throws ListenerException {
		this(rawMessage, listener.getIdFromRawMessage(rawMessage, context));
		this.context.putAll(context);
	}

	public RawMessageWrapper(M rawMessage, String id, Map<String, Object> context) {
		this(rawMessage, id);
		this.context.putAll(context);
	}

	@Deprecated
	public void setId(String string) {
		id = string;
	}
}
