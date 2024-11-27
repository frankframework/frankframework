/*
 * Copyright 2021 - 2024 WeAreFrank!
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
package org.frankframework.extensions.akamai;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.Nonnull;

import org.frankframework.util.StreamUtil;

/**
 * General utility functions needed to implement the HTTP SDK.  Many of these functions are also
 * available as standard parts of other libraries, but this package strives to operate without any
 * external dependencies.
 *
 * @author colinb@akamai.com (Colin Bendell)
 */
public class NetStorageUtils {

	/**
	 * An enum of the keyed-hash algorithms supported by {@link #computeKeyedHash(byte[], String, NetStorageUtils.KeyedHashAlgorithm)}
	 * Currently supported hashes include HMAC-MD5; HMAC-SHA1; HMAC-SHA256
	 *
	 * The string representation matches the java {@link javax.crypto.Mac#getInstance(String)} cononical names.
	 */
	public enum KeyedHashAlgorithm {

		HMACMD5("HmacMD5"), HMACSHA1("HmacSHA1"), HMACSHA256("HmacSHA256");

		/**
		 * Algorithm name as defined in
		 * {@link javax.crypto.Mac#getInstance(String)}
		 */
		private final String algorithm;

		KeyedHashAlgorithm(final String algorithm) {
			this.algorithm = algorithm;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}
	}

	/**
	 * Computes the HMAC hash of a given byte[]. This is a wrapper over the Mac crypto functions.
	 * @param data byte[] of content to hash
	 * @param key secret key to salt the hash
	 * @param hashType determines which alogirthm to use. The recommendation is to use HMAC-SHA256
	 * @return a byte[] presenting the HMAC hash of the source data.
	 */
	public static byte[] computeKeyedHash(@Nonnull byte[] data, String key, KeyedHashAlgorithm hashType) {
		if(key == null) return new byte[0];

		try {
			Mac mac = Mac.getInstance(hashType.getAlgorithm());
			mac.init(new SecretKeySpec(key.getBytes(), hashType.getAlgorithm()));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new IllegalArgumentException("This should never happen!", e);
		}
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
				result.append("%s%s=%s".formatted(
						result.length() > 0 ? "&" : "",
						URLEncoder.encode(entry, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING),
						URLEncoder.encode(data.get(entry), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING)));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("This should never happen! StandardCharsets.UTF_8 is an enum!", e);
		}
		return result.toString();
	}
}
