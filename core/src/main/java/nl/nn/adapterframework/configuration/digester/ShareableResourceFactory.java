/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.configuration.digester;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.Lifecycle;

import nl.nn.adapterframework.core.ShareableResource;

/**
 * Registers the newly created bean directly in Spring, which will manage it's {@link Lifecycle}.
 */
public class ShareableResourceFactory extends AbstractSpringPoweredDigesterFactory {

	@Override
	public String getSuggestedBeanName() {
		return null;
	}

	@Override
	protected Object createObject(Map<String, String> attrs) throws ClassNotFoundException {
		Object object = super.createObject(attrs);

		if(!(object instanceof ShareableResource)) {
			throw new IllegalStateException("bean must be of type ShareableResource");
		}

		String objectName = attrs.get("name");
		if(StringUtils.isBlank(objectName)) {
			throw new IllegalStateException("Shared Resource must have a name");
		}

		String beanName = ShareableResource.SHARED_RESOURCE_PREFIX + objectName;

		if(getApplicationContext().containsBean(beanName)) {
			throw new IllegalStateException("shareable resource ["+objectName+"] already exists");
		}

		ConfigurableBeanFactory configurableListableBeanFactory = (ConfigurableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
		configurableListableBeanFactory.registerSingleton(beanName, object);

		return object;
	}
}
