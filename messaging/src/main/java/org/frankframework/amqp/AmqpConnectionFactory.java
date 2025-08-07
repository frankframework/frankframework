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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.SaslOptions;
import org.apache.qpid.protonj2.client.Session;
import org.apache.qpid.protonj2.client.SessionOptions;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.springframework.beans.factory.DisposableBean;

import lombok.extern.log4j.Log4j2;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringUtil;

@Log4j2
public class AmqpConnectionFactory implements DisposableBean {

	private final String resourceName;
	private final FrankResource resource;
	private final Client client;

	private Connection connection;
	private final AtomicInteger borrowCount  = new AtomicInteger(0);

	public AmqpConnectionFactory(String resourceName, FrankResource resource, Client client){
		this.resourceName = resourceName;
		this.resource = resource;
		this.client = client;
	}

	public Connection getConnection() throws ClientException {
		// Create and cache a single connection, because ActiveMQ does not allow multiple connections from
		// same host on same clientId.
		// This also means we should wrap it and not allow closing it.
		synchronized (this) {
			if (connection == null) {
				borrowCount.set(0);
				connection = connect();
			}
		}
		return wrap(connection);
	}

	public Session getSession(SessionOptions sessionOptions) throws ClientException {
		try (Connection conn = getConnection()) {
			return conn.openSession(sessionOptions);
		}
	}

	private Connection connect() throws ClientException {
		// TODO: All client options and host/port can be pre-created and cached.
		ConnectionOptions connectionOptions = new ConnectionOptions();
		connectionOptions.reconnectEnabled(true);

		connectionOptions.disconnectedHandler((conn, disconnectionEvent) -> {
			log.warn(() -> "Disconnected from AMQP connection to [%s:%d]:".formatted(disconnectionEvent.host(), disconnectionEvent.port()), disconnectionEvent.failureCause());

			conn.closeAsync();

			// NULL the cached connection so that next time anyone requests a connection, a new
			// one will be created
			synchronized (AmqpConnectionFactory.this) {
				if (conn == AmqpConnectionFactory.this.connection) {
					AmqpConnectionFactory.this.connection = null;
				}
			}
		});

		CredentialFactory cf = resource.getCredentials();
		if (StringUtils.isNotEmpty(cf.getUsername()) && StringUtils.isNotEmpty(cf.getPassword())) {
			connectionOptions.user(cf.getUsername());
			connectionOptions.password(cf.getPassword());
		}
		Properties properties = resource.getProperties();
		ClassUtils.invokeSetters(connectionOptions, properties);
		ClassUtils.invokeSetters(connectionOptions.reconnectOptions(), properties);
		ClassUtils.invokeSetters(connectionOptions.transportOptions(), properties);
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
		return client.connect(host, port, connectionOptions);
	}

	@Override
	public String toString() {
		return "AmqpConnectionFactory [resourceName=" + resourceName + ", resource=" + resource + ", client=" + client;
	}

	@Override
	public void destroy() {
		int count = borrowCount.get();
		if (count > 0) {
			log.warn("{} Unclosed connections still outstanding", count);
		}
		CloseUtils.closeSilently(connection);
	}

	private Connection wrap(Connection conn) {
		return (Connection) Proxy.newProxyInstance(
				conn.getClass().getClassLoader(),
				conn.getClass().getInterfaces(),
				new NonClosingProxyInvocationHandler(conn)
		);
	}

	class NonClosingProxyInvocationHandler implements InvocationHandler {
		private final Object target;
		private boolean closed = false;

		public NonClosingProxyInvocationHandler(Object target) {
			this.target = target;
			borrowCount.incrementAndGet();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("close") || method.getName().equals("closeAsync")) {
				if (!closed) {
					borrowCount.decrementAndGet();
					closed = true;
				}
				return null;
			}
			return method.invoke(target, args);
		}
	}
}
