/*
   Copyright 2020 Nationale-Nederlanden

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
package org.frankframework.lifecycle;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.frankframework.http.WebServiceListener;
import org.frankframework.http.cxf.NamespaceUriProvider;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.util.LogUtil;

/**
 * This bean creates an (CXF) endpoint with the /rpcrouter mapping for older SOAP based requests.
 *
 * When a {@link WebServiceListener} is registered without the `address` attribute, the listener uses
 * the `serviceNamespaceURI` or `name` attribute to register the service in the {@link ServiceDispatcher}.
 * <br/>
 * Requests that come in on this endpoint, will be dispatched to the appropriate {@link WebServiceListener} based
 * on their default namespace.
 * <br/>
 * Example: request with xmlns="urn:ws", will be dispatched to the {@link WebServiceListener} with serviceNamespaceURI="urn:ws"
 * <br/>
 * See {@link NamespaceUriProvider} for more information.
 *
 * @author Niels Meijer
 *
 */
@IbisInitializer
public class NamespaceUriProviderBean implements ApplicationContextAware, InitializingBean, DisposableBean {

	private final Logger log = LogUtil.getLogger(this);
	private ApplicationContext applicationContext;
	private EndpointImpl namespaceRouter = null;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void destroy() throws Exception {
		if(namespaceRouter != null && namespaceRouter.isPublished()) {
			namespaceRouter.stop();
		}

		namespaceRouter = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		//TODO look into NamespaceHandlerResolver
		Bus bus = (Bus) applicationContext.getBean("cxf");
		if(bus instanceof SpringBus) {
			log.debug("default CXF SpringBus [{}]", bus.getId());

			log.info("registering NamespaceURI Provider with JAX-WS CXF Dispatcher");
			namespaceRouter = new EndpointImpl(bus, new NamespaceUriProvider());
			namespaceRouter.publish("/rpcrouter");

			if(namespaceRouter.isPublished()) {
				log.info("published NamespaceURI Provider on CXF endpoint [rpcrouter] on SpringBus [{}]", namespaceRouter.getBus().getId());
			} else {
				throw new IllegalStateException("unable to NamespaceURI Service Provider on CXF endpoint [rpcrouter]");
			}
		} else {
			throw new IllegalStateException("CXF bus ["+bus+"] not instance of [SpringBus]");
		}
	}
}
