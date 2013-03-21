/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.ifsa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.ListenerException;

/**
 * Wrapper for messages that are not serializable.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version $Id$
 */
public class IfsaMessageWrapper implements Serializable, IMessageWrapper {

	static final long serialVersionUID = 6543734487515204545L;
	
	private HashMap context = new HashMap();
	private String text; 
	private String id; 
	
	public IfsaMessageWrapper(Object message, IListener listener) throws ListenerException  {
		super();
		text = listener.getStringFromRawMessage(message, context);
		id = listener.getIdFromRawMessage(message, context);
	}

	public Map getContext() {
		return context;
	}

	public String getId() {
		return id;
	}

	public String getText() {
		return text;
	}

}
