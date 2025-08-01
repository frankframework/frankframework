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
package org.frankframework.amqp;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.exceptions.ClientException;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.CredentialFactory;


public class AmqpConnectionFactory extends ObjectFactory<Connection, Object> {

	public AmqpConnectionFactory() {
		super(null, "amqp", "AMQP 1.0");
	}

	@Override
	protected Connection augment(Object object, String objectName) {
		if (object instanceof Connection connection) {
			return connection;
		}
		if (object instanceof FrankResource resource) {
			return map(resource, objectName);
		}
		throw new IllegalArgumentException("resource ["+objectName+"] not of required type");
	}

	private Connection map(FrankResource resource, String name) {
		Client client = Client.create();

		ConnectionOptions connectionOptions = new ConnectionOptions();
		CredentialFactory cf = resource.getCredentials();
		if (StringUtils.isNotEmpty(cf.getUsername()) && StringUtils.isNotEmpty(cf.getPassword())) {
			connectionOptions.user(cf.getUsername());
			connectionOptions.password(cf.getPassword());
		}
		// TODO: Many more options, including SASL / SSL

		// TODO: Get hostname / port directly instead of from fake URL?
		String url = resource.getUrl();
		URI u;
		try {
			u = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot parse URL for AMQP resource [" + name + "]", e);
		}
		String host = u.getHost();
		int port = u.getPort();
		try {
			return client.connect(host, port, connectionOptions);
		} catch (ClientException e) {
			throw new IllegalArgumentException("Cannot connect to AMQP server [" + name + "]", e);
		}
	}

	@Override
	protected void destroyObject(Connection object) {
		object.close();
	}

	public Connection getConnection(String name) {
		return get(name, null);
	}
}
