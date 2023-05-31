/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package nl.nn.adapterframework.ftp;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;

/**
 * Listener that polls a directory via FTP for files according to a wildcard.
 * When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.
 * The name of the moved file is passed to the pipeline.
 *
 *
 * @author  John Dekker
 */
@Deprecated
@ConfigurationWarning("Please replace with FtpFileSystemListener")
public class FtpListener extends FtpSession implements IPullingListener<String>, RunStateEnquiring {

	private LinkedList<String> remoteFilenames;
	private RunStateEnquirer runStateEnquirer=null;

	private String remoteDirectory;
	private long responseTime = 3600000; // one hour

	private long localResponseTime =  1000; // time between checks if adapter still state 'started'

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, Map<String,Object> context) throws ListenerException {
	}

	@Override
	public void open() throws ListenerException {
	}

	@Override
	public void close() throws ListenerException {
	}

	@Override
	public Map<String,Object> openThread() throws ListenerException {
		return null;
	}

	@Override
	public void closeThread(Map<String,Object> threadContext) throws ListenerException {
	}

	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		remoteFilenames = new LinkedList<>();
	}

	/**
	 * Returns the name of the file in process (the 'archiveFile(File)' archived} file) concatenated with the
	 * record number. As the 'archiveFile(File)' archivedFile method always renames to a
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs!
	 */
	public String getIdFromRawMessage(String rawMessage, Map<String, Object> threadContext) {
		// TODO: Check where used, this is ugly. Can perhaps be inlined?
		PipeLineSession.updateListenerParameters(threadContext, rawMessage, rawMessage, null, null);
		return rawMessage;
	}

	/**
     * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
     * is a new file to process and returns the first record.
     */
	@Override
	public synchronized RawMessageWrapper<String> getRawMessage(Map<String, Object> threadContext) throws ListenerException {
		log.debug("FtpListener [{}] in getRawMessage, retrieving contents of directory [{}]", getName(), remoteDirectory);
		if (remoteFilenames.isEmpty()) {
			try {
				openClient(remoteDirectory);
				List<String> names = ls(remoteDirectory, true, true);
				log.debug("FtpListener [{}] received ls result of [{}] files", this::getName, names::size);
				if (!names.isEmpty()) {
					remoteFilenames.addAll(names);
				}
			}
			catch(Exception e) {
				throw new ListenerException("Exception retrieving contents of directory [" +remoteDirectory+ "]", e);
			}
			finally {
				closeClient();
			}
		}
		if (! remoteFilenames.isEmpty()) {
			String result = remoteFilenames.removeFirst();
			log.debug("FtpListener [{}] returns {}", getName(), result);
			return wrapRawMessage(result, threadContext);
		}
		waitAWhile();
		return null;
	}

	private RawMessageWrapper<String> wrapRawMessage(String rawMessage, Map<String, Object> threadContext) {
		return new RawMessageWrapper<>(rawMessage, this.getIdFromRawMessage(rawMessage, threadContext), null);
	}

	private void waitAWhile() throws ListenerException {
		try {
			log.debug("FtpListener [{}] starts waiting [{}] ms in chunks of [{}] ms", getName(), responseTime, localResponseTime);
			long timeWaited;
			for (timeWaited=0; canGoOn() && timeWaited+localResponseTime<responseTime; timeWaited+=localResponseTime) {
				Thread.sleep(localResponseTime);
			}
			if (canGoOn() && responseTime-timeWaited>0) {
				Thread.sleep(responseTime-timeWaited);
			}
		}
		catch(InterruptedException e) {
			throw new ListenerException("Interrupted while listening", e);
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName());
		ts.append("remoteDirectory", remoteDirectory);
		result += ts.toString();
		return result;

	}
	/**
	 * Returns a string of the rawMessage
	 */
	@Override
	public Message extractMessage(RawMessageWrapper<String> rawMessage, Map<String, Object> threadContext) throws ListenerException {
		return new Message(rawMessage.getRawMessage());
	}

	protected boolean canGoOn() {
		return runStateEnquirer!=null && runStateEnquirer.getRunState()==RunState.STARTED;
	}

	@Override
	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}

	/**
	 * Time <i>in milliseconds</i> between each poll interval
	 * @ff.default 3600000
	 */
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	public long getResponseTime() {
		return responseTime;
	}

	/** remote directory from which files have to be downloaded */
	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}
}
