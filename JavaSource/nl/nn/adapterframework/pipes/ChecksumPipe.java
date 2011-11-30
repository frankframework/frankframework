/*
 * $Log: ChecksumPipe.java,v $
 * Revision 1.5  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2009/03/16 16:11:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * clarified error message
 *
 * Revision 1.2  2008/08/07 10:56:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute inputIsFile
 *
 * Revision 1.1  2008/08/07 07:57:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
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
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe to calculate checksum on input.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedForwardPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td>  <td>type of checksum to be calculated. Must be one of MD5, SHA, CRC32, Adler32</td><td>MD5</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td>  <td>character encoding to be used to encode message before calculating checksum</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setInputIsFile(boolean) inputIsFile}</td><td>when set <code>true</code>, the input is assumed to be a filename; otherwise the input itself is used in the calculations</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9  
 * @version Id
 */
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
		public void update(byte b[],int length);
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


	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
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


	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	public void setType(String string) {
		type = string;
	}
	public String getType() {
		return type;
	}

	public void setInputIsFile(boolean b) {
		inputIsFile = b;
	}
	public boolean isInputIsFile() {
		return inputIsFile;
	}

}
