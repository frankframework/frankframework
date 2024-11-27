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
package org.frankframework.extensions.akamai;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.Getter;

import org.frankframework.stream.Message;
import org.frankframework.util.UUIDUtil;

/**
 * An enum of the hash algorithms. Currently supported hashes include MD5; SHA1; SHA256
 *
 * The string representation matches the java {@link java.security.MessageDigest#getInstance(String)} canonical names.
 *
 * @author Niels Meijer
 */
public enum HashAlgorithm {
	MD5("MD5"), SHA1("SHA-1"), SHA256("SHA-256");

	/**
	 * Algorithm name as defined in {@link java.security.MessageDigest#getInstance(String)}
	 */
	private final @Getter String algorithm;

	HashAlgorithm(final String algorithm) {
		this.algorithm = algorithm;
	}

	public String computeHash(Message file) throws IOException {
		if (Message.isEmpty(file)) {
			throw new IllegalStateException("unable to compute hash over null message");
		}
		byte[] fileBytes = file.asByteArray();

		byte[] checksum = computeHash(fileBytes, this);
		if(checksum != null) {
			return UUIDUtil.asHex(checksum);
		}

		throw new IllegalStateException("error computing checksum");
	}

	/**
	 * Computes the hash of a given InputStream. This is a wrapper over the MessageDigest crypto functions.
	 *
	 * @param srcBytes to generate a hash over
	 * @param hashAlgorithm the Algorithm to use to compute the hash
	 * @return a byte[] representation of the hash. If the InputStream is a null object
	 * then null will be returned. If the InputStream is empty an empty byte[] {} will be returned.
	 */
	private static byte[] computeHash(byte[] srcBytes, HashAlgorithm hashAlgorithm) {
		try {
			MessageDigest digest = MessageDigest.getInstance(hashAlgorithm.getAlgorithm());

			return digest.digest(srcBytes);
		} catch (NoSuchAlgorithmException e) {
			//no-op. This will never happen since we are using an enum to limit the hash algorithms
			throw new IllegalArgumentException("This should never happen! We are using an enum!", e);
		}
	}
}
