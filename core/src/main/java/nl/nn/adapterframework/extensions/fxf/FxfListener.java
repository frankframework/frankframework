/*
   Copyright 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;

/**
 * FxF extension of EsbJmsListener.
 * 
 * <p><b>Configuration </b><i>(where deviating from EsbJmsListener)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>"jms/FileTransferAction"</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>"qcf_tibco_p2p_ff"</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of ESB service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>"FF"</td></tr>
 * <tr><td>{@link #setFxfFileSessionKey(String) fxfFileSessionKey}</td><td>name of the session key to store the name of the received file in</td><td>fxfFile</td></tr>
 * <tr><td>{@link #setMoveProcessedFile(boolean) moveProcessedFile}</td><td>when set to <code>true</code>, the received file if moved after being processed</td><td>true</td></tr>
 * <tr><td>{@link #setProcessedSiblingDirectory(String) processedSiblingDirectory}</td><td>(only used when <code>moveProcessedFile=true</code>) <b>sibling</b> directory (related to the parent directory of the file to process) where files are stored after being processed</td><td>"processed"</td></tr>
 * <tr><td>{@link #setCreateProcessedDirectory(boolean) createProcessedDirectory}</td><td>(only used when <code>moveProcessedFile=true</code>) when set to <code>true</code>, the directory to move processed files in is created if it does not exist</td><td>false</td></tr>
 * </table></p>
 * 
 * @author Peter Leeuwenburgh
 */
public class FxfListener extends EsbJmsListener {
	private String fxfFileSessionKey = "fxfFile";
	private boolean moveProcessedFile = true;
	private String processedSiblingDirectory = "processed";
	private boolean createProcessedDirectory = false;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getJmsRealmName())) {
			setJmsRealm("qcf_tibco_p2p_ff");
		}
		if (StringUtils.isEmpty(getMessageProtocol())) {
			setMessageProtocol("FF");
		}
		if (StringUtils.isEmpty(getDestinationName())) {
			setDestinationName("jms/FileTransferAction");
		}
		super.configure();
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, Object rawMessageOrWrapper, Map<String,Object> threadContext) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessageOrWrapper, threadContext);

		//TODO plr.getState() may return null when there is an error.
		// The message will be placed in the errorstore due to this, 
		// when solving the NPE this no longer happens
		if (isMoveProcessedFile() && plr.isSuccessful()) {
			File srcFile = null;
			File dstFile = null;
			try {
				String srcFileName = (String) threadContext.get(getFxfFileSessionKey());
				if (StringUtils.isEmpty(srcFileName)) {
					warn("No file to move");
				} else {
					srcFile = new File(srcFileName);
					if (!srcFile.exists()) {
						warn("File [" + srcFileName + "] does not exist");
					} else {
						File srcDir = srcFile.getParentFile();
						String dstDirName = srcDir.getParent() + File.separator + getProcessedSiblingDirectory();
						dstFile = new File(dstDirName, srcFile.getName());
						dstFile = FileUtils.getFreeFile(dstFile);
						if (!dstFile.getParentFile().exists()) {
							if (isCreateProcessedDirectory()) {
								if (dstFile.getParentFile().mkdirs()) {
									log.debug("Created directory [" + dstFile.getParent() + "]");
								} else {
									log.warn("Directory [" + dstFile.getParent() + "] could not be created");
								}
							} else {
								log.warn("Directory [" + dstFile.getParent() + "] does not exist");
							}
						}
						if (FileUtils.moveFile(srcFile, dstFile, 1, 0) == null) {
							warn("Could not move file [" + srcFile.getAbsolutePath() + "] to file [" + dstFile.getAbsolutePath() + "]");
						} else {
							log.info("Moved file [" + srcFile.getAbsolutePath() + "] to file [" + dstFile.getAbsolutePath() + "]");
						}
					}
				}
			} catch (Exception e) {
				warn("Error while moving file [" + srcFile.getAbsolutePath() + "] to file [" + dstFile.getAbsolutePath() + "]: " + e.getMessage());
			}
		}
	}

	private void warn(String msg) {
		warn(msg, null);
	}

	private void warn(String msg, Throwable t) {
		log.warn(msg, t);
		IReceiver iReceiver = getReceiver();
		if (iReceiver != null && iReceiver instanceof ReceiverBase) {
			ReceiverBase rb = (ReceiverBase) iReceiver;
			IAdapter iAdapter = rb.getAdapter();
			if (iAdapter != null) {
				iAdapter.getMessageKeeper().add("WARNING: " + msg + (t != null ? ": " + t.getMessage() : ""), MessageKeeperLevel.WARN);
			}
		}
	}

	public String getFxfFileSessionKey() {
		return fxfFileSessionKey;
	}

	public void setFxfFileSessionKey(String fxfFileSessionKey) {
		this.fxfFileSessionKey = fxfFileSessionKey;
	}

	public void setMoveProcessedFile(boolean b) {
		moveProcessedFile = b;
	}

	public boolean isMoveProcessedFile() {
		return moveProcessedFile;
	}

	public void setProcessedSiblingDirectory(String processedSiblingDirectory) {
		this.processedSiblingDirectory = processedSiblingDirectory;
	}

	public String getProcessedSiblingDirectory() {
		return processedSiblingDirectory;
	}

	public void setCreateProcessedDirectory(boolean b) {
		createProcessedDirectory = b;
	}

	public boolean isCreateProcessedDirectory() {
		return createProcessedDirectory;
	}
}
