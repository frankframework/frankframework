/*
   Copyright 2013 Nationale-Nederlanden, 2020 - 2024 WeAreFrank!

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
package org.frankframework.compression;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IDataIterator;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.pipes.IteratingPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

/**
 * Sends a message to a Sender for each entry of its input, that must be an ZipInputStream. The input of the pipe must be one of:
 * <ul>
 * 	<li>String refering to a filename</li>
 *  <li>File</li>
 *  <li>InputStream</li>
 * </ul>
 * The message sent each time to the sender is the filename of the entry found in the archive.
 * The contents of the archive is available as a Stream or a String in a session variable.
 * <p>
 * <br/>
 *
 * @author Gerrit van Brakel
 * @since 4.9.10
 */
public class ZipIteratorPipe extends IteratingPipe<String> {

	private @Getter String contentsSessionKey = "zipdata";
	private @Getter boolean streamingContents = true;
	private @Getter boolean closeInputstreamOnExit = true;
	private @Getter String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getContentsSessionKey())) {
			throw new ConfigurationException("attribute contentsKey must be specified");
		}
	}

	protected ZipInputStream getZipInputStream(Message input) throws SenderException {
		if (input == null) {
			throw new SenderException("input is null. Must supply String (Filename, with processFile=true), File or InputStream as input");
		}

		InputStream source;

		try {
			source = input.asInputStream();
		} catch (IOException e) {
			throw new SenderException("cannot open stream", e);
		}

		if (!(source instanceof BufferedInputStream)) {
			source = new BufferedInputStream(source);
		}

		return new ZipInputStream(source);
	}

	@Override
	protected IDataIterator<String> getIterator(Message input, PipeLineSession session, Map<String, Object> threadContext) throws SenderException {
		ZipInputStream source = getZipInputStream(input);

		if (source == null) {
			throw new SenderException("no ZipInputStream found");
		}

		return new ZipStreamIterator(source, session);
	}

	/**
	 * Session key used to store contents of each zip entry
	 *
	 * @ff.default zipdata
	 */
	public void setContentsSessionKey(String string) {
		contentsSessionKey = string;
	}

	/**
	 * If set to <code>false</code>, a string containing the contents of the entry is placed under the session key, instead of the inputstream to the contents
	 *
	 * @ff.default true
	 */
	public void setStreamingContents(boolean b) {
		streamingContents = b;
	}

	/**
	 * If set to <code>false</code>, the inputstream is not closed after it has been used.
	 * Use with caution, they may create memory leaks.
	 *
	 * @ff.default true
	 */
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}

	/**
	 * Charset used when reading the contents of the entry (only used if streamingContents=false)
	 *
	 * @ff.default utf-8
	 */
	public void setCharset(String string) {
		charset = string;
	}

	private class ZipStreamIterator implements IDataIterator<String> {
		ZipInputStream source;
		PipeLineSession session;

		boolean nextRead = false;
		boolean currentOpen = false;
		ZipEntry current;

		ZipStreamIterator(ZipInputStream source, PipeLineSession session) {
			super();
			this.source = source;
			this.session = session;
		}

		private void skipCurrent() throws IOException {
			if (currentOpen) {
				currentOpen = false;
				nextRead = false;
				source.closeEntry();
			}
			if (!nextRead) {
				current = source.getNextEntry();
				nextRead = true;
			}
		}

		@Override
		public boolean hasNext() throws SenderException {
			log.debug("hasNext()");
			try {
				skipCurrent();
				return current != null;
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

		@Override
		public String next() throws SenderException {
			log.trace("next()");
			try {
				skipCurrent();
				currentOpen = true;
				log.debug("found zipEntry name [{}] compressed size [{}]", current::getName, current::getCompressedSize);
				String filename = current.getName();
				if (isStreamingContents()) {
					MessageContext context = new MessageContext().withName(current.getName());
					if (current.getTime() > 0L) {
						context.withModificationTime(current.getTime());
					}

					log.debug("storing stream to contents of zip entries under session key [{}]", ZipIteratorPipe.this::getContentsSessionKey);
					Message message = new Message(CloseUtils.dontClose(source), context);
					session.put(getContentsSessionKey(), message); // do this each time, to allow reuse of the session key when an item is optionally encoded
				} else {
					log.debug("storing contents of zip entry under session key [{}]", ZipIteratorPipe.this::getContentsSessionKey);
					String content = StreamUtil.streamToString(CloseUtils.dontClose(source), null, getCharset());
					session.put(getContentsSessionKey(), content);
				}
				return filename;
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

		@Override
		public void close() throws SenderException {
			try {
				if (isCloseInputstreamOnExit()) {
					source.close();
				}
				session.remove(getContentsSessionKey());
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}
	}
}
