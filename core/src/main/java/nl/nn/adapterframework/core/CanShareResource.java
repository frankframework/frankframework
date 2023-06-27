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
package nl.nn.adapterframework.core;

import javax.annotation.Nonnull;

import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle;

public interface CanShareResource<T> extends IConfigurable, ConfigurableLifecycle {

	void setSharedResourceName(String sharedResourceName);

	/** Retrieve the shared resource from Spring */
	default @Nonnull T getSharedResource(String sharedResourceName) {
		String beanName = ShareableResource.SHARED_RESOURCE_PREFIX + sharedResourceName;
		if(getApplicationContext().containsBean(beanName)) {
			ShareableResource<?> container = getApplicationContext().getBean(beanName, ShareableResource.class);
			ShareableResource<T> resource = (ShareableResource<T>) container;
			return resource.getSharedResource();
			//TODO Handle exceptions
		}
		throw new IllegalArgumentException("Shared Resource ["+beanName+"] does not exist");
	}

	/** Retrieve the local resource */
	T getLocalResource();

	@Override
	default boolean isRunning() {
		return getLocalResource() != null;
	}
}
