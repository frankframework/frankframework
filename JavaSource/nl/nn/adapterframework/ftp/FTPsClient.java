/*
 * $Log: FTPsClient.java,v $
 * Revision 1.2  2005-11-11 12:30:40  europe\l166817
 * Aanpassingen door John Dekker
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactory;
import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactoryBase;
import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactoryForJsse10x;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.SocketFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

/**
 * Apaches FTPCLient doesn't support FTPS. This class do support
 * implicit and explicit FTPS.
 * 
 * @author John Dekker
 */
class FTPsClient extends FTPClient implements SocketFactory {
	public static final String version = "$RCSfile: FTPsClient.java,v $  $Revision: 1.2 $ $Date: 2005-11-11 12:30:40 $";
	protected Logger log = Logger.getLogger(this.getClass());;
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
			setSocketFactory(this);
		}
	}
	
	protected void _connectAction_() throws IOException {
		// if explicit FTPS, the socket connection is establisch unsecure
		if (session.getFtpType() == FtpSession.FTPS_EXPLICIT_SSL || session.getFtpType() == FtpSession.FTPS_EXPLICIT_TLS) {
			orgSocket = _socket_; // remember the normal socket 

			// set the properties to aan appropriate default
			_socket_.setSoTimeout(10000);
			_socket_.setKeepAlive(true);
			
			// now send the command to inform the server to transform the connection 
			// into a secure connection
			String protocol = getProtocol();
			log.debug(_sendCommand("AUTH " + protocol, orgSocket.getOutputStream(), orgSocket.getInputStream()));
			
			// replace the normal socket with the secure one 
			try {
				socketFactory.initSSLContext();
				_socket_ = socketFactory.createSocket(orgSocket, orgSocket.getInetAddress().getHostAddress(), orgSocket.getPort(), true);
				setSocketProperties(_socket_);

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
				if (getReplyCode() < 200 || getReplyCode() >= 300) {
					throw new IOException(getReplyString());
				}				
				sendCommand("PROT", "P");
				if (getReplyCode() < 200 || getReplyCode() >= 300) {
					throw new IOException(getReplyString());
				}				
				sendCommand("PASV");
				if (getReplyCode() < 200 || getReplyCode() >= 300) {
					throw new IOException(getReplyString());
				}				
		
				// Parse the host and port name to which the result is send
				String reply = getReplyString(); 
				String line = reply.substring(reply.indexOf('(')+1, reply.lastIndexOf(')'));
				String[] host = line.split(",");
				InetSocketAddress address = new InetSocketAddress(host[0] + "." + host[1] + "." + host[2] + "." + host[3],(Integer.parseInt(host[4]) << 8) + Integer.parseInt(host[5]));

				// connect to the result address
				Socket socket = new Socket();
				socket.connect(address);
				socket = socketFactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);

				// send the requested command (over the original socket)				
				sendCommand(cmdNr, param);
				
				// return the new socket for the reply 
				return socket;
			}
		}
		return super._openDataConnection_(cmdNr, param);
	}


	private void setSocketProperties(Socket socket) throws IOException {
		if (session.isJdk13Compatibility()) {	
		}
		else {
			javax.net.ssl.SSLSocket sslSocket = (javax.net.ssl.SSLSocket)socket; 
			sslSocket.setEnableSessionCreation(true);
			sslSocket.setUseClientMode(true);
			sslSocket.setNeedClientAuth(true);
			sslSocket.startHandshake();
		}
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

		AuthSSLProtocolSocketFactoryBase factory;
		if (session.isJdk13Compatibility()) {
			addProvider("sun.security.provider.Sun");
			addProvider("com.sun.net.ssl.internal.ssl.Provider");
			System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
			factory =
				new AuthSSLProtocolSocketFactoryForJsse10x(
					certificateUrl,
					session.getCertificatePassword(),
					session.getKeystoreType(),
					truststoreUrl,
					session.getTruststorePassword(),
					session.getTruststoreType(),
					session.isVerifyHostname());
		}
		else {
			factory =
				new AuthSSLProtocolSocketFactory(
					certificateUrl,
					session.getCertificatePassword(),
					session.getKeystoreType(),
					truststoreUrl,
					session.getTruststorePassword(),
					session.getTruststoreType(),
					session.isVerifyHostname());
		}
		factory.setProtocol(getProtocol());
		factory.setAllowSelfSignedCertificates(session.isAllowSelfSignedCertificates());

		return factory;
	}

	protected void addProvider(String name) {
		try {
			Class clazz = Class.forName(name);
			java.security.Security.addProvider((java.security.Provider)clazz.newInstance());
		} catch (Throwable t) {
			log.error("cannot add provider ["+name+"], "+t.getClass().getName()+": "+t.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createSocket(java.lang.String, int)
	 */
	public Socket createSocket(String host, int port) throws UnknownHostException, IOException {
		return socketFactory.createSocket(host, port);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
	 */
	public Socket createSocket(String host, int port, InetAddress localhost, int localport) throws UnknownHostException, IOException {
		return socketFactory.createSocket(host, port, localhost, localport);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createSocket(java.net.InetAddress, int)
	 */
	public Socket createSocket(InetAddress address, int port) throws IOException {
		return socketFactory.createSocket(address, port);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)
	 */
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		return socketFactory.createSocket(address, port, localAddress, localPort);
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createServerSocket(int)
	 */
	public ServerSocket createServerSocket(int port) throws IOException {
		return new ServerSocket(port);
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createServerSocket(int, int)
	 */
	public ServerSocket createServerSocket(int port, int backlog) throws IOException {
		return new ServerSocket(port, backlog);
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.net.SocketFactory#createServerSocket(int, int, java.net.InetAddress)
	 */
	public ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
		return new ServerSocket(port, backlog, bindAddr);
	}

	/*
	 * send the comand and read the reply
	 */
	private String _sendCommand(String cmd, OutputStream out, InputStream in) throws IOException {
		// send the command 
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
		ArrayList replyList = new ArrayList();
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
					throw new FTPConnectionClosedException("Connection closed without indication.");
	
				reply.append(line).append("\n");
				replyList.add(line);
			}
			while (!(line.length() >= 4 && line.charAt(3) != '-' && Character.isDigit(line.charAt(0))));
		}
	
		if (replyCode == FTPReply.SERVICE_NOT_AVAILABLE)
			throw new FTPConnectionClosedException("FTP response 421 received.  Server closed connection.");
			
		if (replyCode < 200 || replyCode >= 300) 
			throw new IOException("Exception while sending command \n" + reply.toString());
	
		if (concatenateLines)
			return reply.toString();
		else
			return (String[])replyList.toArray(new String[0]);
	}
	

}
