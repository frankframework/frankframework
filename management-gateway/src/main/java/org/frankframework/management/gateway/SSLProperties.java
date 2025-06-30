/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.management.gateway;

import java.net.URI;

import lombok.Getter;

@Getter
public class SSLProperties {

	private final String keyStore;
	private final String keyStorePassword;
	private final String trustStore;
	private final String trustStorePassword;
	private final String switchboardDomain;
	private final String switchboardWebsocketPort;
	private final String switchboardApiPort;

	public SSLProperties() {
		this.keyStore = System.getProperty("client.ssl.key-store");
		this.keyStorePassword = System.getProperty("client.ssl.key-store-password");
		this.trustStore = System.getProperty("client.ssl.trust-store");
		this.trustStorePassword = System.getProperty("client.ssl.trust-store-password");
		this.switchboardDomain = System.getProperty("client.switchboard.host", "localhost");
		this.switchboardWebsocketPort = System.getProperty("client.switchboard.websocket.port", "8443");
		this.switchboardApiPort = System.getProperty("client.switchboard.api.port", "8081");
	}

	public URI getWebsocketUri() {
		return URI.create("wss://" + switchboardDomain + ":" + switchboardWebsocketPort + "/ws");
	}

	public URI getApiUri() {
		return URI.create("https://" + switchboardDomain + ":" + switchboardWebsocketPort);
	}
}
