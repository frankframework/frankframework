/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2021-2025 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;

/**
 * Pipe that performs base64 encoding and decoding.
 *
 * @since   4.4
 * @author  Niels Meijer
 * @version 2.0
 */
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class Base64Pipe extends FixedForwardPipe {

	private @Getter Direction direction = Direction.ENCODE;
	private @Getter String charset = null;
	private @Getter String lineSeparator = "auto";
	private @Getter int lineLength = 76;

	private byte[] lineSeparatorArray;

	public enum Direction {
		ENCODE,
		DECODE
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		Direction dir=getDirection();
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
	}

	private void setLineSeparatorArray(String separator) {
		lineSeparatorArray = separator.getBytes();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		boolean directionEncode = getDirection() == Direction.ENCODE;// TRUE encode - FALSE decode

		InputStream binaryInputStream;
		try {
			binaryInputStream = message.asInputStream(directionEncode ? getCharset() : null);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		InputStream base64 = new Base64InputStream(binaryInputStream, directionEncode, getLineLength(), lineSeparatorArray);

		Message result = new Message(base64, message.copyContext().withoutSize().withCharset(directionEncode ? StandardCharsets.US_ASCII.name() : getCharset()));
		if (directionEncode) {
			try {
				result = new Message(result.asReader(), result.copyContext());
			} catch (IOException e) {
				throw new PipeRunException(this,"cannot open stream", e);
			}
		}
		// As we wrap the input-stream, we should make sure it's not closed when the session is closed as that might close this stream before reading it.
		message.unscheduleFromCloseOnExitOf(session);
		result.closeOnCloseOf(session);
		return new PipeRunResult(getSuccessForward(), result);
	}

	/** @ff.default ENCODE */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	/** Character encoding to be used to when reading input from strings for direction=encode or writing data for direction=decode. */
	public void setCharset(String string) {
		charset = string;
	}

	/**
	 *  (Only used when direction=encode) Defines separator between lines. Special values: <code>auto</code>: platform default, <code>dos</code>: crlf, <code>unix</code>: lf
	 * @ff.default auto
	 */
	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	/**
	 *  (Only used when direction=encode) Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 4). If linelength &lt;= 0, then the output will not be divided into lines
	 * @ff.default 76
	 */
	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}
}
