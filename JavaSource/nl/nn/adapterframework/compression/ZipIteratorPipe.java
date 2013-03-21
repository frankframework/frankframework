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
/*
 * $Log: ZipIteratorPipe.java,v $
 * Revision 1.9  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.8  2011/12/08 13:01:59  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.7  2011/11/30 13:51:57  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2010/04/28 09:49:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * store stream to contents of zip entries each time, to allow 
 * reuse of the session key when an item is optionally encoded
 *
 * Revision 1.4  2010/04/01 11:57:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved configwarning
 *
 * Revision 1.3  2010/03/25 12:56:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed attribute closeStreamOnExit into closeInputstreamOnExit
 *
 * Revision 1.2  2010/02/25 13:41:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted javadoc for resultOnTimeOut attribute
 *
 * Revision 1.1  2010/01/06 17:57:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.IteratingPipe;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;

import org.apache.commons.lang.StringUtils;


/**
 * Sends a message to a Sender for each entry of its input, that must be an ZipInputStream. The input of the pipe must be one of:
 * <ul>
 * 	<li>String refering to a filename</li>
 *  <li>File</li>
 *  <li>InputStream</li> 
 * </ul>
 * The message sent each time to the sender is the filename of the entry found in the archive. 
 * The contents of the archive is available as a Stream or a String in a session variable. 
 * <br>
 * The output of each of the processing of each of the elements is returned in XML as follows:
 * <pre>
 *  &lt;results count="num_of_elements"&gt;
 *    &lt;result&gt;result of processing of first item&lt;/result&gt;
 *    &lt;result&gt;result of processing of second item&lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.StreamLineIteratorPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit (e.g. "receiver timed out").</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. 
 * 		Iteration stops if condition returns anything other than <code>false</code> or an empty result.
 * For example, to stop after the second child element has been processed, one of the following expressions could be used:
 * <table> 
 * <tr><td><li><code>result[position()='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>position()='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * <tr><td>{@link #setBlockSize(int) blockSize}</td><td>controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.</td><td>0 (one line at a time, no prefix of suffix)</td></tr>
 * <tr><td>{@link #setBlockPrefix(String) blockPrefix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the start of the set of lines.</td><td>&lt;block&gt;</td></tr>
 * <tr><td>{@link #setBlockSuffix(String) blockSuffix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the end of the set of lines.</td><td>&lt;/block&gt;</td></tr>
 * <tr><td>{@link #setContentsSessionKey(String) contentsSessionKey}</td><td>session key used to store contents of each zip entry</td><td>zipdata</td></tr>
 * <tr><td>{@link #setStreamingContents(boolean) streamingContents}</td><td>when set to <code>false</code>, a string containing the contents of the entry is placed under the session key, instead of the inputstream to the contents</td><td>true</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td><td>when set to <code>false</code>, the inputstream is not closed after it has been used</td><td>true</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>charset used when reading the contents of the entry (only used if streamingContens=false></td><td>UTF-8</td></tr>
 * </table>
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
 * @version $Id$
 */
public class ZipIteratorPipe extends IteratingPipe {

	private String contentsSessionKey="zipdata";
	private boolean streamingContents=true;
	private boolean closeInputstreamOnExit=true;
	private String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;

	public void configure() throws ConfigurationException {
		super.configure();
			if (StringUtils.isEmpty(getContentsSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"attribute contentsKey must be specified");
			}
	}
	
	private class ZipStreamIterator implements IDataIterator {
		
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

		public boolean hasNext() throws SenderException {
			if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"hasNext()");
			try {
				skipCurrent();
				return current!=null;
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

		public Object next() throws SenderException {
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
					session.put(getContentsSessionKey(),StreamUtil.streamToString(source,null,getCharset()));
				}
				return filename;
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

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
	
	protected ZipInputStream getZipInputStream(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		if (input==null) {
			throw new SenderException("input is null. Must supply String (Filename), File or InputStream as input");
		}
		InputStream source=null;
		if (input instanceof InputStream) {
			source=(InputStream)input;
		} else if (input instanceof File) {
			try {
				source= new FileInputStream((File)input);
			} catch (FileNotFoundException e) {
				throw new SenderException("Cannot find file ["+((File)input).getName()+"]",e);
			}
		} else if (input instanceof String) {
			String filename=(String)input;
			try {
				source=new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				throw new SenderException("Cannot find file ["+filename+"]",e);
			}
		} else {
			throw new SenderException("input is of type ["+ClassUtils.nameOf(input)+"]. Must supply String (Filename), File or InputStream as input");
		}
		if (!(source instanceof BufferedInputStream)) {
			source=new BufferedInputStream(source);
		}
		ZipInputStream zipstream=new ZipInputStream(source);
		return zipstream;
	}
	
	protected IDataIterator getIterator(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		ZipInputStream source=getZipInputStream(input, session, correlationID, threadContext);
		if (source==null) {
			throw new SenderException(getLogPrefix(session)+"no ZipInputStream found");
		}
		return new ZipStreamIterator(source,session);
	}




	public void setContentsSessionKey(String string) {
		contentsSessionKey = string;
	}
	public String getContentsSessionKey() {
		return contentsSessionKey;
	}

	public void setStreamingContents(boolean b) {
		streamingContents = b;
	}
	public boolean isStreamingContents() {
		return streamingContents;
	}

	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public void setCloseStreamOnExit(boolean b) {
		ConfigurationWarnings.getInstance().add(getLogPrefix(null)+"attribute 'closeStreamOnExit' has been renamed into 'closeInputstreamOnExit'");
		setCloseInputstreamOnExit(b);
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

}
