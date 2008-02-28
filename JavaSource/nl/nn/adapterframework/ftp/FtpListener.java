/*
 * $Log: FtpListener.java,v $
 * Revision 1.11  2008-02-28 16:20:57  europe\L190409
 * use PipeLineSession.setListenerParameters()
 *
 * Revision 1.10  2007/10/03 08:36:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.9  2006/01/19 12:15:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.8  2006/01/05 14:17:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2005/12/19 17:22:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typos in javadoc
 *
 * Revision 1.6  2005/12/19 16:46:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework, lots of changes
 *
 * Revision 1.5  2005/12/07 15:54:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved response to stopping of adapter
 *
 * Revision 1.3  2005/11/07 08:21:35  John Dekker <john.dekker@ibissource.org>
 * Enable sftp public/private key authentication
 *
 * Revision 1.2  2005/10/24 11:41:27  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.1  2005/10/24 09:59:19  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.ftp;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Listener that polls a directory via FTP for files according to a wildcard. 
 * When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.ftp.FtpListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(String) remoteDirectory}</td><td>remote directory from which files have to be downloaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseTime(long) responseTime}</td><td>time between pollings</td><td>3600000 (one hour)</td></tr>
 * 
 * <tr><td>{@link #setHost(String) host}</td><td>name or ip adres of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPort(int) port}</td><td>portnumber of remote host</td><td>22</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>name of the alias to obtain credentials to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>name of the user to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>name of the password to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>proxy host name</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>proxy port</td><td>1080</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>name of the alias to obtain credentials to authenticate on proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUsername(String) proxyUsername}</td><td>default user name in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>default password in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFtpTypeDescription(String) ftpTypeDescription}</td><td>One of FTP, SFTP, FTPS(I) or FTPSI, FTPSX(SSL), FTPSX(TLS)</td><td>FTP</td></tr>
 * <tr><td>{@link #setFileType(String) fileType}</td><td>File type, one of ASCII, BINARY</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageIsContent(boolean) messageIsContent}</td><td>if true, the contents of the message is send, otherwise it message contains the local filenames of the files to be send</td><td>false</td></tr>
 * <tr><td>{@link #setPassive(boolean) passive}</td><td>if true, passive FTP is used: before data is sent, a PASV command is issued, and the connection is set up by the server</td><td>true</td></tr>
 * <tr><td>{@link #setProxyTransportType(int) ProxyTransportType}</td><td>(SFTP) transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)</td><td>4</td></tr>
 * <tr><td>{@link #setPrefCSEncryption(String) prefCSEncryption}</td><td>(SFTP) Optional preferred encryption from client to server for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefSCEncryption(String) prefSCEncryption}</td><td>(SFTP) Optional preferred encryption from server to client for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyFilePath(String) privateKeyFilePath}</td><td>(SFTP) Path to private key file for SFTP authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyAuthAlias(String) privateKeyAuthAlias}</td><td>(SFTP) name of the alias to obtain credentials for Passphrase of private key file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyPassword(String) privateKeyPassword}</td><td>(SFTP) Passphrase of private key file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKnownHostsPath(String) knownHostsPath}</td><td>(SFTP) path to file with knownhosts</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConsoleKnownHostsVerifier(boolean) consoleKnownHostsVerifier}</td><td>(SFTP) &nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>(FTPS) resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateType(String) certificateType}</td><td>(FTPS) &nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>(FTPS) alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>(FTPS) &nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>(FTPS) resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>(FTPS) &nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>(FTPS) alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>(FTPS) &nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>(FTPS) enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>(FTPS) when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>(FTPS) if true, the server certificate can be self signed</td><td>false</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>(FTPS) if true, the server returns data via another socket</td><td>false</td></tr>
 * </table>
 * </p>
 *
 * @author  John Dekker
 * @version Id
 */
public class FtpListener extends FtpSession implements IPullingListener, INamedObject, RunStateEnquiring {
	public static final String version = "$RCSfile: FtpListener.java,v $  $Revision: 1.11 $ $Date: 2008-02-28 16:20:57 $";

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
	 * Returns the name of the file in process (the {@link #archiveFile(File) archived} file) concatenated with the
	 * record number. As te {@link #archiveFile(File) archivedFile} method always renames to a 
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String correlationId = rawMessage.toString();
		PipeLineSession.setListenerParameters(threadContext, correlationId, correlationId, null, null);
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


	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	public long getResponseTime() {
		return responseTime;
	}

	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}

}
