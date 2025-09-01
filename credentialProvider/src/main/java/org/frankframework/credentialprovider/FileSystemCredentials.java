/*
   Copyright 2021 Nationale-Nederlanden, 2022-2025 WeAreFrank!

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
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class FileSystemCredentials extends Credentials {

	private final Path root;

	public FileSystemCredentials(CredentialAlias alias, Path root) {
		super(alias);

		if (root == null) {
			throw new IllegalStateException("no path provided");
		}

		this.root = root;
	}

	private void populateFieldFromFile(String folder, String file, Consumer<String> setter) throws IOException {
		Path p = Paths.get(root.toString(), folder, file);
		populateFieldFromFile(p, setter);
	}

	private void populateFieldFromFile(Path p, Consumer<String> setter) throws IOException {
		if (Files.exists(p)) {
			String fileContents = Files.readAllLines(p).get(0);
			setter.accept(fileContents);
		}
	}

	@Override
	protected void getCredentialsFromAlias(String usernameField, String passwordField) {
		try {
			Path aliasPath = Paths.get(root.toString(), getAlias());
			if (Files.exists(aliasPath)) {
				if (Files.isDirectory(aliasPath)) {
					populateFieldFromFile(getAlias(), usernameField, this::setUsername);
					populateFieldFromFile(getAlias(), passwordField, this::setPassword);
				} else {
					populateFieldFromFile(aliasPath, this::setPassword);
				}
			} else {
				throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]: alias not found");
			}
		} catch (IOException e) {
			throw new NoSuchElementException("cannot obtain credentials from authentication alias [" + getAlias() + "]", e);
		}
	}

}
