/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.xcom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * XCom client voor het versturen van files via XCom.

 *
 * @author John Dekker
 */
public class XComSender extends SenderWithParametersBase {

	private File workingDir;
	private String name;
	private String fileOption = null;
	private Boolean queue = null;
	private Boolean truncation = null;
	private Integer tracelevel = null;
	private String logfile = null;
	private String codeflag = null;
	private String carriageflag = null;
	private String port = null;
	private String authAlias = null;
	private String userid = null;
	private String password = null;
	private String compress = null;
	private String remoteSystem = null;
	private String remoteDirectory = null;
	private String remoteFilePattern = null;
	private String configFile = null;
	private String workingDirName = ".";
	private String xcomtcp = "xcomtcp";

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(fileOption) &&
				! "CREATE".equals(fileOption) && ! "APPEND".equals(fileOption) &&
				! "REPLACE".equals(fileOption)
		) {
			throw new ConfigurationException("Attribute [fileOption] has incorrect value " + fileOption + ", should be one of CREATE | APPEND or REPLACE");
		}
		if (! StringUtils.isEmpty(compress) &&
				! "YES".equals(compress) && ! "COMPACT".equals(compress) &&
				! "LZLARGE".equals(compress) && ! "LZMEDIUM".equals(compress) &&
				! "LZSMALL".equals(compress) && ! "RLE".equals(compress) &&
				! "NO".equals(compress)
		) {
			throw new ConfigurationException("Attribute [compress] has incorrect value " + compress + ", should be one of YES | NO | RLE | COMPACT | LZLARGE | LZMEDIUM | LZSMALL");
		}
		if (! StringUtils.isEmpty(codeflag) &&
				! "EBCDIC".equals(codeflag) && ! "ASCII".equals(codeflag)
		) {
			throw new ConfigurationException("Attribute [codeflag] has incorrect value " + fileOption + ", should be ASCII or EBCDIC");
		}
        if (! StringUtils.isEmpty(carriageflag) &&
				! "YES".equals(carriageflag) && ! "VLR".equals(carriageflag) &&
				! "VLR2".equals(carriageflag) && ! "MPACK".equals(carriageflag) &&
				! "XPACK".equals(carriageflag) && ! "NO".equals(carriageflag)
		) {
			throw new ConfigurationException("Attribute [cariageflag] has incorrect value " + compress + ", should be one of YES | NO | VRL | VRL2 | MPACK | XPACK");
		}
		if (! StringUtils.isEmpty(port)) {
			try {
				Integer.parseInt(port);
			}
			catch(NumberFormatException e) {
				throw new ConfigurationException("Attribute [port] is not a number");
			}
		}
		if (tracelevel != null && (tracelevel.intValue() < 0 || tracelevel.intValue() > 10)) {
			throw new ConfigurationException("Attribute [tracelevel] should be between 0 (no trace) and 10, not " + tracelevel.intValue());
		}
		if (StringUtils.isEmpty(workingDirName)) {
			throw new ConfigurationException("Attribute [workingDirName] is not set");
		}
		else {
			workingDir = new File(workingDirName);
			if (! workingDir.isDirectory()) {
				throw new ConfigurationException("Working directory [workingDirName=" + workingDirName + "] is not a directory");
			}
		}
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String messageString;
		try {
			messageString = message.asString();
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
		for (Iterator filenameIt = getFileList(messageString).iterator(); filenameIt.hasNext(); ) {
			String filename = (String)filenameIt.next();
			log.debug("Start sending " + filename);

			// get file to send
			File localFile = new File(filename);

			// execute command in a new operating process
			try {
				String cmd = getCommand(session, localFile, true);

				Process p = Runtime.getRuntime().exec(cmd, null, workingDir);

				// read the output of the process
				BufferedReader br = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(p.getInputStream()));
				StringBuffer output = new StringBuffer();
				String line = null;
				while ((line = br.readLine()) != null) {
					output.append(line);
				}

				// wait until the process is completely finished
				try {
					p.waitFor();
				}
				catch(InterruptedException e) {
					log.warn(getLogPrefix()+"has been interrupted");
				}

				log.debug("output for " + localFile.getName() + " = " + output.toString());
				log.debug(localFile.getName() + " exits with " + p.exitValue());

				// throw an exception if the command returns an error exit value
				if (p.exitValue() != 0) {
					throw new SenderException("XComSender failed for file " + localFile.getAbsolutePath() + "\r\n" + output.toString());
				}
			}
			catch(IOException e) {
				throw new SenderException("Error while executing command " + getCommand(session, localFile, false), e);
			}
		}
		return message;
	}

	private String getCommand(PipeLineSession session, File localFile, boolean inclPasswd) throws SenderException {
		try {
			StringBuffer sb = new StringBuffer();

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
			if (StringUtils.isNotEmpty(fileOption))
				sb.append(" FILE_OPTION=").append(fileOption);
			if (queue != null)
				sb.append(" QUEUE=").append(queue.booleanValue() ? "YES" : "NO");
			if (tracelevel != null)
				sb.append(" TRACE=").append(tracelevel.intValue());
			if (truncation != null)
				sb.append(" TRUNCATION=").append(truncation.booleanValue() ? "YES" : "NO");
			if (! StringUtils.isEmpty(port))
				sb.append(" PORT=" + port);
			if (! StringUtils.isEmpty(logfile))
				sb.append(" XLOGFILE=" + logfile);
			if (! StringUtils.isEmpty(compress))
				sb.append(" COMPRESS=").append(compress);
			if (! StringUtils.isEmpty(codeflag))
				sb.append(" CODE_FLAG=").append(codeflag);
			if (! StringUtils.isEmpty(carriageflag))
				sb.append(" CARRIAGE_FLAG=").append(carriageflag);
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

	public String getXcomtcp() {
		return xcomtcp;
	}

	private List getFileList(String message) {
		StringTokenizer st = new StringTokenizer(message, ";");
		LinkedList list = new LinkedList();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@IbisDoc({"name of the sender", ""})
	public void setName(String name) {
		this.name = name;
	}

	public String getFileOption() {
		return fileOption;
	}

	@IbisDoc({"one of create, append or replace", ""})
	public void setFileOption(String newVal) {
		fileOption = newVal;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	@IbisDoc({"remote directory is prefixed witht the remote file", ""})
	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}

	public String getCariageflag() {
		return carriageflag;
	}

	public String getCodeflag() {
		return codeflag;
	}

	public String getCompress() {
		return compress;
	}

	public String getLogfile() {
		return logfile;
	}

	public String getPort() {
		return port;
	}

	public Boolean getQueue() {
		return queue;
	}

	public String getRemoteSystem() {
		return remoteSystem;
	}

	public Integer getTracelevel() {
		return tracelevel;
	}

	public Boolean getTruncation() {
		return truncation;
	}

	public String getUserid() {
		return userid;
	}

	@IbisDoc({"one of yes, no, vrl, vrl2, mpack or xpack", ""})
	public void setCarriageflag(String string) {
		carriageflag = string;
	}

	@IbisDoc({"characterset conversion, one of ascii or ebcdic", ""})
	public void setCodeflag(String string) {
		codeflag = string;
	}

	@IbisDoc({"one of yes, no, rle, compact, lzlarge, lzmedium or lzsmall", ""})
	public void setCompress(String string) {
		compress = string;
	}

	@IbisDoc({"name of logfile for xcomtcp to be used", ""})
	public void setLogfile(String string) {
		logfile = string;
	}

	@IbisDoc({"password of user on remote system", ""})
	public void setPassword(String string) {
		password = string;
	}

	@IbisDoc({"port of remote host", ""})
	public void setPort(String string) {
		port = string;
	}

	@IbisDoc({"set queue off or on", ""})
	public void setQueue(Boolean b) {
		queue = b;
	}

	@IbisDoc({"hostname or tcpip adres of remote host", ""})
	public void setRemoteSystem(String string) {
		remoteSystem = string;
	}

	@IbisDoc({"set between 0 (no trace) and 10", ""})
	public void setTracelevel(Integer i) {
		tracelevel = i;
	}

	@IbisDoc({"set truncation off or on", ""})
	public void setTruncation(Boolean b) {
		truncation = b;
	}

	@IbisDoc({"loginname of user on remote system", ""})
	public void setUserid(String string) {
		userid = string;
	}

	public String getRemoteFilePattern() {
		return remoteFilePattern;
	}

	@IbisDoc({"remote file to create. if empty, the name is equal to the local file", ""})
	public void setRemoteFilePattern(String string) {
		remoteFilePattern = string;
	}
	public String getWorkingDirName() {
		return workingDirName;
	}

	@IbisDoc({"directory in which to run the xcomtcp command", ""})
	public void setWorkingDirName(String string) {
		workingDirName = string;
	}

	@IbisDoc({"path to xcomtcp command", ""})
	public void setXcomtcp(String string) {
		xcomtcp = string;
	}

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String string) {
		configFile = string;
	}

	@IbisDoc({"name of the alias to obtain credentials to authenticatie on remote server", ""})
	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}


}
