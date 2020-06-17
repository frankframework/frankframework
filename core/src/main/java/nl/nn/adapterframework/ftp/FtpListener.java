/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;

/**
 * Listener that polls a directory via FTP for files according to a wildcard. 
 * When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 *
 * @author  John Dekker
 */
public class FtpListener extends FtpSession implements IPullingListener<String>, INamedObject, RunStateEnquiring {

	private LinkedList<String> remoteFilenames;
	private RunStateEnquirer runStateEnquirer=null;

	private String name;
	private String remoteDirectory;
	private long responseTime = 3600000; // one hour

	private long localResponseTime =  1000; // time between checks if adapter still state 'started'

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String,Object> context) throws ListenerException {
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
	@Override
	public String getIdFromRawMessage(String rawMessage, Map<String, Object> threadContext) throws ListenerException {
		String correlationId = rawMessage.toString();
		PipeLineSessionBase.setListenerParameters(threadContext, correlationId, correlationId, null, null);
		return correlationId;
	}

	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	@Override
	public synchronized String getRawMessage(Map<String, Object> threadContext) throws ListenerException {
		log.debug("FtpListener [" + getName() + "] in getRawMessage, retrieving contents of directory [" +remoteDirectory+ "]");
		if (remoteFilenames.isEmpty()) {
			try {
				openClient(remoteDirectory);
				List<String> names = ls(remoteDirectory, true, true);
				log.debug("FtpListener [" + getName() + "] received ls result of ["+names.size()+"] files");
				if (names != null && names.size() > 0) {
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
			log.debug("FtpListener " + getName() + " returns " + result);
			return result;
		}
		waitAWhile();
		return null;
	}
	
	private void waitAWhile() throws ListenerException {
		try {
			log.debug("FtpListener " + getName() + " starts waiting ["+responseTime+"] ms in chunks of ["+localResponseTime+"] ms");
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
	public Message extractMessage(String rawMessage, Map<String, Object> threadContext) throws ListenerException {
		return new Message(rawMessage);
	}

	protected boolean canGoOn() {
		return runStateEnquirer!=null && runStateEnquirer.isInState(RunStateEnum.STARTED);
	}

	@Override
	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}


	
	@Override
	@IbisDoc({"name of the listener", ""})
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}
	
	@IbisDoc({"time between pollings", "3600000 (one hour)"})
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	public long getResponseTime() {
		return responseTime;
	}

	@IbisDoc({"remote directory from which files have to be downloaded", ""})
	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}

}
