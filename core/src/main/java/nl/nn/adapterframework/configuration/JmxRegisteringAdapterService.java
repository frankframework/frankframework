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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.MBeanExporter;

import nl.nn.adapterframework.core.IAdapter;

/**
 * This implementation of {@link IAdapterService} registers the adapters to a JMX server.

 * @author Niels Meijer
 */
public class JmxRegisteringAdapterService extends AdapterService implements InitializingBean {

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
	}

	@Override
	public void unRegisterAdapter(IAdapter adapter) {
		super.unRegisterAdapter(adapter);

		synchronized(registeredAdapters) {
			ObjectName name = registeredAdapters.remove(adapter);
			mBeanManager.unregisterManagedResource(name);
		}
	}

	@Autowired
	@Qualifier("MBeanManager")
	public void setMBeanManager(MBeanExporter mBeanManager) {
		this.mBeanManager = mBeanManager;
	}
}
