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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

/**
 * Pipe to calculate checksum on input.
 *
 * 
 * @author  Gerrit van Brakel
 * @since   4.9  
 */
public class ChecksumPipe extends FixedForwardPipe {
	
	
	private @Getter String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter ChecksumType type=ChecksumType.MD5;
	private @Getter boolean inputIsFile=false;
	
	public enum ChecksumType {
		MD5,
		SHA,
		CRC32,
		ADLER32
	}

	protected interface ChecksumGenerator {
		public void update(byte b[], int length);
		public String getResult();
	}

	protected ChecksumGenerator createChecksumGenerator() throws NoSuchAlgorithmException {
		switch(getType()) {
			case MD5:
			case SHA:
				return new MessageDigestChecksumGenerator(getType());
			case CRC32:
				return new ZipChecksumGenerator(new CRC32());
			case ADLER32:
				return new ZipChecksumGenerator(new Adler32());
			default:
				throw new NoSuchAlgorithmException("unsupported algorithm ["+getType()+"]");
		}
	}

	protected class ZipChecksumGenerator implements ChecksumGenerator {	
		
		private Checksum checksum;

		ZipChecksumGenerator(Checksum checksum) {
			super();
			this.checksum=checksum;
			checksum.reset();
		}

		@Override
		public void update(byte b[],int length){
			checksum.update(b,0,length);
		}

		@Override
		public String getResult(){
			String result=Long.toHexString(checksum.getValue());
			return result;
		}
	}

	protected class MessageDigestChecksumGenerator implements ChecksumGenerator {	
		
		private MessageDigest messageDigest;

		MessageDigestChecksumGenerator(ChecksumType type) throws NoSuchAlgorithmException {
			super();
			this.messageDigest=MessageDigest.getInstance(type.name());
		}

		@Override
		public void update(byte b[],int length){
			messageDigest.update(b,0,length);
		}
		@Override
		public String getResult(){
			String result=new BigInteger(1,messageDigest.digest()).toString(16);
			return result;
		}
	}


	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;
		try {
			ChecksumGenerator cg=createChecksumGenerator();
			if (isInputIsFile()) {
				byte barr[]=new byte[1000];
				FileInputStream fis=new FileInputStream(message.asString());
				int c;
				while ((c=fis.read(barr))>=0) {
					cg.update(barr,c);
				}
			} else {
				byte barr[];
				if (StringUtils.isEmpty(getCharset())) {
					barr=message.asByteArray();
				} else {
					barr=message.asByteArray(getCharset());
				}
				cg.update(barr,barr.length);
			}
			result=cg.getResult();
			return new PipeRunResult(getSuccessForward(),result);
		} catch (Exception e) {
			throw new PipeRunException(this,"cannot calculate ["+getType()+"]"+(isInputIsFile()?" on file ["+message+"]":" using charset ["+getCharset()+"]"),e);
		}
	}


	/**
	 * Character encoding to be used to encode message before calculating checksum.
	 * @ff.default UTF-8
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
	public void setInputIsFile(boolean b) {
		inputIsFile = b;
	}

}
