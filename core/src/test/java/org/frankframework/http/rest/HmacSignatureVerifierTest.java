package org.frankframework.http.rest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.frankframework.http.rest.HmacSignatureVerifier.HmacAlgorithm;
import org.frankframework.http.rest.HmacSignatureVerifier.SignatureEncoding;

public class HmacSignatureVerifierTest {

	private static final String SECRET = "it-is-a-secret";
	private static final byte[] BODY = "{\"event\":\"ping\"}".getBytes(StandardCharsets.UTF_8);

	private static String sign(HmacAlgorithm algorithm, String secret, byte[] body, SignatureEncoding encoding) throws Exception {
		Mac mac = Mac.getInstance(algorithm.name());
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm.name()));
		byte[] result = mac.doFinal(body);
		return encoding == SignatureEncoding.HEX ? Hex.encodeHexString(result) : Base64.getEncoder().encodeToString(result);
	}

	@ParameterizedTest
	@EnumSource(HmacAlgorithm.class)
	public void acceptsCorrectHexSignatureForEveryAlgorithm(HmacAlgorithm algorithm) throws Exception {
		// Arrange
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(algorithm, SignatureEncoding.HEX, null, SECRET);
		String signature = sign(algorithm, SECRET, BODY, SignatureEncoding.HEX);

		// Act / Assert
		assertTrue(verifier.verify(BODY, signature));
	}

	@ParameterizedTest
	@EnumSource(SignatureEncoding.class)
	public void acceptsCorrectSignatureForEveryEncoding(SignatureEncoding encoding) throws Exception {
		// Arrange
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, encoding, null, SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA256, SECRET, BODY, encoding);

		// Act / Assert
		assertTrue(verifier.verify(BODY, signature));
	}

	@Test
	public void acceptsUppercaseHexSignature() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA256, SECRET, BODY, SignatureEncoding.HEX);

		assertTrue(verifier.verify(BODY, signature.toUpperCase()));
	}

	@Test
	public void acceptsSignatureWithConfiguredPrefix() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, "sha256=", SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA256, SECRET, BODY, SignatureEncoding.HEX);

		assertTrue(verifier.verify(BODY, "sha256=" + signature));
	}

	@Test
	public void rejectsSignatureMissingTheConfiguredPrefix() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, "sha256=", SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA256, SECRET, BODY, SignatureEncoding.HEX);

		assertFalse(verifier.verify(BODY, signature));
	}

	@Test
	public void rejectsSignatureMadeWithAnotherSecret() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA256, "another-secret", BODY, SignatureEncoding.HEX);

		assertFalse(verifier.verify(BODY, signature));
	}

	@Test
	public void rejectsSignatureOverAnotherBody() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA256, SECRET, "{\"event\":\"tampered\"}".getBytes(StandardCharsets.UTF_8), SignatureEncoding.HEX);

		assertFalse(verifier.verify(BODY, signature));
	}

	@Test
	public void rejectsMissingEmptyAndUndecodableSignatures() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, SECRET);

		assertFalse(verifier.verify(BODY, null), "missing header must not authenticate");
		assertFalse(verifier.verify(BODY, ""), "empty header must not authenticate");
		assertFalse(verifier.verify(BODY, "   "), "blank header must not authenticate");
		assertFalse(verifier.verify(BODY, "not-hex-at-all"), "undecodable signature must not authenticate");
	}

	@Test
	public void rejectsSignatureFromAnotherAlgorithm() throws Exception {
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, SECRET);
		String signature = sign(HmacAlgorithm.HmacSHA512, SECRET, BODY, SignatureEncoding.HEX);

		assertFalse(verifier.verify(BODY, signature));
	}

	@Test
	public void secretIsEncodedAsUtf8RegardlessOfPlatformDefault() throws Exception {
		// Arrange: a non-ASCII secret, where the platform default charset would otherwise decide the bytes
		String nonAscii = "pâté";
		HmacSignatureVerifier verifier = new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, nonAscii);

		// Act
		byte[] actual = verifier.computeMac(BODY);

		// Assert
		Mac utf8 = Mac.getInstance("HmacSHA256");
		utf8.init(new SecretKeySpec(nonAscii.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		assertArrayEquals(utf8.doFinal(BODY), actual);
	}

	@Test
	public void emptySecretIsRejectedOnConstruction() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> new HmacSignatureVerifier(HmacAlgorithm.HmacSHA256, SignatureEncoding.HEX, null, ""));
		assertEquals("secret may not be empty", e.getMessage());
	}
}
