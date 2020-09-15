/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.MBeanExporter;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

/**
 * This implementation of {@link AdapterService} also registers the adapters to a JMX server, and configures the registered Adapters.

 * @author Niels Meijer
 * @since 5.0.29
 */
public class BasicAdapterServiceImpl extends AdapterServiceImpl implements ApplicationContextAware, InitializingBean {

	private final Logger log = LogUtil.getLogger(BasicAdapterServiceImpl.class);
	private MBeanExporter mBeanManager = null;
	private static Map<IAdapter, ObjectName> registeredAdapters = new HashMap<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		if(mBeanManager == null) {
			throw new BeanCreationException("unable to load JmxMBeanManager");
		}
	}

	@Override
	public void registerAdapter(IAdapter adapter) throws ConfigurationException {
		super.registerAdapter(adapter);

		log.debug("Registering adapter [" + adapter.getName() + "] to the JMX server");
		synchronized(registeredAdapters) {
			ObjectName name = mBeanManager.registerManagedResource(adapter);
			registeredAdapters.put(adapter, name);
		}
		log.info("[" + adapter.getName() + "] registered to the JMX server");

		adapter.configure();
	}

	@Override
	public void unRegisterAdapter(IAdapter adapter) {
		super.unRegisterAdapter(adapter);

		synchronized(registeredAdapters) {
			ObjectName name = registeredAdapters.remove(adapter);
			mBeanManager.unregisterManagedResource(name);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		mBeanManager = applicationContext.getBean("MBeanManager", MBeanExporter.class);
	}
}
