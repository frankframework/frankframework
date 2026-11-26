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
package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The containment of {@link LocalFileSystem#setRoot(String)}, for the cases where the folder or the root cannot be
 * compared lexically.
 */
class LocalFileSystemRootContainmentTest {

	@TempDir
	Path base;

	private Path root;
	private Path outside;
	private LocalFileSystem fileSystem;

	@BeforeEach
	void setUp() throws Exception {
		root = Files.createDirectories(base.resolve("root"));
		outside = Files.createDirectories(base.resolve("outside"));

		fileSystem = createFileSystem(root.toAbsolutePath().toString());
	}

	private LocalFileSystem createFileSystem(@Nullable String configuredRoot) throws Exception {
		LocalFileSystem localFileSystem = new LocalFileSystem(mock());
		localFileSystem.setRoot(configuredRoot);
		localFileSystem.configure();
		localFileSystem.open();
		return localFileSystem;
	}

	/**
	 * A symlink needs SeCreateSymbolicLinkPrivilege on Windows, which not every machine grants, so let the
	 * filesystem decide whether these tests can run instead of deciding it by operating system.
	 */
	private static Path createSymbolicLink(Path link, Path target) {
		try {
			return Files.createSymbolicLink(link, target);
		} catch (IOException | UnsupportedOperationException e) {
			return abort("cannot create a symlink here: " + e.getMessage());
		}
	}

	/**
	 * These are the folders that {@link org.apache.commons.io.FilenameUtils#concat(String, String)} answers with
	 * null, because they normalize past their own start. They have to be rejected, not fall over the null.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "..", "../outside", "sub/../..", "sub/../../outside" })
	void folderThatStepsAboveTheRootIsRejected(String folder) {
		assertThrows(FileSystemException.class, () -> fileSystem.toFile(folder, "nope.txt"));
	}

	/** Without a root there is nothing to contain, so such a folder keeps resolving as it did. */
	@Test
	void folderThatStepsAboveItselfIsAllowedWithoutARoot() throws Exception {
		LocalFileSystem noRoot = createFileSystem(null);

		assertEquals(Paths.get("../outside/nope.txt"), noRoot.toFile("../outside", "nope.txt"));
	}

	@Test
	void folderInsideTheRootIsStillAccepted() throws Exception {
		Path resolved = fileSystem.toFile("sub", "file.txt");

		assertTrue(resolved.toAbsolutePath().normalize().startsWith(root), resolved.toString());
	}

	@Test
	void absoluteFilenameOutsideTheRootIsStillRejected() {
		String filename = outside.resolve("nope.txt").toAbsolutePath().toString();

		assertThrows(FileSystemException.class, () -> fileSystem.toFile(filename));
	}

	/**
	 * The root and the filename can spell the same location differently, as /var and /private/var do on macOS, or
	 * as a root that is a symlink to the volume it lives on does in a container. A lexical comparison then rejects
	 * the root its own files.
	 */
	@Test
	void absoluteFilenameInsideASymlinkedRootIsAllowed() throws Exception {
		Path link = createSymbolicLink(base.resolve("link-to-root"), root);
		LocalFileSystem viaLink = createFileSystem(link.toAbsolutePath().toString());

		Path filename = root.resolve("file.txt").toAbsolutePath();

		assertEquals(filename, viaLink.toFile(filename.toString()));
	}

	@Test
	void symlinkedRootStillRejectsPathsOutsideIt() throws Exception {
		Path link = createSymbolicLink(base.resolve("link-to-root"), root);
		LocalFileSystem viaLink = createFileSystem(link.toAbsolutePath().toString());

		assertThrows(FileSystemException.class, () -> viaLink.toFile(outside.resolve("nope.txt").toAbsolutePath().toString()));
		assertThrows(FileSystemException.class, () -> viaLink.toFile("../outside/nope.txt"));
		assertThrows(FileSystemException.class, () -> viaLink.toFile("../outside", "nope.txt"));
	}
}
