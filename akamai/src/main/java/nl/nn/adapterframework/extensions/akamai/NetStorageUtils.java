/*
 * Copyright 2017 Nationale-Nederlanden
 * Copyright 2014 Akamai Technologies http://developer.akamai.com.
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
package nl.nn.adapterframework.extensions.akamai;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import nl.nn.adapterframework.util.StreamUtil;

/**
 * General utility functions needed to implement the HTTP SDK.  Many of these functions are also
 * available as standard parts of other libraries, but this package strives to operate without any
 * external dependencies.
 *
 * @author colinb@akamai.com (Colin Bendell)
 */
public class NetStorageUtils {

	/**
	 * An enum of the hash algorithms supported by {@link #computeHash(java.io.InputStream, nl.nn.adapterframework.extensions.akamai.NetStorageUtils.HashAlgorithm)}
	 * Currently supported hashes include MD5; SHA1; SHA256
	 *
	 * The string representation matches the java {@link java.security.MessageDigest#getInstance(String)} canonical names.
	 */
	public enum HashAlgorithm {
		MD5("MD5"), SHA1("SHA-1"), SHA256("SHA-256");

		/**
		 * Algorithm name as defined in
		 * {@link java.security.MessageDigest#getInstance(String)}
		 */
		private final String algorithm;

		private HashAlgorithm(final String algorithm) {
			this.algorithm = algorithm;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}
	}

	/**
	 * An enum of the keyed-hash algorithms supported by {@link #computeKeyedHash(byte[], String, nl.nn.adapterframework.extensions.akamai.NetStorageUtils.KeyedHashAlgorithm)}
	 * Currently supported hashes include HMAC-MD5; HMAC-SHA1; HMAC-SHA256
	 *
	 * The string representation matches the java {@link javax.crypto.Mac#getInstance(String)}} cononical names.
	 */
	public enum KeyedHashAlgorithm {

		HMACMD5("HmacMD5"), HMACSHA1("HmacSHA1"), HMACSHA256("HmacSHA256");

		/**
		 * Algorithm name as defined in
		 * {@link javax.crypto.Mac#getInstance(String)}
		 */
		private final String algorithm;

		private KeyedHashAlgorithm(final String algorithm) {
			this.algorithm = algorithm;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}
	}

	/**
	 * Computes the hash of a given InputStream. This is a wrapper over the MessageDigest crypto functions.
	 *
	 * @param srcStream a source stream. This will be wrapped in a {@link java.io.BufferedInputStream}
	 *                  in case the source stream is not buffered
	 * @param hashAlgorithm the Algorithm to use to compute the hash
	 * @return a byte[] representation of the hash. If the InputStream is a null object
	 * then null will be returned. If the InputStream is empty an empty byte[] {} will be returned.
	 * @throws IOException If there is a problem with the InputStream during the compute of the hash
	 */
	public static byte[] computeHash(InputStream srcStream, HashAlgorithm hashAlgorithm) throws IOException {
		if (srcStream == null) return null;

		try {
			MessageDigest digest = MessageDigest.getInstance(hashAlgorithm.getAlgorithm());
			InputStream inputStream = new BufferedInputStream(srcStream);
			byte[] buff = new byte[1024 ^ 2];

			int size;
			while ((size = inputStream.read(buff)) != -1)
				digest.update(buff, 0, size);

			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			//no-op. This will never happen since we are using an enum to limit the hash algorithms
			throw new IllegalArgumentException("This should never happen! We are using an enum!", e);
		}
	}

	/**
	 * Computes the hash of a given InputStream. This is a wrapper over the MessageDigest crypto functions.
	 *
	 * @param srcBytes to generate a hash over
	 * @param hashAlgorithm the Algorithm to use to compute the hash
	 * @return a byte[] representation of the hash. If the InputStream is a null object
	 * then null will be returned. If the InputStream is empty an empty byte[] {} will be returned.
	 * @throws IOException If there is a problem with the InputStream during the compute of the hash
	 */
	public static byte[] computeHash(byte[] srcBytes, HashAlgorithm hashAlgorithm) throws IOException {
		if (srcBytes == null) return null;

		try {
			MessageDigest digest = MessageDigest.getInstance(hashAlgorithm.getAlgorithm());

			return digest.digest(srcBytes);
		} catch (NoSuchAlgorithmException e) {
			//no-op. This will never happen since we are using an enum to limit the hash algorithms
			throw new IllegalArgumentException("This should never happen! We are using an enum!", e);
		}
	}

	/**
	 * Converts a bytearray to a hexString
	 * TODO move this to Misc!?
	 * @param arrayBytes the input to convert
	 * @return a string with the converted input
	 */
	public static String convertByteArrayToHexString(byte[] arrayBytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}

	/**
	 * Computes the HMAC hash of a given byte[]. This is a wrapper over the Mac crypto functions.
	 * @param data byte[] of content to hash
	 * @param key secret key to salt the hash
	 * @param hashType determines which alogirthm to use. The recommendation is to use HMAC-SHA256
	 * @return a byte[] presenting the HMAC hash of the source data.
	 */
	public static byte[] computeKeyedHash(byte[] data, String key, KeyedHashAlgorithm hashType) {
		if (data == null || key == null) return null;

		try {
			Mac mac = Mac.getInstance(hashType.getAlgorithm());
			mac.init(new SecretKeySpec(key.getBytes(), hashType.getAlgorithm()));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new IllegalArgumentException("This should never happen!", e);
		}
	}

	/**
	 * Hex encoding wrapper for a byte array. The output will be 2 character padded
	 * string in lower case.
	 * @param value a byte array to encode. The assumption is that the string to encode
	 *              is small enough to be held in memory without streaming the encoding
	 * @return a 2 character zero padded string in lower case
	 */
	public static String encodeHex(byte[] value) {
		if (value == null) return null;
		StringBuilder str = new StringBuilder();
		for (byte aValue : value) str.append(String.format("%02x", aValue));

		return str.toString();
	}

	/**
	 * Lookup table for base64 encoding.
	 */
	private final static char[] BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	/**
	 * Base64-encode a byte array.
	 *
	 * @param value byte array to encode.
	 * @return Encoded string.
	 */
	public static String encodeBase64(byte[] value) {
		//TODO: JDK8 Use java.util.Base64.Encoder(value);
		if (value == null) return null;

		StringBuilder result = new StringBuilder("");
		for (int i = 0; i < value.length; i += 3) {
			int v;
			int c = 2;
			v = (value[i] & 0xff) << 16;
			if ((i + 1) < value.length) {
				v |= (value[i + 1] & 0xff) << 8;
				c = 3;
			}
			if ((i + 2) < value.length) {
				v |= (value[i + 2] & 0xff);
				c = 4;
			}
			result.append(BASE64_CHARS[(v >> 18) & 0x3f]);
			result.append(BASE64_CHARS[(v >> 12) & 0x3f]);
			result.append((c >= 3 ? BASE64_CHARS[(v >> 6) & 0x3f] : '='));
			result.append((c == 4 ? BASE64_CHARS[(v) & 0x3f] : '='));
		}
		return result.toString();
	}

	/**
	 * Convert Map<String, String> into a name=value query params string.
	 * <p/>
	 * NB: This uses URLEncoding - not URI Encoding for escaping name and values. This
	 * shouldn't be an issue for most uses of this function for the Netstorage API, but
	 * could impact non-ascii usernames in the future.
	 *
	 * @param data a Key-Value map
	 * @return a query params encoded string in the form of name=value&name2=value2...
	 */
	public static String convertMapAsQueryParams(Map<String, String> data) {
		final StringBuilder result = new StringBuilder();

		try {
			for (String entry : new TreeSet<String>(data.keySet()))
				result.append(String.format("%s%s=%s",
						result.length() > 0 ? "&" : "",
						URLEncoder.encode(entry, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING),
						URLEncoder.encode(data.get(entry), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING)));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("This should never happen! StandardCharsets.UTF_8 is an enum!", e);
		}
		return result.toString();
	}
}
