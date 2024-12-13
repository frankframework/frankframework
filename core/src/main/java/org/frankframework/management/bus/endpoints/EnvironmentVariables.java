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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.messaging.Message;

import org.frankframework.configuration.Configuration;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.Environment;
import org.frankframework.util.StringUtil;

@BusAware("frank-management-bus")
public class EnvironmentVariables extends BusEndpointBase {

	@TopicSelector(BusTopic.ENVIRONMENT)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getEnvironmentVariables(Message<?> message) {
		List<String> propsToHide = new ArrayList<>();
		String propertiesHideString = AppConstants.getInstance().getString("properties.hide", null);
		if (propertiesHideString!=null) {
			propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
		}

		Map<String, Object> envVars = new HashMap<>();
		Map<String, Object> configVars = new HashMap<>();

		configVars.put("Global", convertPropertiesToMap(AppConstants.getInstance(), propsToHide));
		for(Configuration config : getIbisManager().getConfigurations()) {
			if(config.getClassLoader() != null) {
				configVars.put(config.getName(), convertPropertiesToMap(AppConstants.getInstance(config.getClassLoader()), propsToHide));
			}
		}
		envVars.put("Application Constants", configVars);
		envVars.put("System Properties", convertPropertiesToMap(System.getProperties(), propsToHide));

		try {
			envVars.put("Environment Variables", convertPropertiesToMap(Environment.getEnvironmentVariables()));
		} catch (Throwable t) {
			log.warn("caught Throwable while getting EnvironmentVariables", t);
		}

		return new JsonMessage(envVars);
	}

	private Map<String, Object> convertPropertiesToMap(Properties props) {
		return convertPropertiesToMap(props, null);
	}

	private Map<String, Object> convertPropertiesToMap(Properties props, List<String> propsToHide) {
		Enumeration<Object> enumeration = props.keys();

		Map<String, Object> properties = new HashMap<>(props.size());

		while (enumeration.hasMoreElements()) {
			String propName = (String) enumeration.nextElement();
			String propValue;
			try {
				propValue = props.getProperty(propName);
				if (propsToHide != null && propsToHide.contains(propName)) {
					propValue = StringUtil.hide(propValue);
				}
			} catch (Exception | StackOverflowError e) {
				// catch StackOverflowErrors, to enable analysis of cyclic property definitions
				String msg = "cannot get value of property ["+ propName+"]";
				propValue = msg+" ("+ClassUtils.nameOf(e)+"): "+e.getMessage();
				log.warn(msg, e);
			}
			properties.put(propName, propValue);
		}
		return properties;
	}
}
