/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.jmx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.MBeanExporter;

import org.frankframework.configuration.AbstractAdapterLifecycleWrapper;
import org.frankframework.core.Adapter;

/**
 * NOTE: Using the PlatformMBeanServer on WebSphere changes ObjectNames on registration.
 */
public class JmxAdapterWrapper extends AbstractAdapterLifecycleWrapper implements InitializingBean {

	private MBeanExporter mBeanManager = null;
	private static final Map<Adapter, ObjectName> registeredAdapters = new HashMap<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		if(mBeanManager == null) {
			throw new BeanCreationException("unable to load JmxMBeanManager");
		}
	}

	@Override
	public void addAdapter(Adapter adapter) {
		log.debug("registering adapter [{}] to the JMX server", adapter.getName());
		synchronized(registeredAdapters) {
			ObjectName name = mBeanManager.registerManagedResource(adapter);
			registeredAdapters.put(adapter, name);
			log.info("adapter [{}] objectName [{}] registered to the JMX server", adapter.getName(), name);
		}
	}


	@Override
	public void removeAdapter(Adapter adapter) {
		synchronized(registeredAdapters) {
			ObjectName name = registeredAdapters.remove(adapter);
			if(name == null) {
				return; //return quietly apparently we never registered this adapter on the JMX agent?
			}

			if(!mBeanManager.getServer().isRegistered(name)) {
				log.debug("unable to locate the registered MBean [{}] on the JMX server, try to query and manually unregister it", name);
				for(ObjectName mbean : queryMBean(name)) {
					manuallyRemoveMBean(mbean);
				}
			}

			mBeanManager.unregisterManagedResource(name);
		}
	}

	private void manuallyRemoveMBean(ObjectName objectName) {
		if(mBeanManager.getServer().isRegistered(objectName)) {
			try {
				mBeanManager.getServer().unregisterMBean(objectName);
			} catch (MBeanRegistrationException e) {
				log.warn("unable to unregister mbean [{}]", objectName, e);
			} catch (InstanceNotFoundException e) { //We just checked to see if the bean exists.
				log.debug("mbean [{}] not found", objectName, e);
			}
		} else if(log.isInfoEnabled()) {
			log.info("cannot find mbean [{}] unable to unregister", objectName);
		}
	}

	private Set<ObjectName> queryMBean(ObjectName name) {
		String jmxQuery = "%s,*".formatted(name.getCanonicalName());
		try {
			ObjectName queryObject = new ObjectName(jmxQuery);
			Set<ObjectName> result = mBeanManager.getServer().queryNames(queryObject, null);
			if(result.isEmpty()) {
				log.warn("mbean query [{}] returned 0 results", jmxQuery);
			}
			if(result.size() > 1) {
				log.warn("mbean query returned multiple results {}", result);
			}
			return result;
		} catch (MalformedObjectNameException e) {
			log.warn("error parsing JMX query [{}]", jmxQuery, e);
		} catch(RuntimeOperationsException e) {
			log.error("error querying mBeanServer, query [{}]", jmxQuery, e);
		}

		return Collections.emptySet();
	}

	@Qualifier("MBeanManager")
	public void setMBeanManager(MBeanExporter mBeanManager) {
		this.mBeanManager = mBeanManager;
	}
}
