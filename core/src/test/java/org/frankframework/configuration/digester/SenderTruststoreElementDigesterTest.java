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
package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.core.Adapter;
import org.frankframework.core.Resource;
import org.frankframework.encryption.TruststoreConfiguration;
import org.frankframework.http.HttpSender;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

public class SenderTruststoreElementDigesterTest {

	@Test
	@DisplayName("Test that TruststoreConfiguration is properly loaded and set on the HttpSender when using the truststore element")
	void testLoadTruststoreConfiguration() throws Exception {
		// Arrange
		String configXmlFile = "Digester/Truststore/ElementConfiguration.xml";
		TestConfiguration configuration = new TestConfiguration();

		Resource resource = Resource.getResource(configXmlFile);
		ConfigurationDigester digester = SpringUtils.createBean(configuration);

		// Act
		digester.digest(resource);

		// Assert
		Adapter helloUniverse = configuration.getRegisteredAdapter("HelloUniverse");
		SenderPipe senderPipe = (SenderPipe) helloUniverse.getPipeLine().getPipe("HelloUniverseSender");
		HttpSender sender = (HttpSender) senderPipe.getSender();

		// Check that we have the 'new' TruststoreConfiguration object here for this truststore config
		TruststoreConfiguration truststoreConfiguration = sender.getTruststoreConfiguration();

		assertNotNull(truststoreConfiguration);
		assertEquals("testTruststore", truststoreConfiguration.getTruststoreResource());
	}

	@Test
	@DisplayName("Test that the truststore configuration via attribute works and is properly set on the HttpSender when using the truststore attribute")
	void testLoadTruststore() throws Exception {
		// Arrange
		String configXmlFile = "Digester/Truststore/AttributeConfiguration.xml";
		TestConfiguration configuration = new TestConfiguration();

		Resource resource = Resource.getResource(configXmlFile);
		ConfigurationDigester digester = SpringUtils.createBean(configuration);

		// Act
		digester.digest(resource);

		// Assert
		Adapter helloUniverse = configuration.getRegisteredAdapter("HelloUniverse");
		SenderPipe senderPipe = (SenderPipe) helloUniverse.getPipeLine().getPipe("HelloUniverseSender");
		HttpSender sender = (HttpSender) senderPipe.getSender();

		assertEquals("testTruststoreAttribute", sender.getTruststore());
	}
}
