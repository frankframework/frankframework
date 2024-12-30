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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

import org.frankframework.core.Adapter;

public class AdapterFactory extends GenericFactory implements BeanFactoryAware {

	private AbstractAutowireCapableBeanFactory beanFactory;

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = (AbstractAutowireCapableBeanFactory) beanFactory;
	}

	@Override
	protected Object createObject(Map<String, String> attrs) throws ClassNotFoundException {
		String name = attrs.get("name");
		if (StringUtils.isEmpty(name)) {
			throw new IllegalStateException("Adapter must have a name");
		}

		attrs.put("className", Adapter.class.getCanonicalName());
		Object bean = super.createObject(attrs);
		Adapter adapter = (Adapter) bean;
		beanFactory.registerSingleton(name, adapter);
		return adapter;
	}
}
