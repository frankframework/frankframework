/*
 * $Log: Base64Pipe.java,v $
 * Revision 1.10  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.9  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2010/09/21 14:54:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * lineLength and separator configurable
 *
 * Revision 1.6  2010/04/28 09:53:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
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
 * <tr><td>{@link #setLineLength(int) lineLength}</td>  <td> (only used when direction=encode) Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 4). If lineLength <= 0, then the output will not be divided into lines</td><td>auto</td></tr>
 * <tr><td>{@link #setLineSeparator(String) lineSeparator}</td>  <td> (only used when direction=encode) defines separator between lines. Special values: <code>auto</code>: platform default, <code>dos</code>: CRLF, <code>unix</code>: LF</td><td>auto</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel / Jaco de Groot (***@dynasol.nl)
 * @since   4.4
 * @version Id
 */
public class Base64Pipe extends FixedForwardPipe {

	private String direction="encode";
	private boolean convert2String=true;
	private int lineLength=76;
	private String lineSeparator="auto";
	private String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;

	private byte lineSeparatorArray[];
	
	public void configure() throws ConfigurationException {
		super.configure();
		String dir=getDirection();
		if (dir==null) {
			throw new ConfigurationException(getLogPrefix(null)+"direction must be set");
		}
		if (!dir.equalsIgnoreCase("encode") && !dir.equalsIgnoreCase("decode")) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for direction ["+dir+"], must be 'encode' or 'decode'");
		}
		if (dir.equalsIgnoreCase("encode")) {
			if (StringUtils.isEmpty(getLineSeparator())) {
				setLineSeparatorArray("");
			} else if (getLineSeparator().equalsIgnoreCase("auto")) {
				setLineSeparatorArray(System.getProperty ( "line.separator" ));
			} else if (getLineSeparator().equalsIgnoreCase("dos")) {
				setLineSeparatorArray("\r\n");
			} else if (getLineSeparator().equalsIgnoreCase("unix")) {
				setLineSeparatorArray("\n");
			} else {
				setLineSeparatorArray(getLineSeparator());
			}
		}
	}

	private void setLineSeparatorArray(String separator) {
		lineSeparatorArray=separator.getBytes();
	}
	
	public PipeRunResult doPipe(Object invoer, IPipeLineSession session) throws PipeRunException {
		Object result=null;
		if (invoer!=null) {
			if ("encode".equalsIgnoreCase(getDirection())) {
				InputStream binaryInputStream;
				if (convert2String) {
					if (StringUtils.isEmpty(getCharset())) {
						binaryInputStream = new ByteArrayInputStream(invoer.toString().getBytes());
					} else {
						try {
							binaryInputStream = new ByteArrayInputStream(invoer.toString().getBytes(getCharset()));
						} catch (UnsupportedEncodingException e) {
							throw new PipeRunException(this,"cannot encode message using charset ["+getCharset()+"]",e);
						}
					}
				} else if (invoer instanceof InputStream) {
					binaryInputStream = (InputStream)invoer;
				} else {
					binaryInputStream = new ByteArrayInputStream((byte[])invoer);
				}
				try {
					result=Misc.streamToString(new Base64InputStream(binaryInputStream,true,getLineLength(),lineSeparatorArray),null,false);
				} catch (IOException e) {
					throw new PipeRunException(this,"cannot encode message from inputstream",e);
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

	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}
	public String getLineSeparator() {
		return lineSeparator;
	}

	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}
	public int getLineLength() {
		return lineLength;
	}

}
