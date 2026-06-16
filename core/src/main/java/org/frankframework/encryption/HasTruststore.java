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
import org.frankframework.doc.ReferTo;
import org.frankframework.doc.Unsafe;

/**
 * marker interface with default behaviour to set values in the {@link TruststoreConfiguration} object. The goal is to only support 'setTruststoreConfiguration'
 * in the future instead of all the ReferTo methods, but for now we want to support both ways of setting the keystore values.
 */
public interface HasTruststore extends IScopeProvider {

	/**
	 * creates a new, empty {@link TruststoreConfiguration} instance
	 */
	default TruststoreConfiguration createTruststoreConfiguration() {
		return new TruststoreConfiguration();
	}

	void setTruststoreConfiguration(TruststoreConfiguration truststoreConfiguration);

	TruststoreConfiguration getTruststoreConfiguration();

	default String getTruststore() {
		return getTruststoreConfiguration().getTruststoreResource();
	}

	default KeystoreType getTruststoreType() {
		return getTruststoreConfiguration().getTruststoreType();
	}

	default String getTruststoreAuthAlias() {
		return getTruststoreConfiguration().getTruststoreAuthAlias();
	}

	default String getTruststorePassword() {
		return getTruststoreConfiguration().getTruststorePassword();
	}

	default String getTrustManagerAlgorithm() {
		return getTruststoreConfiguration().getTrustManagerAlgorithm();
	}

	default boolean isVerifyHostname() {
		return getTruststoreConfiguration().isVerifyHostname();
	}
	default boolean isAllowSelfSignedCertificates() {
		return getTruststoreConfiguration().isAllowSelfSignedCertificates();

	}
	default boolean isIgnoreCertificateExpiredException() {
		return getTruststoreConfiguration().isIgnoreCertificateExpiredException();
	}

	/**
	 * Resource url to truststore. If none specified, the JVMs default truststore will be used.
	 * @see TruststoreConfiguration#setTruststoreResource(String)
	 */
	default void setTruststore(String truststore) {
		getTruststoreConfiguration().setTruststoreResource(truststore);
	}

	@ReferTo(TruststoreConfiguration.class)
	default void setTruststoreType(KeystoreType truststoreType) {
		getTruststoreConfiguration().setTruststoreType(truststoreType);
	}

	@ReferTo(TruststoreConfiguration.class)
	default void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getTruststoreConfiguration().setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@ReferTo(TruststoreConfiguration.class)
	default void setTruststorePassword(String truststorePassword) {
		getTruststoreConfiguration().setTruststorePassword(truststorePassword);
	}

	@ReferTo(TruststoreConfiguration.class)
	default void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getTruststoreConfiguration().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}

	@ReferTo(TruststoreConfiguration.class)
	@Unsafe
	default void setVerifyHostname(boolean verifyHostname) {
		getTruststoreConfiguration().setVerifyHostname(verifyHostname);
	}

	@ReferTo(TruststoreConfiguration.class)
	@Unsafe
	default void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		getTruststoreConfiguration().setAllowSelfSignedCertificates(allowSelfSignedCertificates);
	}

	@ReferTo(TruststoreConfiguration.class)
	@Unsafe
	default void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException){
		getTruststoreConfiguration().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
}
