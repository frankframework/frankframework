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
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

/**
 * Pipe to calculate checksum on input.
 *
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class ChecksumPipe extends FixedForwardPipe {

	private @Getter String charset;
	private @Getter ChecksumType type=ChecksumType.MD5;
	private @Getter boolean inputIsFile;

	public enum ChecksumType {
		MD5,
		SHA,
		SHA256("SHA-256"),
		SHA512("SHA-512"),
		CRC32,
		ADLER32;

		private String algorithm;

		ChecksumType(String algorithm) {
			this.algorithm = algorithm;
		}
		ChecksumType() {
			this(null);
		}

		public String getAlgorithm() {
			return algorithm!=null ? algorithm : name();
		}
	}

	protected interface ChecksumGenerator {
		public void update(int b);
		public void update(byte[] b, int offset, int length);
		public String getResult();
	}

	protected ChecksumGenerator createChecksumGenerator() throws NoSuchAlgorithmException {
		switch(getType()) {
			case MD5:
			case SHA:
			case SHA256:
			case SHA512:
				return new MessageDigestChecksumGenerator(getType());
			case CRC32:
				return new ZipChecksumGenerator(new CRC32());
			case ADLER32:
				return new ZipChecksumGenerator(new Adler32());
			default:
				throw new NoSuchAlgorithmException("unsupported algorithm ["+getType()+"]");
		}
	}

	protected static class ZipChecksumGenerator implements ChecksumGenerator {

		private Checksum checksum;

		ZipChecksumGenerator(Checksum checksum) {
			super();
			this.checksum=checksum;
			checksum.reset();
		}

		@Override
		public void update(int b){
			checksum.update(b);
		}

		@Override
		public void update(byte[] b, int offset, int length){
			checksum.update(b,offset,length);
		}

		@Override
		public String getResult(){
			return Long.toHexString(checksum.getValue());
		}
	}

	protected static class MessageDigestChecksumGenerator implements ChecksumGenerator {

		private MessageDigest messageDigest;

		MessageDigestChecksumGenerator(ChecksumType type) throws NoSuchAlgorithmException {
			super();
			this.messageDigest=MessageDigest.getInstance(type.getAlgorithm());
		}

		@Override
		public void update(int b){
			messageDigest.update((byte)b);
		}

		@Override
		public void update(byte[] b, int offset, int length){
			messageDigest.update(b,offset,length);
		}

		@Override
		public String getResult(){
			return new BigInteger(1,messageDigest.digest()).toString(16);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			ChecksumGenerator cg=createChecksumGenerator();
			byte[] barr=new byte[1000];
			try (InputStream fis = isInputIsFile() ? new FileInputStream(message.asString()) : message.asInputStream(getCharset())){
				int c;
				while ((c=fis.read(barr))>=0) {
					cg.update(barr, 0, c);
				}
			}
			return new PipeRunResult(getSuccessForward(), cg.getResult());
		} catch (Exception e) {
			throw new PipeRunException(this,"cannot calculate ["+getType()+"]"+(isInputIsFile()?" on file ["+message+"]":" using charset ["+getCharset()+"]"),e);
		}
	}

	/**
	 * Character encoding to be used to encode message before calculating checksum.
	 */
	public void setCharset(String string) {
		charset = string;
	}

	/**
	 * Type of checksum to be calculated
	 * @ff.default MD5
	 */
	public void setType(ChecksumType value) {
		type = value;
	}

	/**
	 * If set <code>true</code>, the input is assumed to be a filename; otherwise the input itself is used in the calculations.
	 * @ff.default false
	 */
	@Deprecated
	@ConfigurationWarning("Please use fileSystemPipe to read the file first.")
	public void setInputIsFile(boolean b) {
		inputIsFile = b;
	}

}
