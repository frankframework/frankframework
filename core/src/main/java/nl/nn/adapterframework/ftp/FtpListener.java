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
package nl.nn.adapterframework.ftp;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


/** 
 * @author  John Dekker
 */
@IbisDescription(
	"Listener that polls a directory via FTP for files according to a wildcard.  \n" + 
	"When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.   \n" + 
	"The name of the moved file is passed to the pipeline.   \n" 
)
public class FtpListener extends FtpSession implements IPullingListener, INamedObject, RunStateEnquiring {

	private LinkedList remoteFilenames;
	private RunStateEnquirer runStateEnquirer=null;

	private String name;
	private String remoteDirectory;
	private long responseTime = 3600000; // one hour

	private long localResponseTime =  1000; // time between checks if adapter still state 'started'
	

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
	}

	public void open() throws ListenerException {
	}

	public void close() throws ListenerException {
	}

	public Map openThread() throws ListenerException {
		return null;
	}

	public void closeThread(Map threadContext) throws ListenerException {
	}

	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		remoteFilenames = new LinkedList();
	}

	/**
	 * Returns the name of the file in process (the 'archiveFile(File)' archived} file) concatenated with the
	 * record number. As the 'archiveFile(File)' archivedFile method always renames to a
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String correlationId = rawMessage.toString();
		PipeLineSessionBase.setListenerParameters(threadContext, correlationId, correlationId, null, null);
		return correlationId;
	}

	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	public synchronized Object getRawMessage(Map threadContext) throws ListenerException {
		log.debug("FtpListener [" + getName() + "] in getRawMessage, retrieving contents of directory [" +remoteDirectory+ "]");
		if (remoteFilenames.isEmpty()) {
			try {
				openClient(remoteDirectory);
				List names = ls(remoteDirectory, true, true);
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
			Object result = remoteFilenames.removeFirst();
			log.debug("FtpListener " + getName() + " returns " + result.toString());
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
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		return rawMessage.toString();
	}

	protected boolean canGoOn() {
		return runStateEnquirer!=null && runStateEnquirer.isInState(RunStateEnum.STARTED);
	}

	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}


	
	@IbisDoc({"name of the listener", ""})
	public void setName(String name) {
		this.name = name;
	}
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
