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
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

public class FileSystemSecret extends Secret {

	private final Path aliasPath;

	public FileSystemSecret(CredentialAlias alias, Path root) {
		super(alias);

		if (root == null) {
			throw new IllegalStateException("no path provided");
		}

		this.aliasPath = root.resolve(alias.getName());
		if (!Files.exists(aliasPath)) {
			throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+alias.getName()+"]: alias not found");
		}
	}

	@Override
	public String getField(String fieldname) throws IOException {
		if (StringUtils.isNotBlank(fieldname) && Files.isRegularFile(aliasPath)) {
			throw new NoSuchElementException("cannot obtain field from secret [" + this + "]");
		}

		Path field = StringUtils.isBlank(fieldname) ? aliasPath : aliasPath.resolve(fieldname);
		if (Files.exists(field)) {
			return Files.readAllLines(field).get(0);
		}

		return null;
	}

}
