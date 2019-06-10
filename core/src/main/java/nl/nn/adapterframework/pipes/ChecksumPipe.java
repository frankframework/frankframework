/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;


/** 
 * @author  Gerrit van Brakel
 * @since   4.9  
 */
@IbisDescription(
	"Pipe to calculate checksum on input." 
)
public class ChecksumPipe extends FixedForwardPipe {
	
	public static final String CHECKSUM_MD5="MD5";
	public static final String CHECKSUM_SHA="SHA";
	public static final String CHECKSUM_CRC32="CRC32";
	public static final String CHECKSUM_ADLER32="Adler32";
	
	private String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private String type=CHECKSUM_MD5;
	private boolean inputIsFile=false;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getType())) {
			throw new ConfigurationException(getLogPrefix(null)+"type must be specified");
		}
		if (!CHECKSUM_MD5.equals(getType()) && 
			!CHECKSUM_SHA.equals(getType()) && 
			!CHECKSUM_CRC32.equals(getType()) && 
			!CHECKSUM_ADLER32.equals(getType())) {
			throw new ConfigurationException(getLogPrefix(null)+"type ["+getType()+"] must be one of ["+
				CHECKSUM_MD5+","+
				CHECKSUM_SHA+","+
				CHECKSUM_CRC32+","+
				CHECKSUM_ADLER32+"]");
		}
	}

	protected interface ChecksumGenerator {
		public void update(byte b[], int length);
		public String getResult();
	}
	
	protected ChecksumGenerator createChecksumGenerator() throws NoSuchAlgorithmException {
		if (CHECKSUM_MD5.equals(getType())) {
			return new MessageDigestChecksumGenerator(getType());
		} else if (CHECKSUM_CRC32.equals(getType())) {
			return new ZipChecksumGenerator(new CRC32());
		} else if (CHECKSUM_ADLER32.equals(getType())) {
			return new ZipChecksumGenerator(new Adler32());
		} else if (CHECKSUM_SHA.equals(getType())) {
			return new MessageDigestChecksumGenerator(getType());
		} else {
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

		public void update(byte b[],int length){
			checksum.update(b,0,length);
		}

		public String getResult(){
			String result=Long.toHexString(checksum.getValue());
			return result;
		}
	}

	protected class MessageDigestChecksumGenerator implements ChecksumGenerator {	
		
		private MessageDigest messageDigest;

		MessageDigestChecksumGenerator(String type) throws NoSuchAlgorithmException {
			super();
			this.messageDigest=MessageDigest.getInstance(type);
		}

		public void update(byte b[],int length){
			messageDigest.update(b,0,length);
		}
		public String getResult(){
			String result=new BigInteger(1,messageDigest.digest()).toString(16);
			return result;
		}
	}


	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String message=(String)input;
		String result;
		try {
			ChecksumGenerator cg=createChecksumGenerator();
			if (isInputIsFile()) {
				byte barr[]=new byte[1000];
				FileInputStream fis=new FileInputStream((String)input);
				int c;
				while ((c=fis.read(barr))>=0) {
					cg.update(barr,c);
				}
			} else {
				byte barr[];
				if (StringUtils.isEmpty(getCharset())) {
					barr=message.getBytes();
				} else {
					barr=message.getBytes(getCharset());
				}
				cg.update(barr,barr.length);
			}
			result=cg.getResult();
			return new PipeRunResult(getForward(),result);
		} catch (Exception e) {
			throw new PipeRunException(this,"cannot calculate ["+getType()+"]"+(isInputIsFile()?" on file ["+input+"]":" using charset ["+getCharset()+"]"),e);
		}
	}


	@IbisDoc({"character encoding to be used to encode message before calculating checksum", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"type of checksum to be calculated. must be one of md5, sha, crc32, adler32", "md5"})
	public void setType(String string) {
		type = string;
	}
	public String getType() {
		return type;
	}

	@IbisDoc({"when set <code>true</code>, the input is assumed to be a filename; otherwise the input itself is used in the calculations", "false"})
	public void setInputIsFile(boolean b) {
		inputIsFile = b;
	}
	public boolean isInputIsFile() {
		return inputIsFile;
	}

}
