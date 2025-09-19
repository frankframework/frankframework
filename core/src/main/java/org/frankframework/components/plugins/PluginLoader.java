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

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.InitializingBean;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AppConstants;

@Log4j2
public class PluginLoader implements InitializingBean {

	private Path directory;
	private PluginManager pluginManager;

	public PluginLoader() {
		this(AppConstants.getInstance().getProperty("plugins.directory"));
	}

	protected PluginLoader(String pluginDirectory) {
		if (StringUtils.isNotBlank(pluginDirectory)) {
			directory = Path.of(pluginDirectory);
			if (!Files.isDirectory(directory)) {
				throw new IllegalArgumentException("no valid path provided");
			}
		}
	}

	@Nullable
	public PluginWrapper findPlugin(String pluginName) {
		if (pluginManager == null) {
			return null;
		}

		return pluginManager.getPlugin(pluginName);
	}

	@Override
	public void afterPropertiesSet() {
		if (directory == null) {
			log.info("disabled plugin capability");
			return;
		}
		log.info("enabled plugin capability using directory [{}]", directory);

		pluginManager = new FrankPluginManager(directory);
		pluginManager.loadPlugins();

		log.info("loaded [{}] plugin(s) / component(s)", pluginManager.getPlugins().size());
	}
}
