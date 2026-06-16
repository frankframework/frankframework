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

@Data
public class TruststoreConfiguration {
	/**
	 * Resource url to truststore or certificate. If none specified, the JVMs default truststore will be used.
	 */
	private String truststoreResource;

	/** Authentication alias used to obtain truststore password */
	private String truststoreAuthAlias;

	/** Default password to access truststore */
	private String truststorePassword;

	/**
	 * Type of truststore
	 * @ff.default JKS
	 */
	private KeystoreType truststoreType = KeystoreType.JKS;

	/** Alias to obtain specific certificate in truststore */
	private String truststoreAlias;

	/** Authentication alias to authenticate access to certificate indicated by <code>truststoreAlias</code> */
	private String truststoreAliasAuthAlias;

	/** Default password to authenticate access to certificate indicated by <code>truststoreAlias</code> */
	private String truststoreAliasPassword;

	/** Trust manager algorithm. Can be left empty to use the servers default algorithm */
	private String trustManagerAlgorithm;

	/**
	 * If <code>true</code>, the hostname in the certificate will be checked against the actual hostname of the peer
	 * @ff.default true
	 */
	private boolean verifyHostname = true;

	/**
	 * If <code>true</code>, self-signed certificates are accepted
	 * @ff.default false
	 */
	private boolean allowSelfSignedCertificates = false;

	/**
	 * If <code>true</code>, CertificateExpiredExceptions are ignored
	 * @ff.default false
	 */
	private boolean ignoreCertificateExpiredException = false;
}
