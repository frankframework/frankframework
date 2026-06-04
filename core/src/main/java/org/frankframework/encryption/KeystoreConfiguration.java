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
package org.frankframework.encryption;

import lombok.Data;

/**
 * Container for Keystore data
 */
@Data
public class KeystoreConfiguration {
	/**
	 * Resource url to keystore or certificate. If none specified, the JVMs default keystore will be used.
	 */
	private String keystoreResource;

	/** Authentication alias used to obtain keystore password */
	private String keystoreAuthAlias;

	/** Default password to access keystore */
	private String keystorePassword;

	/**
	 * Type of keystore
	 * @ff.default pkcs12
	 */
	private KeystoreType keystoreType = KeystoreType.PKCS12;

	/** Alias to obtain specific certificate or key in keystore */
	private String keystoreAlias;

	/** Authentication alias to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	private String keystoreAliasAuthAlias;

	/** Default password to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	private String keystoreAliasPassword;

	/** Key manager algorithm. Can be left empty to use the servers default algorithm */
	private String keyManagerAlgorithm;
}
