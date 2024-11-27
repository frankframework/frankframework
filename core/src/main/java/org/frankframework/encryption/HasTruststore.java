/*
   Copyright 2021 WeAreFrank!

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

import org.frankframework.core.IScopeProvider;
import org.frankframework.doc.Unsafe;

public interface HasTruststore extends IScopeProvider {

	String getTruststore();
	KeystoreType getTruststoreType();
	String getTruststoreAuthAlias();
	String getTruststorePassword();
	String getTrustManagerAlgorithm();

	boolean isVerifyHostname();
	boolean isAllowSelfSignedCertificates();
	boolean isIgnoreCertificateExpiredException();

	/** Resource url to truststore. If none specified, the JVMs default truststore will be used. */
	void setTruststore(String truststore);
	/** Type of truststore
	 * @ff.default jks
	 */
	void setTruststoreType(KeystoreType truststoreType);
	/** Authentication alias used to obtain truststore password */
	void setTruststoreAuthAlias(String truststoreAuthAlias);
	/** Default password to access truststore */
	void setTruststorePassword(String truststorePassword);
	/** Trust manager algorithm. Can be left empty to use the servers default algorithm  */
	void setTrustManagerAlgorithm(String trustManagerAlgorithm);

	/** If <code>true</code>, the hostname in the certificate will be checked against the actual hostname of the peer
	 * @ff.default false
	 */
	@Unsafe
	void setVerifyHostname(boolean verifyHostname);

	/** If <code>true</code>, self signed certificates are accepted
	 * @ff.default false
	 */
	@Unsafe
	void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates);

	/**
	 * If <code>true</code>, CertificateExpiredExceptions are ignored
	 * @ff.default false
	 */
	@Unsafe
	void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException);
}
