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
import org.frankframework.doc.ReferTo;

/**
 * marker interface with default behaviour to set values in the {@link KeystoreConfiguration} object. The goal is to only support 'setKeystoreConfiguration'
 * in the future instead of all the ReferTo methods, but for now we want to support both ways of setting the keystore values.
 */
public interface HasKeystore extends IScopeProvider, HasApplicationContext {

	/**
	 * Creates a new, empty {@link KeystoreConfiguration} instance
	 * @return
	 */
	default KeystoreConfiguration createKeystoreConfiguration() {
		return new KeystoreConfiguration();
	}

	/**
	 * Set the {@link KeystoreConfiguration} object
	 */
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

	/**
	 * Resource url to keystore or certificate. If none specified, the JVMs default keystore will be used.
	 * @see KeystoreConfiguration#setKeystoreResource(String)
	 */
	default void setKeystore(String keystore) {
		getKeystoreConfiguration().setKeystoreResource(keystore);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeystoreType(KeystoreType keystoreType) {
		getKeystoreConfiguration().setKeystoreType(keystoreType);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getKeystoreConfiguration().setKeystoreAuthAlias(keystoreAuthAlias);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeystorePassword(String keystorePassword) {
		getKeystoreConfiguration().setKeystorePassword(keystorePassword);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getKeystoreConfiguration().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeystoreAlias(String keystoreAlias) {
		getKeystoreConfiguration().setKeystoreAlias(keystoreAlias);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getKeystoreConfiguration().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}

	@ReferTo(KeystoreConfiguration.class)
	default void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getKeystoreConfiguration().setKeystoreAliasPassword(keystoreAliasPassword);
	}
}
