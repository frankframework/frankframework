/*
   Copyright 2023 - 2024 WeAreFrank!

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
package org.frankframework.management.bus;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.util.ClassUtils;

import lombok.Setter;

import org.frankframework.util.SpringUtils;

/**
 * Allows the creation of inbound integration gateways.
 * Not a factory bean, as multiple inbound gateways may exist.
 * Does find all 'gateways', creates them and registers them as singletons in the application.
 */
public class InboundGatewayFactory implements InitializingBean, ApplicationContextAware {

	private final Logger log = LogManager.getLogger(this);
	private @Setter ApplicationContext applicationContext;

	private @Setter String gatewayClassnames;

	@Override
	public void afterPropertiesSet() throws Exception {
		Set<String> gateways = getInboundGateways();
		if(gateways.isEmpty()) {
			log.info("did not find any inbound gateways to initialize");
			return;
		}

		log.info("found the following inbound gateways {}", gateways::toString);

		for(String gateway : gateways) {
			try {
				IntegrationPattern ipGw = createGateway(gateway);
				ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
				beanFactory.registerSingleton(gateway, ipGw);

				log.info("created inbound gateway [{}]", gateway);
			} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
				log.warn("unable to create inbound gateway [{}]", gateway, e);
			}
		}
	}

	/**
	 * It is possible multiple inbound gateways are specified using a comma separated list.
	 * Also always adds the local bus for internal traffic as well as the Frank!Console backend.
	 */
	private Set<String> getInboundGateways() {
		if(StringUtils.isBlank(gatewayClassnames)) {
			return Collections.emptySet();
		}

		// Ensure an unique list of gateways.
		Set<String> gateways = new TreeSet<>(Arrays.asList(gatewayClassnames.split(",")));
		return Collections.unmodifiableSet(gateways);
	}

	/**
	 * Creates the actual gateway through Spring, and ensures it's of the required 'inbound' type.
	 */
	private IntegrationPattern createGateway(String gatewayClassname) {
		Class<?> gatewayClass = ClassUtils.resolveClassName(gatewayClassname, applicationContext.getClassLoader());
		if(!IntegrationPattern.class.isAssignableFrom(gatewayClass)) {
			throw new IllegalArgumentException("gateway ["+gatewayClassname+"] does not implement type IntegrationPattern");
		}

		IntegrationPattern gateway = (IntegrationPattern) SpringUtils.createBean(applicationContext, gatewayClass);

		IntegrationPatternType type = gateway.getIntegrationPatternType();
		if(IntegrationPatternType.inbound_gateway == type) {
			return gateway;
		}

		applicationContext.getAutowireCapableBeanFactory().destroyBean(gateway);
		throw new IllegalArgumentException("gateway ["+gatewayClassname+"] must be of an Inbound Gateway");
	}
}
