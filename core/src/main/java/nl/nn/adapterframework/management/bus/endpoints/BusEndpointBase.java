/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

public class BusEndpointBase implements InitializingBean {
	protected Logger log = LogUtil.getLogger(this);
	private ApplicationContext applicationContext;
	private IbisManager ibisManager;

	public final void setApplicationContext(ApplicationContext ac) {
		this.applicationContext = ac;
	}

	protected final ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	protected final IbisManager getIbisManager() {
		return ibisManager;
	}

	protected final <T> T createBean(Class<T> beanClass) {
		return SpringUtils.createBean(applicationContext, beanClass);
	}

	protected final <T> T getBean(String beanName, Class<T> beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	@Override
	public final void afterPropertiesSet() {
		if(applicationContext == null) {
			throw new BusException("ApplicationContext not set");
		}

		ibisManager = applicationContext.getBean("ibisManager", IbisManager.class);
	}
}
