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
package nl.nn.adapterframework.pipes;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.SupportsOutputStreaming;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;

/**
 * Pipe to calculate checksum on input.
 *
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@SupportsOutputStreaming
public class ChecksumPipe extends StreamingPipe {

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

	@Override
	protected boolean canProvideOutputStream() {
		return !isInputIsFile() && super.canProvideOutputStream();
	}

	@Override
	protected MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		ChecksumGenerator cg;
		try {
			cg = createChecksumGenerator();
		} catch (NoSuchAlgorithmException e) {
			throw new StreamingException("Cannot create ChecksumGenerator", e);
		}
		OutputStream targetStream = new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				cg.update(b);
			}

			@Override
			public void write(byte[] buf, int offset, int length) throws IOException {
				cg.update(buf, offset, length);
			}
		};
		return new MessageOutputStream(this, targetStream, getNextPipe(), getCharset()) {

			@Override
			public Message getResponse() {
				return new Message(cg.getResult());
			}

		};
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
