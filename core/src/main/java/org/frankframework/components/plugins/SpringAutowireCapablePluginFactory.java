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
package org.frankframework.components.plugins;

import java.lang.reflect.Modifier;

import org.pf4j.Plugin;
import org.pf4j.PluginFactory;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.SpringUtils;

/**
 * Wire the Plugins via Spring so they can use the ApplicationContext.
 */
@Log4j2
public class SpringAutowireCapablePluginFactory implements PluginFactory {
	private final ApplicationContext applicationContext;

	public SpringAutowireCapablePluginFactory(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public Plugin create(PluginWrapper pluginWrapper) {
		String pluginClassName = pluginWrapper.getDescriptor().getPluginClass();
		log.debug("Create instance for plugin '{}'", pluginClassName);

		Class<?> pluginClass;
		try {
			pluginClass = pluginWrapper.getPluginClassLoader().loadClass(pluginClassName);
		} catch (ClassNotFoundException e) {
			log.error(e.getMessage(), e);
			return null;
		}

		// Once we have the class, we can do some checks on it to ensure that it is a valid implementation of a plugin.
		int modifiers = pluginClass.getModifiers();
		if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers) || (!Plugin.class.isAssignableFrom(pluginClass))) {
			log.error("The plugin class '{}' is not valid", pluginClassName);
			return null;
		}

		return (Plugin) SpringUtils.createBean(applicationContext, pluginClass);
	}

}
