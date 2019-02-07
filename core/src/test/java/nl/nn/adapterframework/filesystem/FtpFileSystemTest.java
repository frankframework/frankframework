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

import java.io.File;

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
public class FtpFileSystemTest extends LocalFileSystemTestBase<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs;
	
	// TODO: Add local connection parameters. 

	private String localFilePath = "";
	private String share = null;
	private String relativePath = "DummyFolder/";
	private String username = "";
	private String password = "";
	private String host = "";
	private int port = 21;

	@Override
	protected File getFileHandle(String filename) {
		return new File(localFilePath + relativePath + filename);
	}
	
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
}