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

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.ClientOptions;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.UUIDUtil;


public class AmqpConnectionFactoryFactory extends ObjectFactory<AmqpConnectionFactory, Object> {

	private final Client defaultClient;
	private final Map<String, Client> namedClients = new ConcurrentHashMap<>();

	public AmqpConnectionFactoryFactory() {
		super(null, "amqp", "AMQP 1.0");

		ClientOptions clientOptions = new ClientOptions();
		clientOptions.id(Misc.getHostname() + UUIDUtil.createRandomUUID());

		defaultClient = Client.create(clientOptions);
	}

	@Override
	protected AmqpConnectionFactory augment(Object object, String objectName) {
		if (object instanceof FrankResource resource) {
			return map(resource, objectName);
		}
		throw new IllegalArgumentException("resource ["+objectName+"] not of required type");
	}

	private AmqpConnectionFactory map(FrankResource resource, String objectName) {
		Properties props = resource.getProperties();
		String clientId = props.getProperty("clientId");
		if (StringUtils.isBlank(clientId)) {
			return new AmqpConnectionFactory(objectName, resource, defaultClient);
		}

		if (!namedClients.containsKey(clientId)) {
			ClientOptions clientOptions = new ClientOptions();
			clientOptions.id(clientId);
			namedClients.put(clientId, Client.create(clientOptions));
		}
		return new AmqpConnectionFactory(objectName, resource, namedClients.get(clientId));
	}


	public AmqpConnectionFactory getConnectionFactory(String name) {
		return get(name, null);
	}

	@Override
	protected void postDestroy() throws Exception {
		CloseUtils.closeSilently(defaultClient);
		CloseUtils.closeSilently(namedClients.values());
		namedClients.clear();
		super.postDestroy();
	}
}
