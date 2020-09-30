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
package nl.nn.adapterframework.jmx;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.naming.IdentityNamingStrategy;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

public class JmxNamingStrategy extends IdentityNamingStrategy implements InitializingBean {

	private final Logger log = LogUtil.getLogger(this);
	private String jmxDomain = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.isEmpty(jmxDomain)) {
			throw new BeanCreationException("domain may not be null");
		}
	}

	public void setDefaultDomain(String defaultDomain) {
		this.jmxDomain = defaultDomain;
	}

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		Hashtable<String, String> properties = new Hashtable<>();

		if(managedBean == null) {
			throw new MalformedObjectNameException("managedBean cannot be null");
		}

		if(managedBean instanceof IAdapter) {
			IAdapter adapter = (IAdapter) managedBean;
			Configuration config = adapter.getConfiguration();
			if (config != null) {
				String version = null;
				if (StringUtils.isNotEmpty(config.getVersion())) {
					version = config.getVersion();
				} else {
					version = Integer.toHexString(this.hashCode()); //Give the configuration a version to differentiate when (re-/un-)loading.
				}
				properties.put("type", String.format("%s-%s", config.getName(), version));
			} else { //if configuration is null (for whatever reason) we need to be able to differentiate adapters in between reloads.
				properties.put("identity", Integer.toHexString(adapter.hashCode()));
			}
			properties.put("name", adapter.getName().replaceAll("[=:,]", "_"));

			ObjectName name = new ObjectName(jmxDomain, properties);
			if(log.isDebugEnabled()) log.debug("determined ObjectName ["+name+"] for MBean ["+managedBean+"]");
			return name;
		} else {
			ObjectName name = super.getObjectName(managedBean, beanKey);
			log.warn("currently only MBeans of type [Adapter] are supported, falling back to IdentityNamingStrategy converting key ["+beanKey+"] bean ["+managedBean+"] to ObjectName ["+name+"]");
			return name;
		}
	}
}
