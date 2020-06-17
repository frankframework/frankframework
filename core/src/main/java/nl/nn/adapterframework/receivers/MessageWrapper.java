/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.stream.Message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for messages that are not serializable.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class MessageWrapper implements Serializable, IMessageWrapper {

	static final long serialVersionUID = -8251009650246241025L;

	private Map context = new HashMap();
	private Message message;
	private String id;

	public MessageWrapper()  {
		super();
	}
	public MessageWrapper(Object rawMessage, IListener listener) throws ListenerException  {
		this();
		message = listener.extractMessage(rawMessage, context);
		Object rm = context.remove("originalRawMessage"); //PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
		id = listener.getIdFromRawMessage(rawMessage, context);
	}

	public Map getContext() {
		return context;
	}

	public void setId(String string) {
		id = string;
	}
	public String getId() {
		return id;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	public Message getMessage() {
		return message;
	}
}
