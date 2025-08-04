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

import com.swiftmq.amqp.AMQPContext;
import com.swiftmq.amqp.v100.client.Connection;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;


public class SwiftAmqpConnectionFactory extends ObjectFactory<Connection, Object> {

	AMQPContext context = new AMQPContext(AMQPContext.CLIENT);

	public SwiftAmqpConnectionFactory() {
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
		String useSaslStr = resource.getProperties().getProperty("sasl");
		boolean useSasl = (useSaslStr != null && useSaslStr.startsWith("!")) ? !Boolean.parseBoolean(useSaslStr.substring(1)): Boolean.parseBoolean(useSaslStr);


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

		Connection connection;
		CredentialFactory cf = resource.getCredentials();
		if (StringUtils.isNotEmpty(cf.getUsername()) && StringUtils.isNotEmpty(cf.getPassword())) {
			connection = new Connection(context, host, port, cf.getUsername(), cf.getPassword());
		} else {
			connection = new Connection(context, host, port, true);
		}
		ClassUtils.invokeSetters(connection, resource.getProperties());
		return connection;
	}

	public Connection getConnection(String name) {
		return get(name, null);
	}
}
