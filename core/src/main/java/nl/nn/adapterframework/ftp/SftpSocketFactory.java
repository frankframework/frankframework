/*
   Copyright 2023 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.jcraft.jsch.SocketFactory;

/**
 * Jsch has a custom wrapper around the SSLSocketFactory.
 * Not sure why, but here we are...
 * 
 * @author Niels Meijer
 */
public class SftpSocketFactory implements SocketFactory {
	private SSLSocketFactory sslSocketFactory;

	public SftpSocketFactory(SSLContext sslContext) {
		this.sslSocketFactory = sslContext.getSocketFactory();
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		return sslSocketFactory.createSocket(host, port);
	}

	@Override
	public InputStream getInputStream(Socket socket) throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream(Socket socket) throws IOException {
		return socket.getOutputStream();
	}

}
