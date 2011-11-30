/*
 * $Log: FTPsClient.java,v $
 * Revision 1.12  2011-11-30 13:52:04  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.10  2011/06/27 15:39:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabled KeyboardInteractive login (experimental)
 * allow to set keyManagerAlgorithm and trustManagerAlgorithm
 *
 * Revision 1.9  2007/10/08 13:30:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.8  2007/05/11 09:39:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make class public
 *
 * Revision 1.7  2007/02/12 13:50:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.6  2006/01/19 10:34:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * overided isPositiveCompletion to avoid hanging ls (belastingdienst)
 *
 * Revision 1.5  2005/12/20 09:33:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove TYPE I from prot_p code
 *
 * Revision 1.4  2005/12/19 16:46:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework, lots of changes
 *
 * Revision 1.3  2005/12/07 15:47:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modifications to make protp working
 *
 * Revision 1.1  2005/10/11 13:03:31  John Dekker <john.dekker@ibissource.org>
 * Supports retrieving files (FtpFileRetrieverPipe) and sending files (FtpSender)
 * via one of the FTP protocols (ftp, sftp, ftps both implicit as explicit).
 *
 */
package nl.nn.adapterframework.ftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactoryBase;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

/**
 * Apaches FTPCLient doesn't support FTPS; This class does support
 * implicit and explicit FTPS.
 * 
 * @author John Dekker
 */
public class FTPsClient extends FTPClient {
	protected Logger log = LogUtil.getLogger(this);
	
	private FtpSession session;
	private AuthSSLProtocolSocketFactoryBase socketFactory;
	private Socket orgSocket = null;
	
	FTPsClient(FtpSession session) throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		this.session = session;
		
		if (this.session.getFtpType() != FtpSession.FTP) {
			// obtain the socket factory, but do not do anything with it
			socketFactory = createSocketFactory();
		}
		
		// if implicit ftps, use SSL from the beginning
		if (this.session.getFtpType() == FtpSession.FTPS_IMPLICIT) {
			// instruct the FTPClient to use this SSLSocketFactory
			socketFactory.initSSLContext();
			setSocketFactory(socketFactory);
		}
	}
	
	protected void checkReply(String cmd) throws IOException  {
		if (!FTPReply.isPositiveCompletion(getReplyCode())) {
			throw new IOException("Command [" + cmd + "] returned error [" + getReplyString() + "]");
		} 
		log.debug("Command [" + cmd + "] returned " + getReplyString());
	}

	// FTPsClient did hang when positive completion was send without 
	// preliminary positive. Therefore completePendingCommand is 
	// overriden. 2006-01-18 GvB
	public boolean completePendingCommand() throws IOException
	{
		if (FTPReply.isPositiveCompletion(getReplyCode())) {
			return true;
		}
		return super.completePendingCommand();
	}

		
	
	protected void _connectAction_() throws IOException {
		// if explicit FTPS, the socket connection is establisch unsecure
		if (session.getFtpType() == FtpSession.FTPS_EXPLICIT_SSL ||
		    session.getFtpType() == FtpSession.FTPS_EXPLICIT_TLS) {
			orgSocket = _socket_; // remember the normal socket 

			// set the properties to aan appropriate default
			_socket_.setSoTimeout(10000);
			_socket_.setKeepAlive(true);
			
			// now send the command to inform the server to transform the connection 
			// into a secure connection
			log.debug(_readReply(orgSocket.getInputStream(), true));
			String protocol = getProtocol();
			log.debug(_sendCommand("AUTH " + protocol, orgSocket.getOutputStream(), orgSocket.getInputStream()));
			
			// replace the normal socket with the secure one 
			try {
				socketFactory.initSSLContext();
				_socket_ = socketFactory.createSocket(orgSocket, orgSocket.getInetAddress().getHostAddress(), orgSocket.getPort(), true);

				// send a dummy command over the secure connection without reading 
				// the reply
				// this allows us to call super._connectAction_()
				_sendCommand("FEAT", _socket_.getOutputStream(), null);
				super._connectAction_();
			}
			catch(IOException e) {
				throw e;
			}
			catch(Exception e) {
				log.error(e);
				throw new IOException("Unexpected error");
			}
		}
		else {
			super._connectAction_();
		}
	}
	
	protected Socket _openDataConnection_(int cmdNr, String param) throws IOException {
		// if explicit FTPS, the socket connection is establisch unsecure
		if (session.getFtpType() == FtpSession.FTPS_EXPLICIT_SSL || session.getFtpType() == FtpSession.FTPS_EXPLICIT_TLS) {
			if (session.isProtp()) {
				// With Prot P the result is returned over a different port
				// .. send protp commands  
 
 				sendCommand("PBSZ", "0");
				checkReply("PBSZ 0");
				sendCommand("PROT", "P");
				checkReply("PROT P");
				sendCommand("PASV");
				checkReply("PASV");
		
				// Parse the host and port name to which the result is send
				String reply = getReplyString(); 
				String line = reply.substring(reply.indexOf('(')+1, reply.lastIndexOf(')'));
				String[] hostinfo = line.split(",");
				String host=hostinfo[0] + "." + hostinfo[1] + "." + hostinfo[2] + "." + hostinfo[3];
				int port=(Integer.parseInt(hostinfo[4]) << 8) + Integer.parseInt(hostinfo[5]);
				log.debug("channel from pasv reply="+host+":"+port);
				InetSocketAddress address = new InetSocketAddress(host,port);

				// connect to the result address
				Socket socket = new Socket();
				socket.connect(address);
				socket.setSoTimeout(1000);
				host=socket.getInetAddress().getHostAddress();
				port=socket.getPort();
				log.debug("channel from socket="+host+":"+port);
				socket = socketFactory.createSocket(socket, host, port, true);

				String cmdLine=FTPCommand.getCommand(cmdNr);
				if (param!=null) {
					cmdLine+=' ' + param;
				}
				// send the requested command (over the original socket)  <-- toch maar niet! GvB
//				_sendCommand(cmdLine, _socket_.getOutputStream(), null);			
				sendCommand(cmdNr, param);
				
				// return the new socket for the reply 
				return socket;
				
			}
		}
		return super._openDataConnection_(cmdNr, param);
	}




	private String getProtocol() {
		if (this.session.getFtpType() == FtpSession.FTPS_IMPLICIT) {
			return "TLS";
		}
		else if (this.session.getFtpType() == FtpSession.FTPS_EXPLICIT_TLS) {
			return "TLS";
		}
		else if (this.session.getFtpType() == FtpSession.FTPS_EXPLICIT_SSL) {
			return "SSL";
		}
		else {
			return null;
		}
	}
	private AuthSSLProtocolSocketFactoryBase createSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		URL certificateUrl = null;
		URL truststoreUrl = null;

		if (!StringUtils.isEmpty(session.getCertificate())) {
			certificateUrl = ClassUtils.getResourceURL(this, session.getCertificate());
			if (certificateUrl == null) {
				throw new IOException("Cannot find URL for certificate resource [" + session.getCertificate() + "]");
			}
			log.debug("resolved certificate-URL to [" + certificateUrl.toString() + "]");
		}
		if (!StringUtils.isEmpty(session.getTruststore())) {
			truststoreUrl = ClassUtils.getResourceURL(this, session.getTruststore());
			if (truststoreUrl == null) {
				throw new IOException("cannot find URL for truststore resource [" + session.getTruststore() + "]");
			}
			log.debug("resolved truststore-URL to [" + truststoreUrl.toString() + "]");
		}

		AuthSSLProtocolSocketFactoryBase factory = AuthSSLProtocolSocketFactoryBase.createSocketFactory(
			certificateUrl,
			session.getCertificateAuthAlias(),
			session.getCertificatePassword(),
			session.getCertificateType(),
			session.getKeyManagerAlgorithm(),
			truststoreUrl,
			session.getTruststoreAuthAlias(),
			session.getTruststorePassword(),
			session.getTruststoreType(),
			session.getTrustManagerAlgorithm(),
			session.isVerifyHostname(),
			session.isJdk13Compatibility());
			
		factory.setProtocol(getProtocol());
		factory.setAllowSelfSignedCertificates(session.isAllowSelfSignedCertificates());

		return factory;
	}



	/*
	 * send the comand and read the reply
	 */
	private String _sendCommand(String cmd, OutputStream out, InputStream in) throws IOException {
		// send the command
		log.debug("_sendCommand ["+cmd+"]"); 
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "ISO-8859-1"));
		writer.write(cmd + "\r\n");
		writer.flush();
		
		// if no in has been passed, then don't read the reply
		if (in == null) {
			return "";
		}
		
		return (String)_readReply(in, true);
	}
	
	private Object _readReply(InputStream in, boolean concatenateLines) throws IOException { 
		// obtain the result
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
	
		int replyCode = 0;
		StringBuffer reply = new StringBuffer();
		List replyList = new ArrayList();
		String line = reader.readLine();
	
		if (line == null)
			throw new FTPConnectionClosedException("Connection closed without indication.");
		reply.append(line).append("\n");
		replyList.add(line);
	
		// In case we run into an anomaly we don't want fatal index exceptions
		// to be thrown.
		int length = line.length();
		if (length < 3)
			throw new MalformedServerReplyException("Truncated server reply: " + line);
	
		try {
			String code = line.substring(0, 3);
			replyCode = Integer.parseInt(code);
		}
		catch (NumberFormatException e) {
			throw new MalformedServerReplyException("Could not parse response code.\nServer Reply: " + line);
		}
	
		// Get extra lines if message continues.
		if (length > 3 && line.charAt(3) == '-') {
			do {
				line = reader.readLine();
				if (line == null)
					throw new FTPConnectionClosedException("Connection closed without indication after having read ["+reply.toString()+"]");
	
				reply.append(line).append("\n");
				replyList.add(line);
			}
			while (!(line.length() >= 4 && line.charAt(3) != '-' && Character.isDigit(line.charAt(0))));
		}
	
		if (replyCode == FTPReply.SERVICE_NOT_AVAILABLE)
			throw new FTPConnectionClosedException("FTP response 421 received. Server closed connection.");
			
		if (!FTPReply.isPositiveCompletion(replyCode)) 
			throw new IOException("Exception while sending command \n" + reply.toString());
	
		log.debug("_readReply ["+reply.toString()+"]");
	
		if (concatenateLines) {
			return reply.toString();
		}
		return (String[])replyList.toArray(new String[0]);
	}
	
}
