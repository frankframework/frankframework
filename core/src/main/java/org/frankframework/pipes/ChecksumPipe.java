/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.hash.Algorithm;
import org.frankframework.stream.Message;

/**
 * This pipe can be used to generate a hash for the given message using an algorithm. With this, you can prove integrity of the message. If you
 * need to prove the authenticity of the message as well, please use the {@link HashPipe} which uses an algorithm and a secret to prove both
 * integrity and authenticity.
 * <p>
 * The hash is generated based on the bytes of the given input message or on the bytes read from the file path if @{code inputIsFile} is @{code true}
 * <p>
 * The supported algorithms are:
 * <ul>
 *     <li>CRC32</li>
 *     <li>Adler32</li>
 *     <li>MD5</li>
 *     <li>SHA</li>
 *     <li>SHA256</li>
 *     <li>SHA384</li>
 *     <li>SHA512</li>
 * </ul>
 *
 * @author Gerrit van Brakel
 * @since 4.9
 * @deprecated please use the {@link HashPipe}
 */
@Deprecated(forRemoval = true, since = "8.3.0")
@ConfigurationWarning("Use the HashPipe")
public class ChecksumPipe extends HashPipe {

	private @Getter boolean inputIsFile;

	@Override
	public void configure() throws ConfigurationException {
		// Set the defaults for this Pipe, these are different compared to the HashPipe
		if (getAlgorithm() == null) {
			setAlgorithm(Algorithm.MD5);
		}

		if (getHashEncoding() == null) {
			setHashEncoding(HashPipe.HashEncoding.Hex);
		}

		super.configure();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try (InputStream fis = isInputIsFile() ? new FileInputStream(message.asString()) : message.asInputStream(getCharset())) {

			return super.doPipe(new Message(fis, message.getContext()), session);
		} catch (IOException e) {
			throw new PipeRunException(this, "Error reading input" + (isInputIsFile() ? " file [" + message + "]" : " using charset [" + getCharset() + "]"), e);
		}
	}

	/**
	 * If set <code>true</code>, the input is assumed to be a filename; otherwise the input itself is used in the calculations.
	 *
	 * @ff.default false
	 */
	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("Please use fileSystemPipe to read the file first.")
	public void setInputIsFile(boolean b) {
		inputIsFile = b;
	}

	/**
	 * Type of checksum to be calculated
	 * @ff.default MD5
	 */
	@Deprecated(forRemoval = true, since = "8.3.0")
	@ConfigurationWarning("Please use setAlgorithm to set the algorithm")
	public void setType(Algorithm value) {
		setAlgorithm(value);
	}
}
