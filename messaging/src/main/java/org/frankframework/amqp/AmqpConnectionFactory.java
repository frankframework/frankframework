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
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.SaslOptions;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.springframework.beans.factory.DisposableBean;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringUtil;

public class AmqpConnectionFactory implements DisposableBean {

	private final String resourceName;
	private final FrankResource resource;
	private final Client client;

	public AmqpConnectionFactory(String resourceName, FrankResource resource, Client client){
		this.resourceName = resourceName;
		this.resource = resource;
		this.client = client;
	}

	public Connection connect(){
		// TODO: TODO TODO TODO TODO ActiveMQ does not allow multiple connections to same server from same client-id

		// TODO: All client options and host/port can be pre-created and cached.
		ConnectionOptions connectionOptions = new ConnectionOptions();
		CredentialFactory cf = resource.getCredentials();
		if (StringUtils.isNotEmpty(cf.getUsername()) && StringUtils.isNotEmpty(cf.getPassword())) {
			connectionOptions.user(cf.getUsername());
			connectionOptions.password(cf.getPassword());
		}
		Properties properties = resource.getProperties();
		ClassUtils.invokeSetters(connectionOptions, properties);
		ClassUtils.invokeSetters(connectionOptions.sslOptions(), properties);
		if (properties.containsKey("saslAllowedMechanisms")) {
			SaslOptions saslOptions = connectionOptions.saslOptions();
			StringUtil.split(properties.getProperty("saslAllowedMechanisms"))
					.forEach(saslOptions::addAllowedMechanism);
		}

		// TODO: Get hostname / port directly instead of from fake URL?
		String url = resource.getUrl();
		URI u;
		try {
			u = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot parse URL for AMQP resource [" + resourceName + "]", e);
		}
		String host = u.getHost();
		int port = u.getPort();
		try {
			return client.connect(host, port, connectionOptions);
		} catch (ClientException e) {
			throw new IllegalArgumentException("Cannot connect to AMQP server [" + resourceName + "]", e);
		}
	}

	@Override
	public String toString() {
		return "AmqpConnectionFactory [resourceName=" + resourceName + ", resource=" + resource + ", client=" + client;
	}

	@Override
	public void destroy() throws Exception {
		// TODO
	}
}
