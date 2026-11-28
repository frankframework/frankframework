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
package org.frankframework.http.rest;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Verifies an HMAC signature sent alongside a request, computed over the raw request body.
 * This is the scheme used by most webhook providers to authenticate their callbacks.
 * <p>
 * The supplied signature is decoded to bytes and compared against the computed MAC with
 * {@link MessageDigest#isEqual(byte[], byte[])}, so the comparison does not leak timing information.
 * The secret is always encoded as UTF-8, so the result does not depend on the platform default charset.
 * </p>
 */
public class HmacSignatureVerifier {

	/** MAC algorithms considered suitable for authenticating a request. */
	public enum HmacAlgorithm {
		HmacSHA256, HmacSHA384, HmacSHA512
	}

	/** Text encoding of the signature as it appears in the request header. */
	public enum SignatureEncoding {
		HEX, BASE64
	}

	private final @NonNull HmacAlgorithm algorithm;
	private final @NonNull SignatureEncoding encoding;
	private final @Nullable String signaturePrefix;
	private final byte[] secret;

	public HmacSignatureVerifier(@NonNull HmacAlgorithm algorithm, @NonNull SignatureEncoding encoding, @Nullable String signaturePrefix, @NonNull String secret) {
		if (StringUtils.isEmpty(secret)) {
			throw new IllegalArgumentException("secret may not be empty");
		}
		this.algorithm = algorithm;
		this.encoding = encoding;
		this.signaturePrefix = signaturePrefix;
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Computes the MAC over the given body and compares it with the signature supplied by the caller.
	 *
	 * @return true when the signature is present, decodable and matches.
	 */
	public boolean verify(byte[] body, @Nullable String suppliedSignature) throws NoSuchAlgorithmException, InvalidKeyException {
		final byte[] supplied = extractSignature(suppliedSignature);
		if (supplied == null) {
			return false;
		}
		return MessageDigest.isEqual(computeMac(body), supplied);
	}

	private @Nullable byte[] extractSignature(@Nullable String suppliedSignature) {
		if (StringUtils.isBlank(suppliedSignature)) {
			return null;
		}
		String value = suppliedSignature.trim();
		if (StringUtils.isNotEmpty(signaturePrefix)) {
			if (!value.startsWith(signaturePrefix)) {
				return null;
			}
			value = value.substring(signaturePrefix.length());
		}
		return decode(value);
	}

	private @Nullable byte[] decode(String value) {
		try {
			return switch (encoding) {
				case HEX -> Hex.decodeHex(value);
				case BASE64 -> Base64.getDecoder().decode(value);
			};
		} catch (DecoderException | IllegalArgumentException e) {
			return null; // an undecodable signature can never match, and is not worth an exception
		}
	}

	byte[] computeMac(byte[] body) throws NoSuchAlgorithmException, InvalidKeyException {
		final Mac mac = Mac.getInstance(algorithm.name());
		mac.init(new SecretKeySpec(secret, algorithm.name()));
		return mac.doFinal(body);
	}
}
