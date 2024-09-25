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
package org.frankframework.extensions.tibco.pipes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Uses a Cipher algorithm similar to the one used in com.tibco.security.ObfuscationEngine used before.
 * That ObfuscationEngine is removed, but we still need to be able to 'obfuscate' strings that way.
 *
 * @author evandongen
 * @see "https://medium.com/@agrawalprince617/eliminating-tibcrypt-dependency-for-a-seamless-java-8-to-17-upgrade-23c95c82a8ad"
 */
public class ObfuscationEngine {
	private static final String PREFIX_THREE_CHARS = "#!!";

	private static final String PREFIX_TWO_CHARS = "#!";

	private static final byte[] SECRET = new byte[]{ 28, -89, -101, -111, 91, -113, 26, -70, 98, -80, -23, -53, -118, 93, -83, -17, 28, -89, -101, -111, 91, -113, 26, -70 };

	private static final String ALGORITHM = "DESede/CBC/PKCS5Padding";

	private static final String SECRET_KEY_SPEC = "DESede";

	private ObfuscationEngine() {
		// Do not construct static class
	}

	public static String encrypt(String plainString) throws Exception {
		Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(plainString.getBytes(StandardCharsets.UTF_16LE));

		try (InputStream cipherInputStream = new CipherInputStream(byteArrayInputStream, cipher)) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int bytesRead;

			outputStream.write(cipher.getIV());

			while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			return PREFIX_TWO_CHARS + new String(Base64.encodeBase64(outputStream.toByteArray()));
		}
	}

	public static String decrypt(String encryptedText) throws Exception {
		Cipher cipher = getCipher(Cipher.DECRYPT_MODE);

		String toDecode = removePrefix(encryptedText);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decodeBase64(toDecode));

		try (InputStream cipherInputStream = new CipherInputStream(byteArrayInputStream, cipher)) {
			cipherInputStream.read(cipher.getIV());

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			return outputStream.toString(StandardCharsets.UTF_16LE);
		}
	}

	/**
	 * @return a configured Cipher instance
	 */
	@SuppressWarnings({ "java:S5542", "java:S5547" })
	private static Cipher getCipher(int cipherMode) throws Exception {
		Cipher cipher = Cipher.getInstance(ALGORITHM);

		// Setting up the IV for Cipher Block Chaining (CBC)
		byte[] ivSetup = new byte[cipher.getBlockSize()];
		SecureRandom random = new SecureRandom();
		random.nextBytes(ivSetup);

		SecretKey key = new SecretKeySpec(SECRET, SECRET_KEY_SPEC);

		// Initialising the cipher object for encryption with the key and IV setup
		cipher.init(cipherMode, key, new IvParameterSpec(ivSetup));

		return cipher;
	}

	private static String removePrefix(String encryptedText) {
		if (encryptedText.startsWith(PREFIX_THREE_CHARS)) {
			return encryptedText.substring(PREFIX_THREE_CHARS.length());
		} else if (encryptedText.startsWith(PREFIX_TWO_CHARS)) {
			return encryptedText.substring(PREFIX_TWO_CHARS.length());
		}

		throw new IllegalStateException("Encrypted string does not start with encryption prefix: " + PREFIX_TWO_CHARS);
	}
}
