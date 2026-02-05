/*
   Copyright 2021-2026 WeAreFrank!

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

import java.util.Collection;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;

/**
 * <p>Interface for a CredentialFactory. A CredentialFactory is responsible for providing credentials to the framework.</p>
 *
 * <p>Implementations of this interface should be registered in the {@code credentialproperties.properties} file.</p>
 *
 * <p>Implementations of this interface should be thread-safe.</p>
 */
public interface ISecretProvider {

	/**
	 * initialize() of an implementation can throw an exception when the credentialFactory cannot be properly configured and used.
	 */
	@SuppressWarnings("java:S112")
	void initialize() throws Exception;

	boolean hasSecret(CredentialAlias alias);

	ISecret getSecret(CredentialAlias alias) throws NoSuchElementException;

	/**
	 * Return a list of all configured aliases, or null if such a list cannot be provided.
	 * @throws Exception Can throw an exception when failing to get the configured aliases.
	 */
	@SuppressWarnings("java:S112")
	@Nullable Collection<String> getConfiguredAliases() throws Exception;
}
