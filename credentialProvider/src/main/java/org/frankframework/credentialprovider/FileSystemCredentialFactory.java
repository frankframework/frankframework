/*
   Copyright 2021, 2022 WeAreFrank!

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.credentialprovider.util.CredentialConstants;

/**
 * It's required to set the property {@value FileSystemCredentialFactory#FILESYSTEM_ROOT_PROPERTY} to the directory you wish to read credentials from.
 */
public class FileSystemCredentialFactory implements ICredentialFactory {

	public static final String FILESYSTEM_ROOT_PROPERTY="credentialFactory.filesystem.root";
	public static final String USERNAME_FILE_PROPERTY="credentialFactory.filesystem.usernamefile";
	public static final String PASSWORD_FILE_PROPERTY="credentialFactory.filesystem.passwordfile";

	public static final String USERNAME_FILE_DEFAULT="username";
	public static final String PASSWORD_FILE_DEFAULT="password";

	private Path root;
	private String usernamefile;
	private String passwordfile;

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

		usernamefile = appConstants.getProperty(USERNAME_FILE_PROPERTY, USERNAME_FILE_DEFAULT);
		passwordfile = appConstants.getProperty(PASSWORD_FILE_PROPERTY, PASSWORD_FILE_DEFAULT);
	}

	@Override
	public boolean hasCredentials(String alias) {
		return Files.exists(Paths.get(root.toString(), alias));
	}

	@Override
	public ICredentials getCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		return new FileSystemCredentials(alias, defaultUsernameSupplier, defaultPasswordSupplier, usernamefile, passwordfile, root);
	}

	@Override
	public List<String> getConfiguredAliases() throws Exception{
		List<String> aliases;
		try(Stream<Path> stream = Files.list(Paths.get(root.toString()))) {
			aliases = stream.map(p->p.getFileName().toString())
					.collect(Collectors.toList());
		}
		return aliases;
	}
}
