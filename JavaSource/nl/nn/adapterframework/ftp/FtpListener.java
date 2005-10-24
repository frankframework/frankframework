/*
 * $Log: FtpListener.java,v $
 * Revision 1.2  2005-10-24 11:41:27  europe\m00f531
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

import java.util.HashMap;
import java.util.LinkedList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

/**
 * File {@link nl.nn.adapterframework.core.IPullingListener listener} that looks in a directory for files according to a wildcard. 
 * When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.DirectoryListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(String) inputDirectory}</td><td>Directory to look for files</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseTime(long) responseTime}</td><td>Waittime to wait between polling</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHost(String) host}</td><td>name or ip adres of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPort(int) port}</td><td>portnumber of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(string) username}</td><td>name of the user to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(string) password}</td><td>name of the password to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyTransportType(int) type}</td><td>transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(string) host}</td><td>proxy host name</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) port}</td><td>proxy port</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUsername(string) username}</td><td>user name in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(string) password}</td><td>password in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFtpTypeDescription(String) ftpTypeDescription}</td><td>One of FTP, SFTP, FTPS(I) or FTPSI, FTPSX(SSL), FTPSX(TLS)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteFilenamePattern(string) filenamePattern}</td><td>remote directory in which files have to be uploaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(string) directory}</td><td>remote directory in which files have to be uploaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransferMode(string) mode}</td><td>transfermode, one of ASCII, BINARY, EBCDIC, STREAM, COMPRESSED</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>if true, the server certificate can be self signed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>if true, the server returns data via another socket</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @version Id
 * @author  John Dekker
 */
public class FtpListener implements IPullingListener, INamedObject {
	public static final String version = "$RCSfile: FtpListener.java,v $  $Revision: 1.2 $ $Date: 2005-10-24 11:41:27 $";

	protected Logger log = Logger.getLogger(this.getClass());
	private FtpSession ftpSession;
	private String name;
	private String remoteDirectory;
	private long responseTime = 3600000; // one hour
	private LinkedList remoteFilenames;

	public FtpListener() {
		this.ftpSession = new FtpSession();
	}
	

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
	}

	public void open() throws ListenerException {
	}

	public void close() throws ListenerException {
	}

	public HashMap openThread() throws ListenerException {
		return null;
	}

	public void closeThread(HashMap threadContext) throws ListenerException {
	}

	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
	 */
	public void configure() throws ConfigurationException {
		ftpSession.configure();
		remoteFilenames = new LinkedList();
	}

	/**
	 * Returns the name of the file in process (the {@link #archiveFile(File) archived} file) concatenated with the
	 * record number. As te {@link #archiveFile(File) archivedFile} method always renames to a 
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		String correlationId = rawMessage.toString();
		threadContext.put("cid", correlationId);
		return correlationId;
	}

	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	public synchronized Object getRawMessage(HashMap threadContext) throws ListenerException {
		if (remoteFilenames.isEmpty()) {
			try {
				ftpSession.openClient(remoteDirectory);
				remoteFilenames.addAll(ftpSession.ls(remoteDirectory, true, true));
			}
			catch(Exception e) {
				throw new ListenerException(e); 
			}
			finally {
				ftpSession.closeClient();
			}
		}
		if (! remoteFilenames.isEmpty()) {
			return remoteFilenames.removeFirst();
		}
		return waitAWhile();
	}
	
	private Object waitAWhile() throws ListenerException {
		try {
			Thread.sleep(responseTime);
			return null;
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
	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		return rawMessage.toString();
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public long getResponseTime() {
		return responseTime;
	}
	
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}

	public void setHost(String host) {
		ftpSession.setHost(host);
	}

	public void setPassword(String passwd) {
		ftpSession.setPassword(passwd);
	}

	public void setPort(int port) {
		ftpSession.setPort(port);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		ftpSession.setFtpTypeDescription(ftpTypeDescription);
	}

	public void setUsername(String username) {
		ftpSession.setUsername(username);
	}

	public void setProxyHost(String proxyHost) {
		ftpSession.setProxyHost(proxyHost);
	}

	public void setProxyPassword(String proxyPassword) {
		ftpSession.setProxyPassword(proxyPassword);
	}

	public void setProxyPort(int proxyPort) {
		ftpSession.setProxyPort(proxyPort);
	}

	public void setProxyTransportType(int proxyTransportType) {
		ftpSession.setProxyTransportType(proxyTransportType);
	}

	public void setProxyUsername(String proxyUsername) {
		ftpSession.setProxyUsername(proxyUsername);
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setCertificate(String certificate) {
		ftpSession.setCertificate(certificate);
	}

	public void setCertificatePassword(String certificatePassword) {
		ftpSession.setCertificatePassword(certificatePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		ftpSession.setJdk13Compatibility(jdk13Compatibility);
	}

	public void setKeystoreType(String keystoreType) {
		ftpSession.setKeystoreType(keystoreType);
	}

	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}

	public void setTruststorePassword(String truststorePassword) {
		ftpSession.setTruststorePassword(truststorePassword);
	}

	public void setTruststoreType(String truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
	}

	public void setVerifyHostname(boolean verifyHostname) {
		ftpSession.setVerifyHostname(verifyHostname);
	}

	public void setTransferMode(String transferMode) {
		ftpSession.setTransferMode(transferMode);
	}

	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}

	public void setProtP(boolean protP) {
		ftpSession.setProtP(protP);
	}
}
