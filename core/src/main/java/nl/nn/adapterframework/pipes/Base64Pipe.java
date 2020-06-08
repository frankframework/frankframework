/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.util.Misc;

/**
 * Pipe that performs base64 encoding and decoding.
 *
 *
 * @since   4.4
 * @author  Niels Meijer
 * @version 2.0
 */
public class Base64Pipe extends StreamingPipe {

	private String direction = "encode";
	private int lineLength = 76;
	private String lineSeparator = "auto";
	private String charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private String outputType = "string";
	private boolean convertToString = true;

	private List<String> outputTypes = Arrays.asList("string", "bytes", "stream");

	private byte lineSeparatorArray[];

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String dir=getDirection();
		if (!dir.equalsIgnoreCase("encode") && !dir.equalsIgnoreCase("decode"))
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for direction ["+dir+"], must be 'encode' or 'decode'");

		if(outputTypes != null && !outputTypes.contains(outputType))
			throw new ConfigurationException("unknown outputType ["+outputType+"] supported attributes are "+outputTypes.toString()+"");

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
			//Allow this for backwards compatibility
			if(getDirection().equals("decode"))
				setOutputType("bytes");
		}
	}

	private void setLineSeparatorArray(String separator) {
		lineSeparatorArray = separator.getBytes();
	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		InputStream binaryInputStream;
		try {
			binaryInputStream = message.asInputStream(getCharset());
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		boolean directionEncode = "encode".equals(getDirection());//TRUE encode - FALSE decode
		InputStream base64 = new Base64InputStream(binaryInputStream, directionEncode, getLineLength(), lineSeparatorArray);

		if(getOutputType().equals("stream")) {
			return new PipeRunResult(getForward(), base64);
		}
		
		try (MessageOutputStream target=getTargetStream(session)) {
			if (getOutputType().equals("string")) {
				try (Writer writer = target.asWriter()) {
					Misc.readerToWriter(new InputStreamReader(base64, getCharset()), writer);
				}
			} else {
				try (OutputStream out = target.asStream()) {
					Misc.streamToStream(base64, out);
				}
			}
			return target.getPipeRunResult();
		} catch (Exception e) {
			throw new PipeRunException(this, "cannot convert base64 "+getDirection()+" result", e);
		}
	}

	@SuppressWarnings("resource")
	@Override
	public MessageOutputStream provideOutputStream(IPipeLineSession session) throws StreamingException {
		MessageOutputStream target = getTargetStream(session);
		boolean directionEncode = "encode".equals(getDirection());//TRUE encode - FALSE decode
		OutputStream targetStream;
		if (getOutputType().equals("string")) {
			targetStream = new WriterOutputStream(target.asWriter(), getCharset());
		} else {
			targetStream = target.asStream();
		}
		OutputStream base64 = new Base64OutputStream(targetStream, directionEncode, getLineLength(), lineSeparatorArray);
		return new MessageOutputStream(this, base64, target);
	}

	
	@IbisDoc({"1", "either <code>encode</code> or <code>decode</code>", "encode"})
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
	@Deprecated
	@ConfigurationWarning("please specify outputType instead")
	public void setConvert2String(boolean b) {
		convertToString = b;
	}

	@IbisDoc({"2", "Either <code>auto</code>, <code>string</code>, <code>bytes</code>, <code>stream</code>", "auto"})
	public void setOutputType(String outputType) {
		this.outputType = outputType.toLowerCase();
	}
	public String getOutputType() {
		return outputType;
	}

	@IbisDoc({"3", "Character encoding to be used to encode or decode message to or from string. (only used when outputType=string or convert2string=true)", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"4", " (Only used when direction=encode) Defines separator between lines. special values: <code>auto</code>: platform default, <code>dos</code>: crlf, <code>unix</code>: lf", "auto"})
	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}
	public String getLineSeparator() {
		return lineSeparator;
	}

	@IbisDoc({"5", " (Only used when direction=encode) Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 4). if linelength &lt;= 0, then the output will not be divided into lines", "auto"})
	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}
	public int getLineLength() {
		return lineLength;
	}

}
