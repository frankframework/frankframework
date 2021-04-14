/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Pipe that performs base64 encoding and decoding.
 *
 *
 * @since   4.4
 * @author  Niels Meijer
 * @version 2.0
 */
public class Base64Pipe extends StreamingPipe {

	private Direction direction = Direction.ENCODE;
	private int lineLength = 76;
	private String lineSeparator = "auto";
	private String charset = null;
	private OutputTypes outputType = null;
	private Boolean convertToString = false;

	private byte lineSeparatorArray[];
	
	public enum Direction {
		ENCODE,
		DECODE;
	}
	public enum OutputTypes {
		STRING,
		BYTES;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		Direction dir=getDirectionEnum();
		if (dir==Direction.ENCODE) {
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
		
		if (dir==Direction.DECODE && convertToString) {
			setOutputType("string");
		}
		
		if (getOutputTypeEnum()==null) {
			setOutputType(dir==Direction.DECODE ? "bytes" : "string");
		}

	}

	private void setLineSeparatorArray(String separator) {
		lineSeparatorArray = separator.getBytes();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		boolean directionEncode = getDirectionEnum()==Direction.ENCODE;//TRUE encode - FALSE decode

		InputStream binaryInputStream;
		try {
			binaryInputStream = message.asInputStream(directionEncode ? getCharset() : null);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		InputStream base64 = new Base64InputStream(binaryInputStream, directionEncode, getLineLength(), lineSeparatorArray);

		Message result = new Message(base64, directionEncode ? StandardCharsets.US_ASCII.name() : getCharset());
		if (getOutputTypeEnum()==OutputTypes.STRING) {
			try {
				result = new Message(result.asReader());
			} catch (IOException e) {
				throw new PipeRunException(this,"cannot open stream", e);
			}
		}
		return new PipeRunResult(getForward(), result);
	}

	@SuppressWarnings("resource")
	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		MessageOutputStream target = getTargetStream(session);
		boolean directionEncode = getDirectionEnum()==Direction.ENCODE;//TRUE encode - FALSE decode
		OutputStream targetStream;
		String outputCharset = directionEncode ? StandardCharsets.US_ASCII.name() : getCharset();
		if (getOutputTypeEnum()==OutputTypes.STRING) {
			targetStream = new WriterOutputStream(target.asWriter(), outputCharset !=null? outputCharset : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		} else {
			targetStream = target.asStream(outputCharset);
		}
		OutputStream base64 = new Base64OutputStream(targetStream, directionEncode, getLineLength(), lineSeparatorArray);
		if (directionEncode && StringUtils.isNotEmpty(getCharset())) {
			try {
				return new MessageOutputStream(this, new OutputStreamWriter(base64, getCharset()), target);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException("cannot open OutputStreamWriter", e);
			}
		}
		return new MessageOutputStream(this, base64, target);
	}

	
	@IbisDoc({"1", "Either <code>encode</code> or <code>decode</code>", "encode"})
	public void setDirection(String direction) {
		this.direction = Misc.parse(Direction.class, direction);
	}
	public Direction getDirectionEnum() {
		return direction;
	}

	@Deprecated
	@ConfigurationWarning("please specify outputType instead")
	public void setConvert2String(boolean b) {
		convertToString = b;
	}

	@IbisDoc({"2", "Either <code>string</code> for character output or <code>bytes</code> for binary output. The value <code>stream</code> is no longer used. Streaming is automatic where possible", "string for direction=encode, bytes for direction=decode"})
	public void setOutputType(String outputType) {
		if (outputType.equalsIgnoreCase("Stream")) {
			ConfigurationWarnings.add(this, log, "outputType 'Stream' is no longer used. Streaming is automatic where possible");
		} else {
			this.outputType = Misc.parse(OutputTypes.class, outputType);
		}
	}
	public OutputTypes getOutputTypeEnum() {
		return outputType;
	}

	@IbisDoc({"3", "Character encoding to be used to when reading input from strings for direction=encode or writing data for direction=decode.", ""})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"4", " (Only used when direction=encode) Defines separator between lines. Special values: <code>auto</code>: platform default, <code>dos</code>: crlf, <code>unix</code>: lf", "auto"})
	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}
	public String getLineSeparator() {
		return lineSeparator;
	}

	@IbisDoc({"5", " (Only used when direction=encode) Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 4). If linelength &lt;= 0, then the output will not be divided into lines", "76"})
	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}
	public int getLineLength() {
		return lineLength;
	}
}
