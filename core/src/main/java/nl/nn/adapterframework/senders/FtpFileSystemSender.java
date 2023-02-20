/*
   Copyright 2019-2023 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.doc.ReferTo;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.ftp.FtpFileSystemDelegator;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;

public class FtpFileSystemSender extends FileSystemSender<FTPFileRef, FtpFileSystem> implements FtpFileSystemDelegator {

	public FtpFileSystemSender() {
		setFileSystem(new FtpFileSystem());
	}

	@ReferTo(FtpFileSystem.class)
	@Deprecated
	@ConfigurationWarning("use attribute ftpType instead")
	public void setFtpTypeDescription(FtpType ftpTypeDescription) {
		getFileSystem().setFtpTypeDescription(ftpTypeDescription);
	}

	@ReferTo(FtpFileSystem.class)
	@Deprecated
	@ConfigurationWarning("use attribute prot=\"P\" instead")
	public void setProtP(boolean protP) {
		getFileSystem().setProtP(protP);
	}
}