/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.compression;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.IteratingPipe;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;


/**
 * Sends a message to a Sender for each entry of its input, that must be an ZipInputStream. The input of the pipe must be one of:
 * <ul>
 * 	<li>String refering to a filename</li>
 *  <li>File</li>
 *  <li>InputStream</li> 
 * </ul>
 * The message sent each time to the sender is the filename of the entry found in the archive. 
 * The contents of the archive is available as a Stream or a String in a session variable. 
 *
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link nl.nn.adapterframework.core.ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * For more configuration options, see {@link MessageSendingPipe}.
 * <br>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipIteratorPipe extends IteratingPipe<String> {

	private String contentsSessionKey="zipdata";
	private boolean streamingContents=true;
	private boolean closeInputstreamOnExit=true;
	private String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private boolean skipBOM=false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
			if (StringUtils.isEmpty(getContentsSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"attribute contentsKey must be specified");
			}
	}
	
	private class ZipStreamIterator implements IDataIterator<String> {
		
		ZipInputStream source; 
		IPipeLineSession session;

		boolean nextRead=false;
		boolean currentOpen=false;
		ZipEntry current;
		
		ZipStreamIterator(ZipInputStream source, IPipeLineSession session) {
			super();
			this.source=source;
			this.session=session;
		}

		private void skipCurrent() throws IOException {
			if (currentOpen) {
				currentOpen=false;
				nextRead=false;
				source.closeEntry();
			}
			if (!nextRead) {
				current=source.getNextEntry();
				nextRead=true;
			}
		}

		@Override
		public boolean hasNext() throws SenderException {
			if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"hasNext()");
			try {
				skipCurrent();
				return current!=null;
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

		@Override
		public String next() throws SenderException {
			if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"next()");
			try {
				skipCurrent();
				currentOpen=true;
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix(session)+"found zipEntry name ["+current.getName()+"] size ["+current.getSize()+"] compressed size ["+current.getCompressedSize()+"]");
				}
				String filename=current.getName();
				if (isStreamingContents()) {
					if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"storing stream to contents of zip entries under session key ["+getContentsSessionKey()+"]");
					session.put(getContentsSessionKey(),source); // do this each time, to allow reuse of the session key when an item is optionally encoded
				} else { 
					if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"storing contents of zip entry under session key ["+getContentsSessionKey()+"]");
					String content;
					if (isSkipBOM()) {
						byte contentBytes[] = StreamUtil.streamToByteArray(source, true);
						content = Misc.byteArrayToString(contentBytes, null, false);
					} else {
						content = StreamUtil.streamToString(source,null,getCharset());
					}
					session.put(getContentsSessionKey(),content);
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
	
	protected ZipInputStream getZipInputStream(Message input, IPipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		if (input==null) {
			throw new SenderException("input is null. Must supply String (Filename), File or InputStream as input");
		}
		InputStream source=null;
		try {
			if (input.asObject() instanceof String) {
				String filename=(String)input.asObject();
				try {
					source=new FileInputStream(filename);
				} catch (FileNotFoundException e) {
					throw new SenderException("Cannot find file ["+filename+"]",e);
				}
			} else {
				source = input.asInputStream();
			}
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(session)+"cannot open stream", e);
		}
		if (!(source instanceof BufferedInputStream)) {
			source=new BufferedInputStream(source);
		}
		ZipInputStream zipstream=new ZipInputStream(source);
		return zipstream;
	}
	
	@Override
	protected IDataIterator<String> getIterator(Message input, IPipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		ZipInputStream source=getZipInputStream(input, session, threadContext);
		if (source==null) {
			throw new SenderException(getLogPrefix(session)+"no ZipInputStream found");
		}
		return new ZipStreamIterator(source,session);
	}




	@IbisDoc({"session key used to store contents of each zip entry", "zipdata"})
	public void setContentsSessionKey(String string) {
		contentsSessionKey = string;
	}
	public String getContentsSessionKey() {
		return contentsSessionKey;
	}

	@IbisDoc({"when set to <code>false</code>, a string containing the contents of the entry is placed under the session key, instead of the inputstream to the contents", "true"})
	public void setStreamingContents(boolean b) {
		streamingContents = b;
	}
	public boolean isStreamingContents() {
		return streamingContents;
	}

	@IbisDoc({"when set to <code>false</code>, the inputstream is not closed after it has been used", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	@Deprecated
	@ConfigurationWarning("attribute 'closeStreamOnExit' has been renamed to 'closeInputstreamOnExit'")
	public void setCloseStreamOnExit(boolean b) {
		setCloseInputstreamOnExit(b);
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	@IbisDoc({"charset used when reading the contents of the entry (only used if streamingcontens=false>", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"when set to <code>true</code>, a possible bytes order mark (bom) at the start of the file is skipped (only used for encoding uft-8)", "false"})
	public void setSkipBOM(boolean b) {
		skipBOM = b;
	}
	public boolean isSkipBOM() {
		return skipBOM;
	}
}
