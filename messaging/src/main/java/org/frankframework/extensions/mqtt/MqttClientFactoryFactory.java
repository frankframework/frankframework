/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.extensions.mqtt;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;

public class MqttClientFactoryFactory extends ObjectFactory<MqttClientFactory, Object> {

	public MqttClientFactoryFactory() {
		super(null, "mqtt", "MQTT");
	}

	@Override
	protected MqttClientFactory augment(Object object, String objectName) {
		if (object instanceof MqttClientFactory clientFactory) {
			return clientFactory;
		}
		if (object instanceof FrankResource resource) {
			return new MqttClientFactory(objectName, resource);
		}
		throw new IllegalArgumentException("resource ["+objectName+"] not of required type");
	}

	public MqttClientFactory getClientFactory(String name) {
		return get(name, null);
	}

	@Override
	protected void destroyObject(MqttClientFactory object) throws Exception {
		object.destroy();
	}
}
