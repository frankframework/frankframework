/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
// Careful.. don't log here!!
public class IafExampleInitializer {

	public static void main(String[] args) throws IOException {
		FrankApplication app = new FrankApplication();
//		setConfigurationsDirectory(app.getProjectDir());
		app.run(args);
	}

	private static void setConfigurationsDirectory(Path projectDir) throws IOException {
		Path configurationDir = projectDir.resolve("src/main/configurations").toAbsolutePath();
		System.setProperty("configurations.directory", configurationDir.toString());

		// Loop though all directories (depth = 1) + skip current directory.
		try(Stream<Path> folders = Files.walk(configurationDir, 1).skip(1).filter(Files::isDirectory)) {
			folders.forEach(path -> {
				String name = path.getFileName().toString();
				System.setProperty("configurations."+name+".classLoaderType", "ScanningDirectoryClassLoader");
				System.setProperty("configurations."+name+".configurationFile", "Configuration.xml");
				System.setProperty("configurations."+name+".basePath", name);
			});
		}
	}
}
