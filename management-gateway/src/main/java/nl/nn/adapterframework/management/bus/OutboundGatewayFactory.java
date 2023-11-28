/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.management.bus;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.util.ClassUtils;

import lombok.Setter;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Allows the creation of outbound integration gateways.
 */
public class OutboundGatewayFactory<T> implements InitializingBean, ApplicationContextAware, FactoryBean<OutboundGateway<T>> {

	private final Logger log = LogManager.getLogger(this);
	private @Setter ApplicationContext applicationContext;
	private OutboundGateway<T> gateway;

	private static final String GATEWAY_CLASS_KEY = "management.gateway.outbound.class";
	private @Setter String gatewayClassname = null;

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.isBlank(gatewayClassname)) {
			throw new IllegalStateException("no outbound gateway class specified. Please set ["+GATEWAY_CLASS_KEY+"]");
		}
		log.info("attempting to initialize using gateway class [{}]", gatewayClassname);

		Class<?> gatewayClass = ClassUtils.resolveClassName(gatewayClassname, applicationContext.getClassLoader());

		if(!OutboundGateway.class.isAssignableFrom(gatewayClass)) {
			throw new IllegalArgumentException("gateway ["+gatewayClassname+"] does not implement type IntegrationGateway");
		}

		gateway = (OutboundGateway<T>) SpringUtils.createBean(applicationContext, gatewayClass);
		IntegrationPatternType type = gateway.getIntegrationPatternType();
		if(IntegrationPatternType.outbound_gateway != type) {
			throw new IllegalArgumentException("gateway ["+gatewayClassname+"] must be of an Outbound Gateway");
		}

		log.info("created gateway [{}]", gateway);
	}

	@Override
	public OutboundGateway<T> getObject() {
		return gateway;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends OutboundGateway> getObjectType() {
		return (this.gateway != null ? this.gateway.getClass() : OutboundGateway.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
