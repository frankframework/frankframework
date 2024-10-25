/*
   Copyright 2019 Nationale-Nederlanden

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
package org.frankframework.extensions.cmis.servlets;

import java.util.HashMap;

import jakarta.servlet.ServletContext;

import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import org.frankframework.extensions.cmis.server.RepositoryConnectorFactory;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;

/**
 * Autowires the CMIS RepositoryConnectorFactory
 *
 * @author Niels Meijer
 *
 */
@IbisInitializer
public class CmisLifecycleBean implements ServletContextAware, InitializingBean, DisposableBean {

	private final Logger log = LogUtil.getLogger(this);
	private ServletContext servletContext;
	private CmisServiceFactory factory;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void destroy() {
		if (factory != null) {
			factory.destroy();
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (factory == null && servletContext != null) {
			factory = createServiceFactory();
			factory.init(new HashMap<>());
			String key = CmisRepositoryContextListener.SERVICES_FACTORY;
			servletContext.setAttribute(key, factory);

			log.debug("created and stored CmisServiceFactory in ServletContext under key [{}]", key);
		}
		else {
			log.error("failed to initialize CMIS RepositoryConnector [{}]", RepositoryConnectorFactory.class::getSimpleName);
		}
	}

	/**
	 * Creates a service factory.
	 */
	private CmisServiceFactory createServiceFactory() {
		// create a factory instance
		try {
			return ClassUtils.newInstance(RepositoryConnectorFactory.class);
		} catch (Exception e) {
			throw new IllegalStateException("could not create a CMIS RepositoryConnector", e);
		}
	}
}
