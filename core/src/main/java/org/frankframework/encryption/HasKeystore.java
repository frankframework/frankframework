/*
   Copyright 2021-2022, 2025 WeAreFrank!

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
package org.frankframework.encryption;

import org.frankframework.core.HasApplicationContext;
import org.frankframework.core.IScopeProvider;

/**
 * marker interface with default behaviour to set values in the Keystore object. The goal is to only support 'setKeystore' in the future
 * instead of all the ReferTo methods, but for now we want to support both ways of setting the keystore values.
 */
public interface HasKeystore extends IScopeProvider, HasApplicationContext {

	default KeystoreConfiguration createKeystoreConfiguration() {
		return new KeystoreConfiguration();
	}

	void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration);

	KeystoreConfiguration getKeystoreConfiguration();

	default String getKeystore() {
		return getKeystoreConfiguration().getKeystoreResource();
	}

	default KeystoreType getKeystoreType() {
		return getKeystoreConfiguration().getKeystoreType();
	}

	default String getKeystoreAuthAlias() {
		return getKeystoreConfiguration().getKeystoreAuthAlias();
	}

	default String getKeystorePassword() {
		return getKeystoreConfiguration().getKeystorePassword();
	}

	default String getKeystoreAlias() {
		return getKeystoreConfiguration().getKeystoreAlias();
	}

	default String getKeystoreAliasAuthAlias() {
		return getKeystoreConfiguration().getKeystoreAliasAuthAlias();
	}

	default String getKeystoreAliasPassword() {
		return getKeystoreConfiguration().getKeystoreAliasPassword();
	}

	default String getKeyManagerAlgorithm() {
		return getKeystoreConfiguration().getKeyManagerAlgorithm();
	}

	default void setKeystore(String keystore) {
		getKeystoreConfiguration().setKeystoreResource(keystore);
	}

	default void setKeystoreType(KeystoreType keystoreType) {
		getKeystoreConfiguration().setKeystoreType(keystoreType);
	}

	default void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getKeystoreConfiguration().setKeystoreAuthAlias(keystoreAuthAlias);
	}

	default void setKeystorePassword(String keystorePassword) {
		getKeystoreConfiguration().setKeystorePassword(keystorePassword);
	}

	default void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getKeystoreConfiguration().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	default void setKeystoreAlias(String keystoreAlias) {
		getKeystoreConfiguration().setKeystoreAlias(keystoreAlias);
	}

	default void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getKeystoreConfiguration().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}

	default void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getKeystoreConfiguration().setKeystoreAliasPassword(keystoreAliasPassword);
	}
}
