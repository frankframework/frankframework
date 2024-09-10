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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import org.frankframework.pipes.HashPipe;

/**
 * HashGenerator interface and different implementations to work with.
 *
 * @see HashPipe
 */
public interface HashGenerator {
	void update(byte[] b, int offset, int length);

	String getResult(HashPipe.HashEncoding hashEncoding);

	default String getHashString(HashPipe.HashEncoding hashEncoding, byte[] result) {
		return switch (hashEncoding) {
			case Base64 -> Base64.encodeBase64String(result);
			case Hex -> Hex.encodeHexString(result);
		};
	}

	static HashGenerator getInstance(Algorithm algorithm, @Nullable SecretKeySpec secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
		return switch (algorithm) {
			case MD5, SHA, SHA256, SHA384, SHA512 -> new MessageDigestGenerator(algorithm);
			case CRC32 -> new BasicGenerator(new CRC32());
			case ADLER32 -> new BasicGenerator(new Adler32());
			case HmacMD5, HmacSHA1, HmacSHA256, HmacSHA384,	HmacSHA512 -> new MacGenerator(algorithm, secretKey);
		};
	}
}

class BasicGenerator implements HashGenerator {
	private final Checksum checksum;

	BasicGenerator(Checksum checksum) {
		super();
		this.checksum = checksum;
		checksum.reset();
	}

	@Override
	public void update(byte[] b, int offset, int length) {
		checksum.update(b, offset, length);
	}

	@Override
	public String getResult(HashPipe.HashEncoding hashEncoding) {
		if (hashEncoding == HashPipe.HashEncoding.Base64) {
			return Base64.encodeBase64String(BigInteger.valueOf(checksum.getValue()).toByteArray());
		}

		// By using getHashString, `Hex.encodeHexString(result)` is used, which results in a left padded String with '00'
		return Long.toHexString(checksum.getValue());
	}
}

class MessageDigestGenerator implements HashGenerator {
	private final MessageDigest messageDigest;

	MessageDigestGenerator(Algorithm algorithm) throws NoSuchAlgorithmException {
		super();
		this.messageDigest = MessageDigest.getInstance(algorithm.getAlgorithm());
	}

	@Override
	public void update(byte[] b, int offset, int length) {
		messageDigest.update(b, offset, length);
	}

	@Override
	public String getResult(HashPipe.HashEncoding hashEncoding) {
		return getHashString(hashEncoding, messageDigest.digest());
	}
}

class MacGenerator implements HashGenerator {
	private final Mac mac;

	public MacGenerator(Algorithm algorithm, SecretKeySpec secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
		super();
		this.mac = Mac.getInstance(algorithm.getAlgorithm());
		this.mac.init(secretKey);
	}

	@Override
	public void update(byte[] b, int offset, int length) {
		mac.update(b, 0, length);
	}

	@Override
	public String getResult(HashPipe.HashEncoding hashEncoding){
		return getHashString(hashEncoding, mac.doFinal());
	}
}
