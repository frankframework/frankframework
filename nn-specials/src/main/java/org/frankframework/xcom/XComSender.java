/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.xcom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Mandatory;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.FileUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;

/**
 * XCom client voor het versturen van files via XCom.
 *
 * @author John Dekker
 */
public class XComSender extends AbstractSenderWithParameters {

	private File workingDir;
	private @Getter FileOptionType fileOption = null;
	private @Getter Boolean queue = null;
	private @Getter Boolean truncation = null;
	private @Getter Integer tracelevel = null;
	private @Getter String logfile = null;
	private @Getter CodeType codeflag = null;
	private @Getter CarriageFlagType carriageflag = null;
	private @Getter String port = null;
	private @Getter String authAlias = null;
	private @Getter String userid = null;
	private String password = null;
	private @Getter CompressType compress = null;
	private @Getter String remoteSystem = null;
	private @Getter String remoteDirectory = null;
	private @Getter String remoteFilePattern = null;
	private @Getter String configFile = null;
	private @Getter String workingDirName = ".";
	private @Getter String xcomtcp = "xcomtcp";


	public enum FileOptionType {
		CREATE,
		APPEND,
		REPLACE
	}

	public enum CompressType {
		YES,
		COMPACT,
		LZLARGE,
		LZMEDIUM,
		LZSMALL,
		RLE,
		NO
	}

	public enum CodeType {
		EBCDIC,
		ASCII
	}

	public enum CarriageFlagType {
		YES,
		VLR,
		VLR2,
		MPACK,
		XPACK,
		NO
	}

	@Override
	public void configure() throws ConfigurationException {
		if (! StringUtils.isEmpty(port)) {
			try {
				Integer.parseInt(port);
			} catch(NumberFormatException e) {
				throw new ConfigurationException("Attribute [port] is not a number", e);
			}
		}
		if (tracelevel != null && (tracelevel < 0 || tracelevel > 10)) {
			throw new ConfigurationException("Attribute [tracelevel] should be between 0 (no trace) and 10, not " + tracelevel);
		}
		if (StringUtils.isEmpty(workingDirName)) {
			throw new ConfigurationException("Attribute [workingDirName] is not set");
		}
		workingDir = new File(workingDirName);
		if (! workingDir.isDirectory()) {
			throw new ConfigurationException("Working directory [workingDirName=" + workingDirName + "] is not a directory");
		}
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		String messageString;
		try {
			messageString = message.asString();
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
		for (String filename : getFileList(messageString)) {
			log.debug("Start sending {}", filename);

			// get file to send
			File localFile = new File(filename);

			// execute command in a new operating process
			try {
				String cmd = getCommand(session, localFile, true);

				Process p = Runtime.getRuntime().exec(cmd, null, workingDir);

				// read the output of the process
				StringBuilder output = new StringBuilder();
				try (BufferedReader br = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(p.getInputStream()))) {
					String line;
					while ((line = br.readLine()) != null) {
						output.append(line);
					}
				} catch (IOException e) {
					// Make sure process is cleaned up
					p.destroy();
					// Rethrow for further cleanup
					throw e;
				}

				// wait until the process is completely finished
				try {
					p.waitFor();
				} catch (InterruptedException e) {
					log.warn("has been interrupted", e);
				}

				log.debug("output for {} = {}", localFile::getName, output::toString);
				log.debug("{} exits with {}", localFile::getName, p::exitValue);

				// throw an exception if the command returns an error exit value
				if (p.exitValue() != 0) {
					throw new SenderException("XComSender failed for file " + localFile.getAbsolutePath() + "\r\n" + output);
				}
			} catch (IOException e) {
				throw new SenderException("Error while executing command " + getCommand(session, localFile, false), e);
			}
		}
		return new SenderResult(message);
	}

	private String getCommand(PipeLineSession session, File localFile, boolean inclPasswd) throws SenderException {
		try {
			StringBuilder sb = new StringBuilder();

			sb.append(xcomtcp). append(" -c1");

			if (StringUtils.isNotEmpty(configFile)) {
				sb.append(" -f ").append(configFile);
			}

			if (StringUtils.isNotEmpty(remoteSystem)) {
				sb.append(" REMOTE_SYSTEM=").append(remoteSystem);
			}

			if (localFile != null) {
				sb.append(" LOCAL_FILE=").append(localFile.getAbsolutePath());

				sb.append(" REMOTE_FILE=");
				if (! StringUtils.isEmpty(remoteDirectory))
					sb.append(remoteDirectory);
				if (StringUtils.isEmpty(remoteFilePattern))
					sb.append(localFile.getName());
				else
					sb.append(FileUtils.getFilename(paramList, session, localFile, remoteFilePattern));
			}

			CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUserid(), password);


			// optional parameters
			if (fileOption!=null)
				sb.append(" FILE_OPTION=").append(fileOption.name());
			if (queue != null)
				sb.append(" QUEUE=").append(queue ? "YES" : "NO");
			if (tracelevel != null)
				sb.append(" TRACE=").append(tracelevel.intValue());
			if (truncation != null)
				sb.append(" TRUNCATION=").append(truncation ? "YES" : "NO");
			if (! StringUtils.isEmpty(port))
				sb.append(" PORT=").append(port);
			if (! StringUtils.isEmpty(logfile))
				sb.append(" XLOGFILE=").append(logfile);
			if (compress!=null)
				sb.append(" COMPRESS=").append(compress.name());
			if (codeflag!=null)
				sb.append(" CODE_FLAG=").append(codeflag.name());
			if (carriageflag!=null)
				sb.append(" CARRIAGE_FLAG=").append(carriageflag.name());
			if (! StringUtils.isEmpty(cf.getUsername()))
				sb.append(" USERID=").append(cf.getUsername());
			if (inclPasswd && ! StringUtils.isEmpty(cf.getPassword()))
				sb.append(" PASSWORD=").append(cf.getPassword());

			return sb.toString();
		}
		catch(ParameterException e) {
			throw new SenderException(e);
		}
	}

	private List<String> getFileList(String message) {
		return StringUtil.split(message, ";");
	}

	/** one of create, append or replace */
	public void setFileOption(FileOptionType newVal) {
		fileOption = newVal;
	}

	/** remote directory is prefixed witht the remote file */
	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}


	public void setCarriageflag(CarriageFlagType value) {
		carriageflag = value;
	}

	/** characterset conversion */
	public void setCodeflag(CodeType value) {
		codeflag = value;
	}

	public void setCompress(CompressType value) {
		compress = value;
	}

	/** name of logfile for xcomtcp to be used */
	public void setLogfile(String string) {
		logfile = string;
	}

	/** password of user on remote system */
	public void setPassword(String string) {
		password = string;
	}

	/** port of remote host */
	public void setPort(String string) {
		port = string;
	}

	/** set queue off or on */
	public void setQueue(Boolean b) {
		queue = b;
	}

	/** hostname or tcpip adres of remote host */
	public void setRemoteSystem(String string) {
		remoteSystem = string;
	}

	/** set between 0 (no trace) and 10 */
	public void setTracelevel(Integer i) {
		tracelevel = i;
	}

	/** set truncation off or on */
	public void setTruncation(Boolean b) {
		truncation = b;
	}

	/** loginname of user on remote system */
	public void setUserid(String string) {
		userid = string;
	}

	/** remote file to create. if empty, the name is equal to the local file */
	public void setRemoteFilePattern(String string) {
		remoteFilePattern = string;
	}

	/** directory in which to run the xcomtcp command */
	@Mandatory
	public void setWorkingDirName(String string) {
		workingDirName = string;
	}

	/** path to xcomtcp command */
	public void setXcomtcp(String string) {
		xcomtcp = string;
	}

	public void setConfigFile(String string) {
		configFile = string;
	}

	/** name of the alias to obtain credentials to authenticatie on remote server */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

}
