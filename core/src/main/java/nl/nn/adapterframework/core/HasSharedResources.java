/*
   Copyright 2021 WeAreFrank!

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

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.functional.ThrowingFunction;

public interface HasSharedResources<R> extends IScopeProvider {

	Map<String,R> getSharedResources();
	void setSharedResources(Map<String,R> resources);

	default R getSharedResource(String name, ThrowingFunction<String, R, ConfigurationException> creator) throws ConfigurationException {
		R result;
		Map<String,R> resources = getSharedResources();
		if (resources==null) {
			return creator.apply(name);
		}
		synchronized (resources) {
			result = resources.get(name);
			if (result==null) {
				result = creator.apply(name);
				resources.put(name, result);
			}
			return result;
		}
	}
}
