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
package org.frankframework.configuration.digester;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.SharedResource;
import org.frankframework.util.SpringUtils;

/**
 * Registers the newly created bean directly in Spring, which will manage it's {@link Lifecycle}.
 */
public class SharedResourceFactory extends AbstractSpringPoweredDigesterFactory {

	@Override
	public String getSuggestedBeanName() {
		return null;
	}

	@Override
	protected Object createBean(ApplicationContext context, Map<String, String> attrs) throws ClassNotFoundException {
		if (!(context instanceof Configuration)) {
			throw new IllegalStateException("context must be of type Shared Resource");
		}

		Object object = super.createBean(context, attrs);
		if (!(object instanceof SharedResource)) {
			throw new IllegalStateException("bean must be of type Shared Resource");
		}

		String objectName = attrs.get("name");
		if(StringUtils.isBlank(objectName)) {
			throw new IllegalStateException("Shared Resource must have a name");
		}

		String beanName = SharedResource.SHARED_RESOURCE_PREFIX + objectName;

		if(context.containsBean(beanName)) {
			throw new IllegalStateException("shared resource ["+objectName+"] already exists");
		}

		SpringUtils.registerSingleton(context, beanName, object);
		return object;
	}
}
