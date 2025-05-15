/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.condition;

import jakarta.annotation.Nonnull;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class AppConstantCondition implements Condition {

	@Override
	public boolean matches(@Nonnull ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnAppConstants.class.getName());
		if (attributes == null)
			return false;

		if (!attributes.containsKey("name") || !attributes.containsKey("value"))
			return false;

		String propertyName = attributes.get("name").toString();
		String propertyValue = attributes.get("value").toString();

		if (attributes.containsKey("defaultValue")) {
			String defaultValue = attributes.get("defaultValue").toString();
			return matchWithAppConstantsProperty(context.getEnvironment(), propertyName, propertyValue, defaultValue);
		}
		return matchWithAppConstantsProperty(context.getEnvironment(), propertyName, propertyValue);
	}

	private boolean matchWithAppConstantsProperty(Environment environment, String propertyName, String propertyValue) {
		String appConstantsPropertyValue = environment.getProperty(propertyName);
		return propertyValue.equalsIgnoreCase(appConstantsPropertyValue);
	}

	private boolean matchWithAppConstantsProperty(Environment environment, String propertyName, String propertyValue, String defaultValue) {
		String appConstantsPropertyValue = environment.getProperty(propertyName, defaultValue);
		return propertyValue.equalsIgnoreCase(appConstantsPropertyValue);
	}
}
