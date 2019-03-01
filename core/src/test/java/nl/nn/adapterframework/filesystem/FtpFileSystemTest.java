/*
   Copyright 2019 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

<<<<<<< HEAD
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.filesystem;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

/**
 * To use this test class, set the local parameters and comment out the @Ignore tag.
 * 
 * @author Daniël Meyer
 *
 */

public class FtpFileSystemTest extends FileSystemTest<FTPFile, FtpFileSystem> {

	private String username = "";
	private String password = "";
	private String host = "";
	private String remoteDirectory = "dummyFolder";
	private int port = 21;

	private FtpSession referenceSession;

	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		FtpFileSystem ffs = new FtpFileSystem();

		ffs.setUsername(username);
		ffs.setPassword(password);
		ffs.setHost(host);
		ffs.setPort(port);
		ffs.setRemoteDirectory(remoteDirectory);

		return ffs;
	}

	@Before
	public void open() throws FtpConnectException, ConfigurationException {
		referenceSession = new FtpSession();
		referenceSession.setUsername(username);
		referenceSession.setPassword(password);
		referenceSession.setHost(host);
		referenceSession.setPort(port);

		referenceSession.configure();
		referenceSession.openClient("");
	}

	@After
	public void close() {
		if (referenceSession != null)
			referenceSession.closeClient();
	}

	@Override
	protected boolean _fileExists(String filename) throws IOException, FtpConnectException, ConfigurationException {
		FTPFile[] files = referenceSession.ftpClient.listFiles();
		for (FTPFile o : files) {
			if (o.isDirectory()) {
				if ((o.getName() + "/").equals(filename)) {
					return true;
				}
			} else if (o.getName().equals(filename)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void _deleteFile(String filename) throws FtpConnectException, IOException, ConfigurationException {
		referenceSession.ftpClient.deleteFile(filename);
	}

	@Override
	protected OutputStream _createFile(String filename)
			throws IOException, FtpConnectException, ConfigurationException {
		OutputStream out = referenceSession.ftpClient.storeFileStream(filename);
		return completePendingCommand(out);
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		FilterOutputStream fos = new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				referenceSession.ftpClient.completePendingCommand();
			}
		};
		return fos;
	}

	@Override
	protected InputStream _readFile(String filename) throws IOException, FtpConnectException, ConfigurationException {
		InputStream is = referenceSession.ftpClient.retrieveFileStream(filename);
		return is;
	}

	@Override
	public void _createFolder(String filename) throws IOException, FtpConnectException, ConfigurationException {
		referenceSession.ftpClient.makeDirectory(filename);
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		FTPFile[] files = referenceSession.ftpClient.listFiles();
		for (FTPFile o : files) {
			if (o.isDirectory()) {
				if ((o.getName() + "/").equals(folderName)) {
					return true;
				}
			} else if (o.getName().equals(folderName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		deleteFile(folderName);
	}
}