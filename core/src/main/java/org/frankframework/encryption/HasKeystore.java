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

public interface HasKeystore extends IScopeProvider, HasApplicationContext {

	String getKeystore();
	KeystoreType getKeystoreType();
	String getKeystoreAuthAlias();
	String getKeystorePassword();
	String getKeystoreAlias();
	String getKeystoreAliasAuthAlias();
	String getKeystoreAliasPassword();
	String getKeyManagerAlgorithm();

	/** Resource url to keystore or certificate. If none specified, the JVMs default keystore will be used. */
	void setKeystore(String keystore);
	/** Type of keystore
	 * @ff.default pkcs12
	 */
	void setKeystoreType(KeystoreType keystoreType);
	/** Authentication alias used to obtain keystore password */
	void setKeystoreAuthAlias(String keystoreAuthAlias);
	/** Default password to access keystore */
	void setKeystorePassword(String keystorePassword);
	/** Key manager algorithm. Can be left empty to use the servers default algorithm */
	void setKeyManagerAlgorithm(String keyManagerAlgorithm);

	/** Alias to obtain specific certificate or key in keystore */
	void setKeystoreAlias(String keystoreAlias);
	/** Authentication alias to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias);
	/** Default password to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	void setKeystoreAliasPassword(String keystoreAliasPassword);

}
