/*
   Copyright 2019-2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.lifecycle;

import javax.servlet.ServletContext;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Starts a Spring Context before all Servlets are initialized. This allows the use of dynamically creating 
 * Spring wired Servlets (using the {@link ServletManager}). These beans can be retrieved later on from within
 * the IbisContext, and are unaffected by the {@link IbisContext#fullReload()}.
 * 
 * @author Niels Meijer
 *
 */
public class IbisApplicationInitializer extends ContextLoaderListener {

	@Override
	protected WebApplicationContext createWebApplicationContext(ServletContext servletContext) {
		System.setProperty(EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER, "false");
		servletContext.log("Starting IBIS WebApplicationInitializer");

		XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
		applicationContext.setConfigLocation(XmlWebApplicationContext.CLASSPATH_URL_PREFIX + "/webApplicationContext.xml");
		applicationContext.setDisplayName("IbisApplicationInitializer");

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("ibis", AppConstants.getInstance()));

		return applicationContext;
	}

	/*
	 * Purely here to print the CXF SpringBus ID
	 */
	@Override
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		WebApplicationContext wac = super.initWebApplicationContext(servletContext);
		SpringBus bus = (SpringBus) wac.getBean("cxf");
		servletContext.log("Successfully started IBIS WebApplicationInitializer with SpringBus ["+bus.getId()+"]");
		return wac;
	}
}
