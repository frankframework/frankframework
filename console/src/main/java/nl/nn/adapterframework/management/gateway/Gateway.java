/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.gateway;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import lombok.Setter;
import nl.nn.adapterframework.management.bus.BusException;

/**
 * A Spring Integration Gateway in it's most simplistic form.
 * Put's messages on their respective Channels.
 */
public class Gateway<T> implements InitializingBean, IntegrationGateway<T>, ApplicationContextAware {

	private IntegrationGateway<T> gateway = null;
	private @Setter ApplicationContext applicationContext;

	@Override
	public Message<T> sendSyncMessage(Message<T> in) {
		if(gateway == null) {
			throw new BusException("no proxy gateway defined");
		}

		return gateway.sendSyncMessage(in);
	}

	@Override
	public void sendAsyncMessage(Message<T> in) {
		if(gateway == null) {
			throw new BusException("no proxy gateway defined");
		}

		gateway.sendAsyncMessage(in);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {
//		gateway = (IntegrationGateway<T>) SpringUtils.createBean(applicationContext, IntegrationGateway.class);
		gateway = applicationContext.getBean("HttpOutboundGateway", IntegrationGateway.class);
//		gateway = applicationContext.getBean("LocalGateway", IntegrationGateway.class);
	}
}