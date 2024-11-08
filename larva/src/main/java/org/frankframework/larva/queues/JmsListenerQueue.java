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
package org.frankframework.larva.queues;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.NotImplementedException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.jms.PullingJmsListener;

public class JmsListenerQueue extends HashMap<String, Object> implements Queue {

	private final PullingJmsListener jmsListener;

	public JmsListenerQueue(PullingJmsListener jmsListener) {
		super();
		this.jmsListener=jmsListener;
		put("jmsListener", jmsListener);
	}

	@Override
	public void configure() throws ConfigurationException {
		jmsListener.configure();
	}

	@Override
	public void open() {
		jmsListener.start();
	}

	@Override
	public int executeWrite(String stepDisplayName, String fileContent, String correlationId, Map<String, Object> xsltParameters) {
		throw new NotImplementedException("executeWrite");
	}

	@Override
	public String executeRead(String step, String stepDisplayName, Properties properties, String fileName, String fileContent) {
		throw new NotImplementedException("executeRead");
	}

}
