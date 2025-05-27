/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.http.cxf;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import org.frankframework.util.LogUtil;

class SpringSoapBus extends SpringBus implements InitializingBean, DisposableBean {
	// This list comes from https://stackoverflow.com/questions/37756464/setting-apache-cxf-bus-properties-for-malicious-xml.
	// The present code to process them expects them to be integers.
	private static final List<String> CFX_SECURITY_PROPERTIES = Arrays.asList(
		"org.apache.cxf.stax.maxAttributeSize",
		"org.apache.cxf.stax.maxChildElements",
		"org.apache.cxf.stax.maxElementDepth",
		"org.apache.cxf.stax.maxAttributeCount",
		"org.apache.cxf.stax.maxTextLength",
		"org.apache.cxf.stax.maxElementCount");
	private static final String SOAP_BUS_PREFIX = "soap.bus.";

	private final Logger log = LogUtil.getLogger(this);
	private ApplicationContext applicationContext;

	public SpringSoapBus() {
		super(true);
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) {
		super.setApplicationContext(ctx);
		this.applicationContext = ctx;
	}

	@Override
	public void afterPropertiesSet() {
		for(String propName: CFX_SECURITY_PROPERTIES) {
			String propValue = applicationContext.getEnvironment().getProperty(SOAP_BUS_PREFIX + propName);
			if(! StringUtils.isBlank(propValue)) {
				this.setProperty(propName, Integer.valueOf(propValue));
				log.info("Set property [{}] of bus [{}] to [{}]", () -> propName, this::getId, () -> propValue);
			}
		}
	}

	@Override
	public void destroy() {
		shutdown();
	}
}
