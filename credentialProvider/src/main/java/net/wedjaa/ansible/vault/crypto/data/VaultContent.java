/*
 * Copyright 2016 - Fabio "MrWHO" Torchetti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wedjaa.ansible.vault.crypto.data;

import java.io.IOException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class VaultContent {
	private static final Log logger = LogFactory.getLog(VaultContent.class);

	private final static String CHAR_ENCODING = "UTF-8";

	private final byte[] salt;
	private final byte[] hmac;
	private final byte[] data;

	public VaultContent(byte[] encryptedVault) throws IOException {
		byte[][] vaultContents = splitData(encryptedVault);
		salt = Util.unhex(new String(vaultContents[0], CHAR_ENCODING));
		hmac = Util.unhex(new String(vaultContents[1], CHAR_ENCODING));
		data = Util.unhex(new String(vaultContents[2], CHAR_ENCODING));
	}

	public VaultContent(byte[] salt, byte[] hmac, byte[] data) {
		this.salt = salt;
		this.hmac = hmac;
		this.data = data;
	}

	public byte[] toByteArray() {
		return toString().getBytes();
	}

	public String toString() {
		if (logger.isDebugEnabled()) {
			logger.debug("Salt: %d - HMAC: %d - Data: %d - TargetLen: %d".formatted(salt.length, hmac.length,
					data.length, (salt.length + hmac.length + data.length) * 2));
		}

		String saltString = Util.hexit(salt);

		if (logger.isDebugEnabled()) {
			logger.debug("Salt String Length: %s".formatted(saltString.length()));
		}

		String hmacString = Util.hexit(hmac);

		if (logger.isDebugEnabled()) {
			logger.debug("HMAC String Length: %s".formatted(hmacString.length()));
		}
		String dataString = Util.hexit(data, -1);
		if (logger.isDebugEnabled()) {
			logger.debug("DATA String Length: %s".formatted(dataString.length()));
		}
		String complete = saltString + "\n" + hmacString + "\n" + dataString;
		if (logger.isDebugEnabled()) {
			logger.debug("Complete: %d - %s".formatted(complete.length(), complete));
		}
		String result = Util.hexit(complete.getBytes(), 80);
		if (logger.isDebugEnabled()) {
			logger.debug("Result: [%d] %d - %s".formatted(complete.length() * 2, result.length(), result));
		}
		return result;
	}

	private int[] getDataLengths(byte[] encodedData) throws IOException {

		int[] result = new int[3];

		int idx = 0;
		int saltLen = 0;
		while (encodedData[idx] != '\n' && idx < encodedData.length) {
			saltLen++;
			idx++;
		}
		// Skip the newline
		idx++;
		if (idx == encodedData.length) {
			throw new IOException("Malformed data - salt incomplete");
		}
		result[0] = saltLen;

		int hmacLen = 0;
		while (encodedData[idx] != '\n' && idx < encodedData.length) {
			hmacLen++;
			idx++;
		}
		// Skip the newline
		idx++;
		if (idx == encodedData.length) {
			throw new IOException("Malformed data - hmac incomplete");
		}
		result[1] = hmacLen;
		int dataLen = 0;
		while (idx < encodedData.length) {
			dataLen++;
			idx++;
		}
		result[2] = dataLen;

		return result;
	}

	private byte[][] splitData(byte[] encodedData) throws IOException {
		int[] partsLength = getDataLengths(encodedData);

		byte[][] result = new byte[3][];

		int idx = 0;
		int saltIdx = 0;
		result[0] = new byte[partsLength[0]];
		while (encodedData[idx] != '\n' && idx < encodedData.length) {
			result[0][saltIdx++] = encodedData[idx++];
		}
		// Skip the newline
		idx++;
		if (idx == encodedData.length) {
			throw new IOException("Malformed data - salt incomplete");
		}
		int macIdx = 0;
		result[1] = new byte[partsLength[1]];
		while (encodedData[idx] != '\n' && idx < encodedData.length) {
			result[1][macIdx++] = encodedData[idx++];
		}
		// Skip the newline
		idx++;
		if (idx == encodedData.length) {
			throw new IOException("Malformed data - hmac incomplete");
		}
		int dataIdx = 0;
		result[2] = new byte[partsLength[2]];
		while (idx < encodedData.length) {
			result[2][dataIdx++] = encodedData[idx++];
		}
		return result;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getHmac() {
		return hmac;
	}

	public byte[] getData() {
		return data;
	}
}
