/*
   Copyright 2013, 2018 Nationale-Nederlanden

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
import java.util.Arrays;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

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
 * <tr><td>{@link #setCharset(String) charset}</td>  <td>character encoding to be used to encode or decode message to or from string. (only used when convert2String=true)</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setLineLength(int) lineLength}</td>  <td> (only used when direction=encode) Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 4). If lineLength <= 0, then the output will not be divided into lines</td><td>auto</td></tr>
 * <tr><td>{@link #setLineSeparator(String) lineSeparator}</td>  <td> (only used when direction=encode) defines separator between lines. Special values: <code>auto</code>: platform default, <code>dos</code>: CRLF, <code>unix</code>: LF</td><td>auto</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either <code>string</code>, <code>bytes</code>, <code>stream</code> or <code>base64</code></td><td>"string"</td></tr>
 * </table>
 * </p>
 * 
 * @since   4.4
 * @author  Niels Meijer
 * @version 2.0
 */
public class Base64Pipe extends FixedForwardPipe {

	private String direction = "encode";
	private int lineLength = 76;
	private String lineSeparator = "auto";
	private String charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private String outputType = "string";
	private boolean convertToString = true;

	private List<String> outputTypes = Arrays.asList("string", "bytes", "stream");

	private byte lineSeparatorArray[];

	public void configure() throws ConfigurationException {
		super.configure();
		String dir=getDirection();
		if (dir==null) {
			throw new ConfigurationException(getLogPrefix(null)+"direction must be set");
		}

		if(outputTypes != null && !outputTypes.contains(outputType))
			throw new ConfigurationException("unknown outputType ["+outputType+"] supported attributes are "+outputTypes.toString()+"");

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

		if(!convertToString) {
			String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute convert2String is deprecated. Please specify an outputType instead";
			ConfigurationWarnings.getInstance().add(log, msg, true);

			//Allow this for backwards compatibility
			if(getDirection().equals("decode"))
				setOutputType("bytes");
		}
	}

	private void setLineSeparatorArray(String separator) {
		lineSeparatorArray = separator.getBytes();
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		InputStream binaryInputStream;

		if (input instanceof InputStream) {
			binaryInputStream = (InputStream)input;
		}
		else if (input instanceof byte[]) {
			binaryInputStream = new ByteArrayInputStream((byte[])input);
		}
		else { //Try parsing it as a String
			try {
				binaryInputStream = new ByteArrayInputStream(input.toString().getBytes(getCharset()));
			} catch (UnsupportedEncodingException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"cannot encode message using charset ["+getCharset()+"]",e);
			}
		}

		boolean direction = "encode".equals(getDirection());//TRUE encode - FALSE decode
		InputStream base64 = new Base64InputStream(binaryInputStream, direction, getLineLength(), lineSeparatorArray);
		Object result = null;

		if(getOutputType().equals("string")) {
			try {
				result = Misc.streamToString(base64, getCharset());
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"cannot convert base64 result to String using charset ["+getCharset()+"]", e);
			}
		}
		else if(getOutputType().equals("bytes")) {
			try {
				result = Misc.streamToBytes(base64);
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"cannot convert base64 result to Byte[]", e);
			}
		}
		else {
			result = base64;
		}

		return new PipeRunResult(getForward(), result);
	}

	public void setDirection(String string) {
		direction = string.toLowerCase();
	}

	public String getDirection() {
		return direction;
	}

	/**
	 * If true and decoding, result is returned as a string, otherwise as a byte array.
	 * If true and encoding, input is read as a string, otherwise as a byte array.
	 * @deprecated please use outputType instead
	 * @param b convert result to string or outputStream depending on the direction used
	 */
	public void setConvert2String(boolean b) {
		convertToString = b;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType.toLowerCase();
	}
	public String getOutputType() {
		return outputType;
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