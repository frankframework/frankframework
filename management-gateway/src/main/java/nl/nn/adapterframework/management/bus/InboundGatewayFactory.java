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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.util.ClassUtils;

import lombok.Setter;

/**
 * Allows the creation of inbound integration gateways.
 */
public class InboundGatewayFactory implements InitializingBean, ApplicationContextAware, FactoryBean<IntegrationPattern> {

	private Logger log = LogManager.getLogger(this);
	private @Setter ApplicationContext applicationContext;
	private IntegrationPattern gateway;

	private @Setter String gatewayClassname = LocalGateway.class.getCanonicalName();

	@Override
	public void afterPropertiesSet() throws Exception {
		Class<?> gatewayClass = ClassUtils.resolveClassName(gatewayClassname, applicationContext.getClassLoader());
		if(!IntegrationPattern.class.isAssignableFrom(gatewayClass)) {
			throw new IllegalArgumentException("gateway ["+gatewayClassname+"] does not implement type IntegrationPattern");
		}

		gateway = (IntegrationPattern) applicationContext.getAutowireCapableBeanFactory().createBean(gatewayClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

		IntegrationPatternType type = gateway.getIntegrationPatternType();
		if(IntegrationPatternType.inbound_gateway != type) {
			throw new IllegalArgumentException("gateway ["+gatewayClassname+"] must be of an Inbound Gateway");
		}

		log.info("created gateway [{}]", gateway);
	}

	@Override
	public IntegrationPattern getObject() {
		return gateway;
	}

	@Override
	public Class<? extends IntegrationPattern> getObjectType() {
		return (this.gateway != null ? this.gateway.getClass() : IntegrationPattern.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
