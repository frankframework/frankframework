/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.pipes.hash;

/**
 * Defines the supported algorithms which can be used in the {@link org.frankframework.pipes.HashPipe}
 */
public enum Algorithm {
	MD5,
	SHA,
	SHA256("SHA-256"),
	SHA384("SHA-384"),
	SHA512("SHA-512"),
	CRC32,
	ADLER32,
	HmacMD5(true),
	HmacSHA1(true),
	HmacSHA256(true),
	HmacSHA384(true),
	HmacSHA512(true);

	private final String algorithm;

	private final boolean secretRequired;

	Algorithm(String algorithm, boolean secretRequired) {
		this.algorithm = algorithm;
		this.secretRequired = secretRequired;
	}

	Algorithm(boolean secretRequired) {
		this(null, secretRequired);
	}

	Algorithm(String algorithm) {
		this(algorithm, false);
	}

	Algorithm() {
		this(null, false);
	}

	public String getAlgorithm() {
		return algorithm != null ? algorithm : name();
	}

	public boolean isSecretRequired() {
		return secretRequired;
	}
}
