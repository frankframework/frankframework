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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Ignore;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

/**
 * To use this test class, set the local parameters and comment out the @Ignore tag.
 * 
 * @author Daniël Meyer
 *
 */
// @Ignore
public class FtpFileSystemTest extends FileSystemTest<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs = new FtpFileSystem();;
	FtpSession ftpSession = new FtpSession();

	private String username = "";
	private String password = "";
	private String host = "";
	private int port = 21;

	@Override
	public void setup() throws ConfigurationException, IOException {
		super.setup();
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);
		ftpSession.configure();
	}

	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		FtpSession session = ffs.getFtpSession();
		session.setUsername(username);
		session.setPassword(password);
		session.setHost(host);
		session.setPort(port);
		ffs.configure();
		return ffs;
	}

	@Override
	protected boolean _fileExists(String filename) throws IOException {
		try {
			close();
			open();
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((o.getName() + "/").equals(filename)) {
						return true;
					}
				} else if (o.getName().equals(filename)) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void close() {
		ftpSession.closeClient();
	}

	private void open() {
		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void _deleteFile(String filename) {
		try {
			close();
			open();
			ftpSession.ftpClient.deleteFile(filename);
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException {
		close();
		open();
		OutputStream out = ftpSession.ftpClient.storeFileStream(filename);
		return out;
	}

	@Override
	protected InputStream _readFile(String filename) throws IOException {
		close();
		open();
		InputStream is = ftpSession.ftpClient.retrieveFileStream(filename);
		return is;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		try {
			close();
			open();
			ftpSession.ftpClient.makeDirectory(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		try {
			close();
			open();
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((o.getName() + "/").equals(folderName)) {
						return true;
					}
				} else if (o.getName().equals(folderName)) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		deleteFile(folderName);
	}
}