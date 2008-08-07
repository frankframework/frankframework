/*
 * $Log: ChecksumPipe.java,v $
 * Revision 1.1  2008-08-07 07:57:06  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

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
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setType(String) type}</td>  <td>type of checksum to be calculated. Must be one of MD5, SHA, CRC32, Adler32</td><td>MD5</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td>  <td>character encoding to be used to encode message before calculating checksum</td><td>UTF-8</td></tr>
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

	public String calculateCRC32(byte[] b) {
		CRC32 crc = new CRC32();
		crc.reset();
		crc.update(b);
		String result=Long.toHexString(crc.getValue());
		return result;
	}

	public String calculateAdler32(byte[] b) {
		Adler32 crc = new Adler32();
		crc.reset();
		crc.update(b);
		String result=Long.toHexString(crc.getValue());
		return result;
	}

	public String calculateMD5(byte[] b) throws NoSuchAlgorithmException {
		MessageDigest m=MessageDigest.getInstance("MD5");
		m.update(b);
        String result=new BigInteger(1,m.digest()).toString(16);
        return result;
	}

	public String calculateSHA(byte[] b) throws NoSuchAlgorithmException {
		MessageDigest m=MessageDigest.getInstance("SHA");
		m.update(b);
		String result=new BigInteger(1,m.digest()).toString(16);
		return result;
	}


	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String message=(String)input;
		String result;
		try {
			byte barr[];
			if (StringUtils.isEmpty(getCharset())) {
				barr=message.getBytes();
			} else {
				barr=message.getBytes(getCharset());
			}
			if (CHECKSUM_MD5.equals(getType())) {
				result = calculateMD5(barr);
			} else if (CHECKSUM_CRC32.equals(getType())) {
				result = calculateCRC32(barr);
			} else if (CHECKSUM_ADLER32.equals(getType())) {
			result = calculateAdler32(barr);
			} else if (CHECKSUM_SHA.equals(getType())) {
				result = calculateSHA(barr);
			} else {
				result = "unsupported algorithm ["+getType()+"]";
			}
			return new PipeRunResult(getForward(),result);
		} catch (Exception e) {
			throw new PipeRunException(this,"cannot calculate ["+getType()+"] using charset ["+getCharset()+"]",e);
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

}
