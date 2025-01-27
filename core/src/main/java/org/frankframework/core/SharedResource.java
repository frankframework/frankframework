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

import org.springframework.context.Phased;

import org.frankframework.lifecycle.ConfigurableLifecycle;

public interface SharedResource<T> extends ConfigurableLifecycle, HasName, Phased {
	String SHARED_RESOURCE_PREFIX = "shared$$";

	/** Retrieve the shared resource so {@link CanUseSharedResource} holders can use it */
	T getSharedResource();

	/**
	 * By default give this the lowest Phase, so it's started first and stopped last.
	 */
	@Override
	default int getPhase() {
		return Integer.MIN_VALUE;
	}
}
