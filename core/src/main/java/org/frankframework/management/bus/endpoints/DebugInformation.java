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

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.util.ClassUtils;
import org.springframework.messaging.Message;

@BusAware("frank-management-bus")
public class DebugInformation extends BusEndpointBase {

	@TopicSelector(BusTopic.DEBUG)
	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getClassInfo(Message<?> message) {
		String baseClassName = BusMessageUtils.getHeader(message, "baseClassName");
		String className = BusMessageUtils.getHeader(message, "className");

		if(StringUtils.isEmpty(className)) {
			throw new BusException("className may not be empty");
		}

		return getClassInfo(baseClassName, className);
	}

	private Message<String> getClassInfo(String baseClassName, String className) {
		try {
			Class<?> baseClass;
			if (StringUtils.isNotEmpty(baseClassName)) {
				baseClass = Class.forName(baseClassName, false, this.getClass().getClassLoader());
			} else {
				baseClass = this.getClass();
			}
			ClassLoader classLoader = baseClass.getClassLoader();
			Class<?> clazz = classLoader.loadClass(className);
			List<?> result = ClassUtils.getClassInfoList(clazz);
			return new JsonMessage(result);
		} catch (Exception e) {
			throw new BusException("Could not determine classInfo for class ["+className+"]", e);
		}
	}
}
