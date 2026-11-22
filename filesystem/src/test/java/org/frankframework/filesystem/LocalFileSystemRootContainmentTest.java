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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.senders.LocalFileSystemSender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

class LocalFileSystemRootContainmentTest {

	@TempDir
	Path base;

	private Path root;
	private Path outside;
	private LocalFileSystem fileSystem;
	private TestConfiguration configuration;

	@BeforeEach
	void setUp() throws Exception {
		root = Files.createDirectories(base.resolve("root"));
		outside = Files.createDirectories(base.resolve("outside"));

		configuration = new TestConfiguration();

		fileSystem = createFileSystem(root.toAbsolutePath().toString());
	}

	@AfterEach
	void tearDown() {
		configuration.close();
	}

	private LocalFileSystem createFileSystem(String root) throws Exception {
		LocalFileSystem localFileSystem = SpringUtils.createBean(configuration, LocalFileSystem.class);
		localFileSystem.setRoot(root);
		localFileSystem.configure();
		localFileSystem.open();
		return localFileSystem;
	}

	@Test
	void absoluteFilenameOutsideRootIsRejected() {
		String filename = outside.resolve("nope.txt").toAbsolutePath().toString();

		FileSystemException e = assertThrows(FileSystemException.class, () -> fileSystem.toFile(filename));
		assertTrue(e.getMessage().contains("outside the root"), e.getMessage());
	}

	@Test
	void parentTraversalIsRejected() {
		assertThrows(FileSystemException.class, () -> fileSystem.toFile("../outside/nope.txt"));
	}

	@Test
	void parentTraversalViaFolderParameterIsRejected() {
		assertThrows(FileSystemException.class, () -> fileSystem.toFile("../outside", "nope.txt"));
	}

	@Test
	void readOutsideRootIsRejected() throws Exception {
		Files.writeString(outside.resolve("secret.txt"), "credentials");

		assertThrows(FileSystemException.class, () -> fileSystem.toFile("../outside/secret.txt"));
	}

	@Test
	void relativeFilenameInsideRootIsAllowed() throws Exception {
		Path resolved = fileSystem.toFile("sub/file.txt");

		assertTrue(resolved.toAbsolutePath().normalize().startsWith(root));
	}

	@Test
	void absoluteFilenameInsideRootIsAllowed() throws Exception {
		Path resolved = fileSystem.toFile(root.resolve("file.txt").toAbsolutePath().toString());

		assertEquals(root.resolve("file.txt"), resolved);
	}

	@Test
	void traversalThatStaysInsideRootIsAllowed() throws Exception {
		Path resolved = fileSystem.toFile("sub/../file.txt");

		assertTrue(resolved.toAbsolutePath().normalize().startsWith(root));
	}

	@Test
	void writeInsideRootIsAllowed() throws Exception {
		Path resolved = fileSystem.toFile("allowed.txt");
		try (ByteArrayInputStream in = new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8))) {
			fileSystem.createFile(resolved, in);
		}

		assertEquals("payload", Files.readString(root.resolve("allowed.txt")));
	}

	/**
	 * Regression: a lexical check rejects valid paths when root and filename reach the same place through
	 * different symlinks, as /var and /private/var do on macOS, or a symlinked mount point does on Linux.
	 */
	@Test
	void rootReachedThroughASymlinkStillContainsItsFiles() throws Exception {
		Path link = base.resolve("link-to-root");
		Files.createSymbolicLink(link, root);

		LocalFileSystem viaLink = createFileSystem(link.toAbsolutePath().toString());

		Path resolved = viaLink.toFile(root.resolve("real-path-file.txt").toAbsolutePath().toString());

		assertEquals(root.resolve("real-path-file.txt"), resolved);
	}

	@Test
	void symlinkedRootStillRejectsPathsOutsideIt() throws Exception {
		Path link = base.resolve("link-to-root-2");
		Files.createSymbolicLink(link, root);

		LocalFileSystem viaLink = createFileSystem(link.toAbsolutePath().toString());

		assertThrows(FileSystemException.class, () -> viaLink.toFile("../outside/nope.txt"));
	}

	@Test
	void withoutRootAbsolutePathsRemainAllowed() throws Exception {
		LocalFileSystem noRoot = createFileSystem(null);
		Path target = outside.resolve("no-root.txt").toAbsolutePath();

		assertEquals(target, noRoot.toFile(target.toString()));
	}

	@Test
	void withoutRootAConfigurationWarningIsRaised() throws Exception {
		createFileSystem(null);

		assertEquals(1, configuration.getConfigurationWarnings().getWarnings().size());
		assertTrue(configuration.getConfigWarning(0).contains("attribute [root] has not been configured"), configuration.getConfigWarning(0));
	}

	@Test
	void withARootNoConfigurationWarningIsRaised() {
		assertEquals(0, configuration.getConfigurationWarnings().getWarnings().size());
	}

	/**
	 * The filesystem is not a Spring bean of its own in a configuration, the sender autowires it, so verify the
	 * warning still finds its way into the configuration through that route.
	 */
	@Test
	void warningIsRaisedThroughTheSender() throws Exception {
		LocalFileSystemSender sender = SpringUtils.createBean(configuration, LocalFileSystemSender.class);
		sender.setName("sender without a root");
		sender.setAction(FileSystemActor.FileSystemAction.READ);

		sender.configure();

		assertEquals(1, configuration.getConfigurationWarnings().getWarnings().size());
		assertTrue(configuration.getConfigWarning(0).contains("attribute [root] has not been configured"), configuration.getConfigWarning(0));
	}
}
