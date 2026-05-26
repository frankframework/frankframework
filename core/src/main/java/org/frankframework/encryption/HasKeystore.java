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
 * marker interface with default behaviour to set values in the Keystore object. The goal is to only support 'setKeystore' in the future
 * instead of all the ReferTo methods, but for now we want to support both ways of setting the keystore values.
 */
public interface HasKeystore extends IScopeProvider, HasApplicationContext {

	default Keystore createKeystore() {
		return new Keystore();
	}

	void setKeystore(Keystore keystore);

	default Keystore getKeystore() {
		return null;
	};

	@ReferTo(Keystore.class)
	default String getKeystoreResource() {
		return getKeystore().getKeystoreResource();
	}

	default KeystoreType getKeystoreType() {
		return getKeystore().getKeystoreType();
	}

	default String getKeystoreAuthAlias() {
		return getKeystore().getKeystoreAuthAlias();
	}

	default String getKeystorePassword() {
		return getKeystore().getKeystorePassword();
	}

	default String getKeystoreAlias() {
		return getKeystore().getKeystoreAlias();
	}

	default String getKeystoreAliasAuthAlias() {
		return getKeystore().getKeystoreAliasAuthAlias();
	}

	default String getKeystoreAliasPassword() {
		return getKeystore().getKeystoreAliasPassword();
	}

	default String getKeyManagerAlgorithm() {
		return getKeystore().getKeyManagerAlgorithm();
	}

	default void setKeystore(String keystore) {
		getKeystore().setKeystoreResource(keystore);
	};

	default void setKeystoreType(KeystoreType keystoreType) {
		getKeystore().setKeystoreType(keystoreType);
	};

	default void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getKeystore().setKeystoreAuthAlias(keystoreAuthAlias);
	};

	default void setKeystorePassword(String keystorePassword) {
		getKeystore().setKeystorePassword(keystorePassword);
	};

	default void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getKeystore().setKeyManagerAlgorithm(keyManagerAlgorithm);
	};

	default void setKeystoreAlias(String keystoreAlias) {
		getKeystore().setKeystoreAlias(keystoreAlias);
	};

	default void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getKeystore().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	};

	default void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getKeystore().setKeystoreAliasPassword(keystoreAliasPassword);
	};
}
