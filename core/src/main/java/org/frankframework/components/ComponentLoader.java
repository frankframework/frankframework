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
package org.frankframework.components;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

@Log4j2
public class ComponentLoader {
	protected static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	private static Collection<Module> modules = null;

	private ComponentLoader() {
		// NO OP, should not be instantiated directly
	}

	public static List<Module> findAllModules() {
		List<Module> allModules = new ArrayList<>(getModules());

		// Filter out modules that already exist, match on name
		LegacyLoader.findActiveApplicationModules()
			.stream()
			.filter(m -> !allModules.contains(m))
			.forEach(allModules::add);

		allModules.forEach(ComponentLoader::registerApplicationModules);

		return allModules;
	}

	/**
	 * Register Frank!Framework modules that can be found on the classpath
	 *
	 * @param module to register
	 */
	private static void registerApplicationModules(Module module) {
		try {
			ModuleInformation moduleInfo = module.getModuleInformation();

			APP_CONSTANTS.put(moduleInfo.getArtifactId() + ".version", moduleInfo.getVersion());
			APPLICATION_LOG.debug("Loading {}", moduleInfo);
		} catch (NoSuchFileException e) {
			log.info("unable to find module manifest file", e);
		} catch (IOException e) {
			log.warn("unable to open module manifest file", e);
		}
	}

	public static Collection<Module> getModules() {
		if (modules == null) {
			try {
				modules = CollectionUtils.collect(ServiceLoader.load(Module.class).iterator(), input -> input);
			} catch (Throwable t) {
				log.warn("unable to load modules", t);
				modules = Collections.emptyList();
			}
		}
		return modules;
	}
}
