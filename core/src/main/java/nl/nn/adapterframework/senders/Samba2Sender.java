/*
   Copyright 2019, 2023 WeAreFrank!

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

import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.smb.Samba2FileSystemDelegator;
import nl.nn.adapterframework.filesystem.smb.SmbFileRef;

/**
*
* Uses the (newer) SMB 2 and 3 protocol.
*
* Possible error codes:
* <br/>
* Pre-authentication information was invalid (24) / Idenitfier doesn't match expected value (906):  login information is incorrect
* Server not found in Kerberos database (7): Verify that the hostname is the FQDN and the server is using a valid SPN.
*
* @author Niels Meijer
*/
public class Samba2Sender extends FileSystemSender<SmbFileRef, Samba2FileSystem> implements Samba2FileSystemDelegator {

	public Samba2Sender() {
		setFileSystem(new Samba2FileSystem());
	}

}
