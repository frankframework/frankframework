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
import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.http.HttpSender;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

public class SenderKeystoreElementDigesterTest {

	@Test
	@DisplayName("Test that the KeystoreConfiguration is properly loaded and set on the HttpSender when using the keystoreConfiguration element")
	void testLoadKeystoreConfiguration() throws Exception {
		// Arrange
		String configXmlFile = "Digester/Keystore/ElementConfiguration.xml";
		TestConfiguration configuration = new TestConfiguration();

		Resource resource = Resource.getResource(configXmlFile);
		ConfigurationDigester digester = SpringUtils.createBean(configuration);

		// Act
		digester.digest(resource);

		// Assert
		Adapter helloUniverse = configuration.getRegisteredAdapter("HelloUniverse");
		SenderPipe senderPipe = (SenderPipe) helloUniverse.getPipeLine().getPipe("HelloUniverseSender");
		HttpSender sender = (HttpSender) senderPipe.getSender();

		// Check that we have the 'new' KeystoreConfiguration object here for this keystore config
		KeystoreConfiguration keystoreConfiguration = sender.getKeystoreConfiguration();

		assertNotNull(keystoreConfiguration);
		assertEquals("testKeystore", keystoreConfiguration.getKeystoreResource());
	}

	@Test
	@DisplayName("Test that the Keystore configuration via attributes works and is properly set on the HttpSender when using the keystore attribute")
	void testLoadKeystore() throws Exception {
		// Arrange
		String configXmlFile = "Digester/Keystore/AttributeConfiguration.xml";
		TestConfiguration configuration = new TestConfiguration();

		Resource resource = Resource.getResource(configXmlFile);
		ConfigurationDigester digester = SpringUtils.createBean(configuration);

		// Act
		digester.digest(resource);

		// Assert
		Adapter helloUniverse = configuration.getRegisteredAdapter("HelloUniverse");
		SenderPipe senderPipe = (SenderPipe) helloUniverse.getPipeLine().getPipe("HelloUniverseSender");
		HttpSender sender = (HttpSender) senderPipe.getSender();

		assertEquals("testKeystoreAttribute", sender.getKeystore());

	}
}
