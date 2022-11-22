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
package nl.nn.adapterframework.lifecycle.servlets;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.lifecycle.ServletManager;

/**
 * This class is meant to trigger last after all other servlets have been registerd at the ServletManager
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@IbisInitializer
public class AfterServletsInitialized implements InitializingBean {

	private @Setter @Autowired ServletManager servletManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(servletManager == null) {
			throw new IllegalStateException("unable to initialize Spring Security, ServletManager not set");
		}

		servletManager.startAuthenticators();
	}
}
