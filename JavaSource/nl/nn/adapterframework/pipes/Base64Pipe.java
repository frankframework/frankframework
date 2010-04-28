/*
 * $Log: Base64Pipe.java,v $
 * Revision 1.6  2010-04-28 09:53:22  L190409
 * enabled use of InputStream as input. 
 * replaced Base64 codec by Apache Commons Codec 1.4
 *
 * Revision 1.5  2008/12/16 13:40:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added charset attribute
 *
 * Revision 1.4  2008/03/20 12:06:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2006/04/25 06:56:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute convert2String
 *
 * Revision 1.2  2005/10/13 11:44:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * switched encode and decode code
 *
 * Revision 1.1  2005/10/05 07:38:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of Base64Pipe
 *
 */
package nl.nn.adapterframework.pipes;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang.StringUtils;



/**
 * Pipe that performs base64 encoding and decoding.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>encode</code> or <code>decode</code></td><td>"encode"</td></tr>
 * <tr><td>{@link #setConvert2String(boolean) convert2String}</td><td>If <code>true</code> and decoding, result is returned as a string, otherwise as a byte array. If <code>true</code> and encoding, input is read as a string, otherwise as a byte array.</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td>  <td>character encoding to be used to encode or decode message to or from string. (only used when convert2String=true)</td><td>UTF-8</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel / Jaco de Groot (***@dynasol.nl)
 * @since   4.4
 * @version Id
 */
public class Base64Pipe extends FixedForwardPipe {
	public static final String version="$RCSfile: Base64Pipe.java,v $ $Revision: 1.6 $ $Date: 2010-04-28 09:53:22 $";

	private String direction="encode";
	private boolean convert2String=true;
	private String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;

	public void configure() throws ConfigurationException {
		super.configure();
		String dir=getDirection();
		if (dir==null) {
			throw new ConfigurationException(getLogPrefix(null)+"direction must be set");
		}
		if (!dir.equalsIgnoreCase("encode") && !dir.equalsIgnoreCase("decode")) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for direction ["+dir+"], must be 'encode' or 'decode'");
		}
	}

	public PipeRunResult doPipe(Object invoer, PipeLineSession session) throws PipeRunException {
		Object result=null;
		if (invoer!=null) {
			if ("encode".equalsIgnoreCase(getDirection())) {
				if (convert2String) {
					if (StringUtils.isEmpty(getCharset())) {
						result=Base64.encodeBase64String(invoer.toString().getBytes());
					} else {
						try {
							result=Base64.encodeBase64String(invoer.toString().getBytes(getCharset()));
						} catch (UnsupportedEncodingException e) {
							throw new PipeRunException(this,"cannot encode message using charset ["+getCharset()+"]",e);
						}
					}
				} else if (invoer instanceof InputStream) {
					try {
						result=Misc.streamToString(new Base64InputStream((InputStream)invoer,true),null,false);
					} catch (IOException e) {
						throw new PipeRunException(this,"cannot encode message from inputstream",e);
					}
				} else {
					result=Base64.encodeBase64String((byte[])invoer);
				}
			} else {
				String in;
				if (invoer instanceof InputStream) {
					try {
						in=Misc.streamToString((InputStream)invoer,null,false);
					} catch (IOException e) {
						throw new PipeRunException(this,"cannot read inputstream",e);
					}
				} else {
					in=invoer.toString();
				}
				try {
					byte[] data=Base64.decodeBase64(in);
					if (convert2String) {
						if (StringUtils.isEmpty(getCharset())) {
							result=new String(data);
						} else {
							result=new String(data,getCharset());
						}
					} else {
						result=data;
					}
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot decode base64, charset ["+getCharset()+"]", e);
				}
			}
		} else {
			log.debug(getLogPrefix(session)+"has null input, returning null");
		}
		return new PipeRunResult(getForward(), result);
	}

	public void setDirection(String string) {
		direction = string;
	}

	public String getDirection() {
		return direction;
	}

	public void setConvert2String(boolean b) {
		convert2String = b;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

}
