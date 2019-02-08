/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Ignore;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpSession;

/**
 * To use this test class, set the local parameters and comment out the @Ignore tag.
 * 
 * @author Daniël Meyer
 *
 */
@Ignore
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FTPFile, FtpFileSystem> {

	private FtpFileSystem ffs;

	// TODO: Add local connection parameters.
	
	private String username = "";
	private String password = "";
	private String host = "";
	private int port = 21;
	
	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		ffs = new FtpFileSystem();
		FtpSession session = ffs.getFtpSession();

		session.setUsername(username);
		session.setPassword(password);
		session.setHost(host);
		session.setPort(port);
		ffs.configure();

		return ffs;
	}

	@Override
	protected boolean _fileExists(String filename) {
		try {
			return ffs.exists(ffs.toFile(filename));
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected void _deleteFile(String filename) {
		try {
			ffs.deleteFile(ffs.toFile(filename));
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException {
		try {
			return ffs.createFile(ffs.toFile(filename));
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected InputStream _readFile(String filename) throws FileNotFoundException {
		try {
			return ffs.readFile(ffs.toFile(filename));
		} catch (FileSystemException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		try {
			ffs.createFolder(ffs.toFile(filename));
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}
}