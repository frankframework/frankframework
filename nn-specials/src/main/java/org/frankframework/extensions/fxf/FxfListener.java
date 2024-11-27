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
package org.frankframework.extensions.fxf;

import java.io.File;

import jakarta.jms.Message;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.esb.EsbJmsListener;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.FileUtils;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;

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
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<Message> rawMessageWrapper, PipeLineSession pipeLineSession) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessageWrapper, pipeLineSession);

		//TODO plr.getState() may return null when there is an error.
		// The message will be placed in the errorstore due to this,
		// when solving the NPE this no longer happens
		if (!isMoveProcessedFile() || !plr.isSuccessful()) {
			return;
		}
		File srcFile = null;
		File dstFile = null;
		try {
			String srcFileName = (String) pipeLineSession.get(getFxfFileSessionKey());
			if (StringUtils.isEmpty(srcFileName)) {
				warn("No file to move");
				return;
			}
			srcFile = new File(srcFileName);
			if (!srcFile.exists()) {
				warn("File [" + srcFileName + "] does not exist");
				return;
			}
			File srcDir = srcFile.getParentFile();
			String dstDirName = srcDir.getParent() + File.separator + getProcessedSiblingDirectory();
			dstFile = new File(dstDirName, srcFile.getName());
			dstFile = FileUtils.getFreeFile(dstFile);
			if (!dstFile.getParentFile().exists()) {
				if (isCreateProcessedDirectory()) {
					if (dstFile.getParentFile().mkdirs()) {
						log.debug("Created directory [{}]", dstFile.getParent());
					} else {
						log.warn("Directory [{}] could not be created", dstFile.getParent());
					}
				} else {
					log.warn("Directory [{}] does not exist", dstFile.getParent());
				}
			}
			if (FileUtils.moveFile(srcFile, dstFile, 1, 0) == null) {
				warn("Could not move file [" + srcFile.getAbsolutePath() + "] to file [" + dstFile.getAbsolutePath() + "]");
			} else {
				log.info("Moved file [{}] to file [{}]", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
			}
		} catch (Exception e) {
			String sourcePath = srcFile != null ? srcFile.getAbsolutePath() : "<unknown>";
			String destinationPath = dstFile != null ? dstFile.getAbsolutePath() : "<unknown>";
			warn("Error while moving file [" + sourcePath + "] to file [" + destinationPath + "]: " + e.getMessage(), e);
		}
	}

	private void warn(String msg) {
		warn(msg, null);
	}

	private void warn(String msg, Throwable t) {
		log.warn(msg, t);
		Receiver<Message> receiver = getReceiver();
		if (receiver != null) {
			Adapter adapter = receiver.getAdapter();
			if (adapter != null) {
				adapter.getMessageKeeper().add("WARNING: " + msg + (t != null ? ": " + t.getMessage() : ""), MessageKeeperLevel.WARN);
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
