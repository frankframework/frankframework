/*
   Copyright 2025-2026 WeAreFrank!

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

import java.util.Map;

import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class AppConstantCondition implements Condition {

	@Override
	public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
		Map<@NonNull String, @Nullable Object> attributes = metadata.getAnnotationAttributes(ConditionalOnAppConstants.class.getName());
		if (attributes == null)
			return false;

		if (!attributes.containsKey("name") || !attributes.containsKey("value"))
			return false;

		Object propNameObj = attributes.get("name");
		if (propNameObj == null) {
			return false;
		}
		String propertyName = propNameObj.toString();
		Object propValueObj = attributes.get("value");
		String propertyValue = propValueObj != null ? propValueObj.toString() : null;
		String appConstantsPropertyValue = context.getEnvironment().getProperty(propertyName);
		return Strings.CI.equals(propertyValue, appConstantsPropertyValue);
	}
}
