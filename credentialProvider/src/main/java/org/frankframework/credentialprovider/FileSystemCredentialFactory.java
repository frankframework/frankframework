/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.credentialprovider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.credentialprovider.util.CredentialConstants;

/**
 * <p>CredentialFactory implementation that reads secrets from the file system.</p>
 *
 * <p>It reads the username and password from files in a directory. The directory is set by the property {@value #FILESYSTEM_ROOT_PROPERTY}.</p>
 *
 * <p>It reads the username from a file with the name set by the property {@value CredentialFactory#DEFAULT_USERNAME_FIELD} and the password from a file with the name set by
 * the property {@value #PASSWORD_FILE_PROPERTY}. These values are relative to the {@value #FILESYSTEM_ROOT_PROPERTY}</p>
 *
 * <p>By default, the default values {@code username} and {@code password} are used for these files.</p>
 *
 * @ff.info It's required to set the property {@value FileSystemCredentialFactory#FILESYSTEM_ROOT_PROPERTY} to the directory you wish to read credentials from.
 *
 */
public class FileSystemCredentialFactory implements ISecretProvider {
	private static final String FILESYSTEM_ROOT_PROPERTY = "credentialFactory.filesystem.root";

	private Path root;

	@Override
	public void initialize() {
		CredentialConstants appConstants = CredentialConstants.getInstance();
		String fsroot = appConstants.getProperty(FILESYSTEM_ROOT_PROPERTY);
		if (StringUtils.isEmpty(fsroot)) {
			throw new IllegalStateException("No property ["+FILESYSTEM_ROOT_PROPERTY+"] found");
		}
		this.root = Paths.get(fsroot);

		if (!Files.exists(root)) {
			throw new IllegalArgumentException("Credential Filesystem ["+root+"] does not exist");
		}
	}

	@Override
	public boolean hasSecret(@Nonnull CredentialAlias alias) {
		return Files.exists(root.resolve(alias.getName()));
	}

	@Override
	public ISecret getSecret(@Nonnull CredentialAlias alias) throws NoSuchElementException {
		return new FileSystemSecret(alias, root);
	}

	@Override
	public List<String> getConfiguredAliases() throws IOException {
		try(Stream<Path> stream = Files.list(root)) {
			return stream.map(Path::getFileName)
					.map(Path::toString)
					.toList();
		}
	}
}
