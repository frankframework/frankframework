/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import lombok.Getter;
import org.apache.commons.codec.binary.Base64;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

/**
 * This pipe can be used to generate a hash for the given message using an algorithm. With this, you can prove integrity of the message. If you
 * need to prove the authenticity of the message as well, please use the {@link HashPipe} which uses an algorithm and a secret to prove both
 * integrity and authenticity.
 * <p>
 * The hash is generated based on the bytes of the given input message or on the bytes read from the file path if @{inputIsFile} is @{code true}
 * <p>
 * The supported algorithms are:
 * <ul>
 *     <li>CRC32</li>
 *     <li>Adler32</li>
 *     <li>MD5</li>
 *     <li>SHA</li>
 *     <li>SHA256</li>
 *     <li>SHA384</li>
 *     <li>SHA512</li>
 * </ul>
 *
 * CRC32 and Adler32 are {@link Checksum} implementations, the others are {@link MessageDigest} implementations.
 * <p>
 * Example usage:
 * <pre>{@code
 * <pipe className="org.frankframework.pipes.ChecksumPipe"
 *     name="SHA"
 *     algorithm="SHA">
 *     <forward name="success" path="READY"/>
 * </pipe>
 * }</pre>
 *
 * @author Gerrit van Brakel
 * @since 4.9
 */
public class ChecksumPipe extends FixedForwardPipe {

	private @Getter String charset;
	private @Getter Algorithm algorithm = Algorithm.MD5;
	private @Getter boolean inputIsFile;
	private @Getter HashPipe.HashEncoding hashEncoding = HashPipe.HashEncoding.Hex;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			ChecksumGenerator checksumGenerator = ChecksumGenerator.getInstance(getAlgorithm());
			byte[] barr = new byte[1000];
			try (InputStream fis = isInputIsFile() ? new FileInputStream(message.asString()) : message.asInputStream(getCharset())) {
				int c;
				while ((c = fis.read(barr)) >= 0) {
					checksumGenerator.update(barr, 0, c);
				}
			}

			return new PipeRunResult(getSuccessForward(), checksumGenerator.getResult(hashEncoding));
		} catch (Exception e) {
			throw new PipeRunException(this, "cannot calculate [" + getAlgorithm() + "]" + (isInputIsFile() ? " on file [" + message + "]" : " using charset [" + getCharset() + "]"), e);
		}
	}

	/**
	 * Character encoding to be used to encode message before calculating checksum.
	 */
	public void setCharset(String string) {
		charset = string;
	}

	/**
	 * Type of Algorithm to use the generate a checksum for the message
	 *
	 * @ff.default MD5
 	 * @Deprecated(forRemoval = true, since = "8.3.0")
	 * @ConfigurationWarning("Please use setAlgorithm to set the type of algorithm you want to use. The possible values remain the same")
	 */
	public void setType(Algorithm value) {
		setAlgorithm(value);
	}

	/**
	 * If set <code>true</code>, the input is assumed to be a filename; otherwise the input itself is used in the calculations.
	 *
	 * @ff.default false
	 */
	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("Please use fileSystemPipe to read the file first.")
	public void setInputIsFile(boolean b) {
		inputIsFile = b;
	}

	/**
	 * The algorithm to use to generate the checksum for the input message.
	 *
	 * @ff.default ChecksumType.MD5
	 * @param algorithm
	 */
	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * Method to use for converting the hash from bytes to String
	 * @ff.default HashEncoding.Hex
	 */
	public void setHashEncoding(HashPipe.HashEncoding hashEncoding) {
		this.hashEncoding = hashEncoding;
	}

	public enum Algorithm {
		MD5,
		SHA,
		SHA256("SHA-256"),
		SHA384("SHA-384"),
		SHA512("SHA-512"),
		CRC32,
		ADLER32;

		private final String algorithm;

		Algorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		Algorithm() {
			this(null);
		}

		public String getAlgorithm() {
			return algorithm != null ? algorithm : name();
		}
	}

	protected interface ChecksumGenerator {
		void update(byte[] b, int offset, int length);

		String getResult(HashPipe.HashEncoding hashEncoding);

		static ChecksumGenerator getInstance(Algorithm algorithm) throws NoSuchAlgorithmException {
			return switch (algorithm) {
				case MD5, SHA, SHA256, SHA384, SHA512 -> new MessageDigestGenerator(algorithm);
				case CRC32 -> new BasicGenerator(new CRC32());
				case ADLER32 -> new BasicGenerator(new Adler32());
			};
		}
	}

	static class BasicGenerator implements ChecksumGenerator {
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

			return Long.toHexString(checksum.getValue());
		}
	}

	static class MessageDigestGenerator implements ChecksumGenerator {
		private final MessageDigest messageDigest;

		MessageDigestGenerator(Algorithm type) throws NoSuchAlgorithmException {
			super();
			this.messageDigest = MessageDigest.getInstance(type.getAlgorithm());
		}

		@Override
		public void update(byte[] b, int offset, int length) {
			messageDigest.update(b, offset, length);
		}

		@Override
		public String getResult(HashPipe.HashEncoding hashEncoding) {
			if (hashEncoding == HashPipe.HashEncoding.Base64) {
				return Base64.encodeBase64String(messageDigest.digest());
			}

			return new BigInteger(1, messageDigest.digest()).toString(16);
		}
	}
}
