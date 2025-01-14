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
package org.frankframework.core;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorSupport;

import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

public class JndiContextPrefixFactory implements InitializingBean {

	private final Logger log = LogUtil.getLogger(this);
	private String jndiContextPrefix = null;

	private enum ContextPrefix {
		JBOSS("java:/"),
		TOMCAT(JndiLocatorSupport.CONTAINER_PREFIX),
		DEFAULT(JndiLocatorSupport.CONTAINER_PREFIX);

		private String prefix = "";
		ContextPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getPrefix() {
			return prefix;
		}
	}

	@Override
	public void afterPropertiesSet() throws IllegalStateException {
		String applicationServerType = AppConstants.getInstance().getProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
		if(applicationServerType == null) {
			throw new IllegalStateException("Unable to determine application server type");
		}

		ContextPrefix prefix = ContextPrefix.DEFAULT;
		try {
			prefix = ContextPrefix.valueOf(applicationServerType);
		} catch (IllegalArgumentException e) {
			log.warn("unable to determine JndiContextPrefix, reverting to default [{}]", ContextPrefix.DEFAULT.name());
		}

		jndiContextPrefix = prefix.getPrefix();
		log.info("using JNDI ContextPrefix [{}]", jndiContextPrefix);
	}

	public String getContextPrefix() {
		return jndiContextPrefix;
	}
}
