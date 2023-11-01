/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.extensions.aspose.services.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import lombok.extern.log4j.Log4j2;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
@Log4j2
public final class FileUtil {
	private FileUtil() {
	}

	public static void deleteDirectoryContents(final File directory) throws IOException {
		if (directory.exists()) {
			Files.walkFileTree(directory.toPath(), new DeleteDirectoryContentsFileVisitor(directory));
		}
	}

	/**
	 * Delete the given file (when file == null nothing will be done). Throws an
	 * runtimeexception when deleting fails.
	 *
	 * @param file
	 * @throws IOException
	 */
	public static void deleteFile(File file) throws IOException {
		// Delete always the temporary file if it exist.
		if (file != null && Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				log.warn("Deleting file failed!", e);
				throw new IOException("Deleting file failed!", e);
			}
		}

	}

	/**
	 * Visitor to delete all files in the given directory except the directory
	 * itself.
	 */
	private static class DeleteDirectoryContentsFileVisitor extends SimpleFileVisitor<Path> {

		private final File directory;

		private DeleteDirectoryContentsFileVisitor(final File directory) {
			this.directory = directory;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			// Do not delete the given directory.
			if (!dir.equals(directory.toPath())) {
				log.debug("Delete directory {}", dir);
				Files.delete(dir);
			}
			if (exc != null) {
				throw exc;
			}
			return FileVisitResult.CONTINUE;
		}
	}
}
