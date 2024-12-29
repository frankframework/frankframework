/*
   Copyright 2024 WeAreFrank!

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

import org.frankframework.core.Adapter;
import org.frankframework.util.SpringUtils;

public class AdapterFactory extends GenericFactory {

	@Override
	protected Object createObject(Map<String, String> attrs) throws ClassNotFoundException {
		String name = attrs.get("name");
		if (StringUtils.isEmpty(name)) {
			throw new IllegalStateException("Adapter must have a name");
		}

		attrs.put("className", Adapter.class.getCanonicalName());
		Object bean = super.createObject(attrs);
		Adapter adapter = (Adapter) getApplicationContext().getAutowireCapableBeanFactory().initializeBean(bean, name);
		SpringUtils.registerSingleton(getApplicationContext(), name, adapter);
		return adapter;
	}
}
