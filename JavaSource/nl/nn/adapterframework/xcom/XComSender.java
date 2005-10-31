/*
 * $Log: XComSender.java,v $
 * Revision 1.7  2005-10-31 14:42:40  europe\L190409
 * updated javadoc
 *
 * Revision 1.6  2005/10/28 12:31:05  John Dekker <john.dekker@ibissource.org>
 * Corrected bug with password added twice to command
 *
 * Revision 1.5  2005/10/27 13:29:26  John Dekker <john.dekker@ibissource.org>
 * Add optional configFile property
 *
 * Revision 1.4  2005/10/27 07:58:57  John Dekker <john.dekker@ibissource.org>
 * Host is not longer a required property, since it could be set in a config file
 *
 * Revision 1.3  2005/10/24 09:59:24  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.2  2005/10/11 13:04:50  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.1  2005/10/11 13:04:24  John Dekker <john.dekker@ibissource.org>
 * Support for sending files via the XComSender
 *
 */
package nl.nn.adapterframework.xcom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * XCom client voor het versturen van files via XCom.

 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.XComSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWorkingDirName(String) name}</td><td>directory in which to run the xcomtcp command</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXcomtcp(String) cmd}</td><td>Path to xcomtcp command</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileOption(String) option}</td><td>One of CREATE, APPEND or REPLACE</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueue(boolean) queue}</td><td>Set queue off or on</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruncation(boolean) truncation}</td><td>Set truncation off or on</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTracelevel(int) tracelevel}</td><td>Set between 0 (no trace) and 10</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCodeflag(String) codeflag}</td><td>Characterset conversion, one of ASCII or EBCDIC</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCarriageflag(String) carriageflag}</td><td>One of YES, NO, VRL, VRL2, MPACK or XPACK</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCompress(String) carriageflag}</td><td>One of YES, NO, RLE, COMPACT, LZLARGE, LZMEDIUM or LZSMALL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLogfile(String) logfile}</td><td>Name of logfile for xcomtcp to be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteSystem(String) remoteSystem}</td><td>Hostname or tcpip adres of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPort(String) port}</td><td>Port of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDir(String) remoteDir}</td><td>Remote directory is prefixed witht the remote file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteFilePattern(String) remoteFile}</td><td>Remote file to create. If empty, the name is equal to the local file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserid(String) userid}</td><td>Loginname of user on remote system</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>Password of user on remote system</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *  
 * @author: John Dekker
 */
public class XComSender extends SenderWithParametersBase {
	public static final String version = "$RCSfile: XComSender.java,v $  $Revision: 1.7 $ $Date: 2005-10-31 14:42:40 $";
	protected Logger logger = Logger.getLogger(this.getClass());
	private File workingDir;
	private String name;
	private String fileOption = null;
	private Boolean queue = null;
	private Boolean truncation = null;
	private Integer tracelevel = null;
	private String logfile = null;
	private String codeflag = null;
	private String cariageflag = null;
	private String port = null;
	private String userid = null;
	private String password = null;
	private String compress = null;
	private String remoteSystem = null;
	private String remoteDirectory = null;
	private String remoteFilePattern = null;
	private String configFile = null;
	private String workingDirName = ".";
	private String xcomtcp = "xcomtcp";
	
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#configure()
	 */
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
		if (! StringUtils.isEmpty(cariageflag) &&
				! "YES".equals(cariageflag) && ! "VLR".equals(cariageflag) && 
				! "VLR2".equals(cariageflag) && ! "MPACK".equals(cariageflag) && 
				! "XPACK".equals(cariageflag) && ! "NO".equals(cariageflag)  
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

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#isSynchronous()
	 */
	public boolean isSynchronous() {
		return true;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#sendMessage(java.lang.String, java.lang.String)
	 */
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		for (Iterator filenameIt = getFileList(message).iterator(); filenameIt.hasNext(); ) {
			String filename = (String)filenameIt.next();
			logger.debug("Start sending " + filename);
		
			// get file to send
			File localFile = new File(filename);
			
			// execute command in a new operating process
			try {
				String cmd = getCommand(prc.getSession(), localFile, true);
				
				Process p = Runtime.getRuntime().exec(cmd, null, workingDir);
	
				// read the output of the process
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
				}
	
				logger.debug("output for " + localFile.getName() + " = " + output.toString());
				logger.debug(localFile.getName() + " exits with " + p.exitValue());
				
				// throw an exception if the command returns an error exit value
				if (p.exitValue() != 0) {
					throw new SenderException("XComSender failed for file " + localFile.getAbsolutePath() + "\r\n" + output.toString());
				}
			}
			catch(IOException e) {
				throw new SenderException("Error while executing command " + getCommand(prc.getSession(), localFile, false), e);
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
			if (! StringUtils.isEmpty(cariageflag)) 
				sb.append(" CARRIAGE_FLAG=").append(cariageflag);
			if (! StringUtils.isEmpty(userid)) 
				sb.append(" USERID=").append(userid);
			if (inclPasswd && ! StringUtils.isEmpty(password)) 
				sb.append(" PASSWORD=").append(password);
				
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

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.INamedObject#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.INamedObject#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public String getFileOption() {
		return fileOption;
	}

	public void setFileOption(String newVal) {
		fileOption = newVal;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}

	public String getCariageflag() {
		return cariageflag;
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

	public Boolean isQueue() {
		return queue;
	}

	public String getRemoteSystem() {
		return remoteSystem;
	}

	public Integer getTracelevel() {
		return tracelevel;
	}

	public Boolean isTruncation() {
		return truncation;
	}

	public String getUserid() {
		return userid;
	}

	public void setCariageflag(String string) {
		cariageflag = string;
	}

	public void setCodeflag(String string) {
		codeflag = string;
	}

	public void setCompress(String string) {
		compress = string;
	}

	public void setLogfile(String string) {
		logfile = string;
	}

	public void setPassword(String string) {
		password = string;
	}

	public void setPort(String string) {
		port = string;
	}

	public void setQueue(Boolean b) {
		queue = b;
	}

	public void setRemoteSystem(String string) {
		remoteSystem = string;
	}

	public void setTracelevel(Integer i) {
		tracelevel = i;
	}

	public void setTruncation(Boolean b) {
		truncation = b;
	}

	public void setUserid(String string) {
		userid = string;
	}

	public String getRemoteFilePattern() {
		return remoteFilePattern;
	}

	public void setRemoteFilePattern(String string) {
		remoteFilePattern = string;
	}
	public String getWorkingDirName() {
		return workingDirName;
	}

	public void setWorkingDirName(String string) {
		workingDirName = string;
	}

	public void setXcomtcp(String string) {
		xcomtcp = string;
	}

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String string) {
		configFile = string;
	}

}
