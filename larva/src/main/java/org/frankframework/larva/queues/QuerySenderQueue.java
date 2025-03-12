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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.NotImplementedException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;

public class QuerySenderQueue extends HashMap<String, Object> implements Queue {

	@Override
	public void configure() throws ConfigurationException {
		// currently handled at creation time;
	}

	@Override
	public void open() throws ConfigurationException {
		// currently handled at creation time;
	}

	@Override
	public int executeWrite(String stepDisplayName, Message fileContent, String correlationId, Map<String, Object> xsltParameters) throws TimeoutException, SenderException {
		throw new IllegalStateException("QuerySender has no 'write' step");
	}

	@Override
	public Message executeRead(String step, String stepDisplayName, Properties properties, String fileName, Message fileContent) throws SenderException, IOException, TimeoutException {
		throw new NotImplementedException("executeRead");
	}
}
