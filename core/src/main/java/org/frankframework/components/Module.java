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
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.frankframework.lifecycle.SpringContextScope;
import org.frankframework.util.Environment;

import jakarta.annotation.Nonnull;

/**
 * Interface for Frank!Framework modules.
 * Once plugins are introduced their use will become more tangible, as a plugin may depend on a module.
 * 
 * Modules may provide a Spring configuration file, to be loaded in the Spring Application Context.
 * See {@link SpringContextScope#APPLICATION} for more info and it's use.
 * 
 * <p>
 * Implementors of the interface should have a no-args constructor and not need any injected dependencies.
 * Classes are loaded via the JDK {@link java.util.ServiceLoader} mechanism.
 * </p>
 */
public interface Module {

	/**
	 * Retrieves the module information based on the MANIFEST.MF file instead of the pom.properties.
	 */
	@Nonnull
	default ModuleInformation getModuleInformation() throws IOException {
		URL jarFileLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		return new ModuleInformation(Environment.getManifest(jarFileLocation));
	}

	/**
	 * Modules may provide a Spring configuration file, to be loaded in the Spring Application Context.
	 * See {@link SpringContextScope#APPLICATION} for more info and it's use.
	 */
	default List<String> getSpringConfigurationFiles() {
		return Collections.emptyList();
	}
}
