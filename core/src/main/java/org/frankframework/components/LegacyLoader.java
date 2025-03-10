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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.frankframework.util.Environment;

import lombok.extern.log4j.Log4j2;

/**
 * Old style of finding Modules, instead of self-registering, the code attempts to locate the module on the classpath.
 * This is purely for debugging purposes to see which modules are present.
 */
@Log4j2
public class LegacyLoader {
	private static final String FRANKFRAMEWORK_NAMESPACE = "META-INF/maven/org.frankframework/";

	private LegacyLoader() {
		// NO OP, should not be instantiated directly
	}

	/**
	 * Register all IBIS modules that can be found on the classpath
	*/
	private static List<String> getApplicationModules() {
		List<String> modulesToScanFor = new ArrayList<>();

		// Legacy
		modulesToScanFor.add("frankframework-akamai");
		modulesToScanFor.add("frankframework-aspose");
		modulesToScanFor.add("frankframework-aws");
		modulesToScanFor.add("frankframework-batch");
		modulesToScanFor.add("frankframework-cmis");
		modulesToScanFor.add("frankframework-commons");
		modulesToScanFor.add("frankframework-console-frontend");
		modulesToScanFor.add("frankframework-console-backend");
		modulesToScanFor.add("frankframework-core");
		modulesToScanFor.add("credentialprovider");
		modulesToScanFor.add("frankframework-dbms");
		modulesToScanFor.add("frankframework-filesystem");
		modulesToScanFor.add("frankframework-idin");
		modulesToScanFor.add("frankframework-ladybug-common");
		modulesToScanFor.add("frankframework-ladybug-debugger");
		modulesToScanFor.add("frankframework-larva");
		modulesToScanFor.add("frankframework-management-gateway");
		modulesToScanFor.add("frankframework-messaging");
		modulesToScanFor.add("frankframework-kubernetes");
		modulesToScanFor.add("frankframework-nn-specials");
		modulesToScanFor.add("frankframework-sap");
		modulesToScanFor.add("frankframework-security");
		modulesToScanFor.add("frankframework-tibco");
		modulesToScanFor.add("frankframework-webapp");

		return modulesToScanFor;
	}

	public static List<Module> findActiveApplicationModules() {
		List<Module> ffModules = new ArrayList<>();

		for (String module : getApplicationModules()) {
			ClassLoader classLoader = Environment.class.getClassLoader();
			URL pomProperties = classLoader.getResource(FRANKFRAMEWORK_NAMESPACE + module + "/pom.properties");
			if (pomProperties != null) {
				try {
					Manifest manifest = convertPomPropertiesToManifest(pomProperties);
					ffModules.add(new LegacyModule(module, manifest));
				} catch (Exception e) {
					log.debug("unable to convert pom properties URL [{}] to jar file URL", pomProperties);
				}
			}
		}

		return ffModules;
	}

	private static Manifest convertPomPropertiesToManifest(URL pomProperties) throws IOException {
		String fullUrl = pomProperties.toExternalForm();
		int bangSlash = fullUrl.indexOf("!/");
		if (bangSlash > 0) { // We found a bang-slash, assume it's file
			String jarFile = fullUrl.substring(fullUrl.indexOf("file:"), bangSlash);
			return Environment.getManifest(new URL(jarFile));
		} else {
			int metaInfFolder = fullUrl.indexOf("/META-INF/");
			if (metaInfFolder > 0) { // We found the meta-inf folder, it could be a shared classpath. Attempt to locate the MANIFEST file.
				String manifestFile = fullUrl.substring(fullUrl.indexOf("file:"), metaInfFolder +1) + JarFile.MANIFEST_NAME;
				URL manifestURL = new URL(manifestFile);
				try (InputStream is = manifestURL.openStream()) { // Throws a FileNotFoundException
					return new Manifest(is);
				}
			}
		}
		throw new FileNotFoundException("no manifest file found relative to path ["+pomProperties+"]");
	}

	public static class LegacyModule implements Module {
		private final ModuleInformation moduleInformation;

		public LegacyModule(String artifactId, Manifest manifest) throws IOException {
			moduleInformation = new ModuleInformation(manifest);
			moduleInformation.setArtifactId(artifactId);
			moduleInformation.setGroupId("org.frankframework");
		}

		@Override
		public ModuleInformation getModuleInformation() {
			return moduleInformation;
		}

		/**
		 * This equals method does not look at `instanceof LegacyModule` but if the module titles match.
		 * This ensures backwards compatibility with older modules
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Module module) {
				try {
					return this.moduleInformation.getArtifactId().equals(module.getModuleInformation().getArtifactId());
				} catch (IOException e) {
					log.trace("attempting to compare modules but was unable to read module information [{}]", module);
				}
			}
			return false;
		}
	}
}
