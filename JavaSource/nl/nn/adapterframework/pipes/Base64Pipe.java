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
 * @version $Id$
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
