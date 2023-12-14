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
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.filesystem.FileSystemListener;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.smb.Samba2FileSystemDelegator;
import nl.nn.adapterframework.filesystem.smb.SmbFileRef;

public class Samba2Listener extends FileSystemListener<SmbFileRef, Samba2FileSystem> implements Samba2FileSystemDelegator {

	@Override
	protected Samba2FileSystem createFileSystem() {
		return new Samba2FileSystem();
	}

}
