/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package org.frankframework.jms;

import org.frankframework.core.ListenerException;
import org.frankframework.receivers.RawMessageWrapper;

/**
 * Basic browser of JMS Messages.
 * @author  Gerrit van Brakel
 */
public class JmsBrowser<M extends jakarta.jms.Message> extends JmsMessageBrowser<M,M> {

	public JmsBrowser() {
		super();
	}

	public JmsBrowser(String selector) {
		super(selector);
	}

	@Override
	public RawMessageWrapper<M> browseMessage(String messageId) throws ListenerException {
		return new RawMessageWrapper<>(browseJmsMessage(messageId), messageId, null);
	}

}
