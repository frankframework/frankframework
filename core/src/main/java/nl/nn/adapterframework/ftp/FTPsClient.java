/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.adapterframework.ftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactory;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Apaches FTPCLient doesn't support FTPS; This class does support
 * implicit and explicit FTPS.
 * 
 * @author John Dekker
 */
public class FTPsClient extends FTPClient implements IScopeProvider {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	public final String FTP_CLIENT_CHARSET="ISO-8859-1";

	private FtpSession session;
	private AuthSSLProtocolSocketFactory socketFactory;
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
			socketFactory.getSSLContext();
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
	@Override
	public boolean completePendingCommand() throws IOException
	{
		if (FTPReply.isPositiveCompletion(getReplyCode())) {
			return true;
		}
		return super.completePendingCommand();
	}

		
	
	@Override
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
				socketFactory.getSSLContext();
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
				IOException ioe = new IOException("Unexpected error");
				ioe.initCause(e);
				throw ioe;
			}
		}
		else {
			super._connectAction_();
		}
	}
	
	@Override
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
	private AuthSSLProtocolSocketFactory createSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
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

		AuthSSLProtocolSocketFactory factory = AuthSSLProtocolSocketFactory.createSocketFactory(
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
			session.isAllowSelfSignedCertificates(),
			session.isVerifyHostname(),
			false);
			
		factory.setProtocol(getProtocol());

		return factory;
	}



	/*
	 * send the comand and read the reply
	 */
	private String _sendCommand(String cmd, OutputStream out, InputStream in) throws IOException {
		// send the command
		log.debug("_sendCommand ["+cmd+"]"); 
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, FTP_CLIENT_CHARSET));
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
		BufferedReader reader = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(in, FTP_CLIENT_CHARSET));
	
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
			MalformedServerReplyException mfre = new MalformedServerReplyException("Could not parse response code.\nServer Reply [" + line+"]");
			mfre.initCause(e);
			throw mfre;
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
