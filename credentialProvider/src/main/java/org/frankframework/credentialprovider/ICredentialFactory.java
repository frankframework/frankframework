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

import java.util.Collection;
import java.util.function.Supplier;

public interface ICredentialFactory {

	/**
	 * initialize() of an implementation can throw an exception when the credentialFactory cannot be properly configured and used.
	 */
	void initialize() throws Exception;

	boolean hasCredentials(String alias);

	ICredentials getCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier);

	/**
	 * return a list of all configured aliases, or null if such a list cannot be provided.
	 * @throws Exception
	 */
	Collection<String> getConfiguredAliases() throws Exception;
}
