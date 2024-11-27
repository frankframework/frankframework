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
package org.frankframework.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.frankframework.lifecycle.ConfigurableLifecycle;

public interface CanUseSharedResource<T> extends IConfigurable, ConfigurableLifecycle {

	void setSharedResourceRef(String sharedResourceName);

	/** Retrieve the shared resource from Spring */
	@SuppressWarnings("unchecked")
	default @Nonnull T getSharedResource(String sharedResourceName) {
		String beanName = SharedResource.SHARED_RESOURCE_PREFIX + sharedResourceName;
		if(getApplicationContext().containsBean(beanName)) {
			SharedResource<?> container = getApplicationContext().getBean(beanName, SharedResource.class);
			Object resource = container.getSharedResource();

			if(getObjectType() != null && !getObjectType().isAssignableFrom(resource.getClass())) {
				// our own 'ClassCastException'
				throw new IllegalStateException("Shared Resource ["+sharedResourceName+"] may not be used here");
			}
			return (T) resource;
		}
		throw new IllegalArgumentException("Shared Resource ["+sharedResourceName+"] does not exist");
	}

	/**
	 * Used to validate the expected type so now unexpected ClassCastExceptions can occur.
	 * May be NULL in which case this check will be skipped and the SharedResource will be returned regardless.
	 */
	@Nullable
	Class<T> getObjectType();
}
