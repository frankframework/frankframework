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
package nl.nn.adapterframework.extensions.cmis.servlets;

import java.util.HashMap;

import javax.servlet.ServletContext;

import org.apache.chemistry.opencmis.commons.impl.ClassLoaderUtil;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Autowires the CMIS RepositoryConnectorFactory
 *
 * @author Niels Meijer
 *
 */
@IbisInitializer
public class CmisLifecycleBean implements ServletContextAware, InitializingBean, DisposableBean {

	private Logger log = LogUtil.getLogger(this);
	private ServletContext servletContext;
	private CmisServiceFactory factory;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void destroy() throws Exception {
		if (factory != null) {
			factory.destroy();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (factory == null && servletContext != null) {
			factory = createServiceFactory();
			factory.init(new HashMap<>());
			String key = CmisRepositoryContextListener.SERVICES_FACTORY;
			servletContext.setAttribute(key, factory);

			if(log.isDebugEnabled()) log.debug("created and stored CmisServiceFactory in ServletContext under key ["+key+"]");
		}
		else {
			log.error("failed to initialize ["+this.getClass().getSimpleName()+"]");
		}
	}

	/**
	 * Creates a service factory.
	 */
	private CmisServiceFactory createServiceFactory() {
		String className = "nl.nn.adapterframework.extensions.cmis.server.RepositoryConnectorFactory";

		// create a factory instance
		Object object = null;
		try {
			object = ClassLoaderUtil.loadClass(className).newInstance();
		} catch (Exception e) {
			log.warn("Could not create a services factory instance", e);
			return null;
		}

		if (!(object instanceof CmisServiceFactory)) {
			log.warn("The provided class is not an instance of CmisServiceFactory!");
		}

		return (CmisServiceFactory) object;
	}
}
