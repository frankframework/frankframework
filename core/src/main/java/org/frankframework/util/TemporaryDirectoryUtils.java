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
package org.frankframework.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TemporaryDirectoryUtils {

	private TemporaryDirectoryUtils() {
		// NO OP
	}

	private static final Path FRANK_TEMP_DIR = Paths.get(computeTempDirectory());

	/**
	 * If the ${ibis.tmpdir} is relative it will turn it into an absolute path.
	 * @return The absolute path of ${ibis.tmpdir} or IllegalStateException if it cannot be resolved.
	 */
	private static @Nonnull String computeTempDirectory() {
		String directory = AppConstants.getInstance().getProperty("ibis.tmpdir");

		if (StringUtils.isNotEmpty(directory)) {
			File file = new File(directory);
			if (!file.isAbsolute()) {
				String absPath = new File("").getAbsolutePath();
				file = new File(absPath, directory);
			}
			if(!file.exists()) {
				file.mkdirs();
			}
			String fileDir = file.getPath();
			if(StringUtils.isEmpty(fileDir) || !file.isDirectory()) {
				throw new IllegalStateException("unknown or invalid path ["+(StringUtils.isEmpty(fileDir)?"NULL":fileDir)+"]");
			}
			directory = file.getAbsolutePath();
		}
		log.debug("resolved temp directory to [{}]", directory);

		//Directory may be NULL but not empty. The directory has to valid, available and the IBIS must have read+write access to it.
		if(StringUtils.isEmpty(directory)) {
			log.error("unable to determine ibis temp directory, falling back to [java.io.tmpdir]");
			return System.getProperty("java.io.tmpdir");
		}
		return directory;
	}

	/**
	 * If the ${ibis.tmpdir} is relative it will turn it into an absolute path.
	 * @return The absolute path of ${ibis.tmpdir} or IllegalStateException if it cannot be resolved.
	 */
	public static @Nonnull Path getTempDirectory() {
		return FRANK_TEMP_DIR;
	}

	/**
	 * @return the ${ibis.tmpdir}/folder or IOException if it cannot be resolved.
	 * If the ${ibis.tmpdir} is relative it will turn it into an absolute path
	 * @throws IOException
	 */
	public static Path getTempDirectory(String folder) throws IOException {
		Path tempDir = getTempDirectory();
		Path newDir = tempDir.resolve(folder);
		if (!Files.exists(newDir) && Files.createDirectories(newDir) == null) {
			throw new IOException("unable to create temp directory [" + newDir + "]");
		}
		return newDir;
	}
}
