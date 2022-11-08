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
package nl.nn.adapterframework.testtool.queues;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;

public interface Queue extends Map<String,Object> {

	void configure() throws ConfigurationException;

	void open() throws ConfigurationException;

	int executeWrite(String stepDisplayName, String fileContent, String correlationId, Map<String, Object> xsltParameters) throws TimeoutException, SenderException, ListenerException;
	String executeRead(String step, String stepDisplayName, Properties properties, String fileName, String fileContent) throws SenderException, IOException, TimeoutException, ListenerException;

}
