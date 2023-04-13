/*
   Copyright 2015 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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

import javax.jms.Message;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
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
 * <tr><td>{@link #setMessageProtocol(MessageProtocol) messageProtocol}</td><td>protocol of ESB service to be called. Possible values
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>"FF"</td></tr>
 * </table></p>
 *
 * @author Peter Leeuwenburgh
 */
public class FxfListener extends EsbJmsListener {
	private @Getter String fxfFileSessionKey = "fxfFile";
	private @Getter boolean moveProcessedFile = true;
	private @Getter String processedSiblingDirectory = "processed";
	private @Getter boolean createProcessedDirectory = false;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getJmsRealmName())) {
			setJmsRealm("qcf_tibco_p2p_ff");
		}
		if (getMessageProtocol()==null) {
			setMessageProtocol(MessageProtocol.FF);
		}
		if (StringUtils.isEmpty(getDestinationName())) {
			setDestinationName("jms/FileTransferAction");
		}
		super.configure();
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<Message> rawMessage, Map<String,Object> threadContext) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessage, threadContext);

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
		Receiver receiver = getReceiver();
		if (receiver != null) {
			IAdapter iAdapter = receiver.getAdapter();
			if (iAdapter != null) {
				iAdapter.getMessageKeeper().add("WARNING: " + msg + (t != null ? ": " + t.getMessage() : ""), MessageKeeperLevel.WARN);
			}
		}
	}

	/**
	 * name of the session key to store the name of the received file in
	 * @ff.default fxfFile
	 */
	public void setFxfFileSessionKey(String fxfFileSessionKey) {
		this.fxfFileSessionKey = fxfFileSessionKey;
	}

	/**
	 * If set to <code>true</code>, the received file is moved after being processed
	 * @ff.default true
	 */
	public void setMoveProcessedFile(boolean b) {
		moveProcessedFile = b;
	}

	/**
	 * (only used when <code>moveProcessedFile=true</code>) <b>sibling</b> directory (related to the parent directory of the file to process) where files are stored after being processed
	 * @ff.default processed
	 */
	public void setProcessedSiblingDirectory(String processedSiblingDirectory) {
		this.processedSiblingDirectory = processedSiblingDirectory;
	}

	/**
	 * (only used when <code>moveProcessedFile=true</code>) when set to <code>true</code>, the directory to move processed files in is created if it does not exist
	 * @ff.default false
	 */
	public void setCreateProcessedDirectory(boolean b) {
		createProcessedDirectory = b;
	}
}
