/*
   Copyright 2019, 2020 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.nio.file.Path;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.filesystem.FileSystemPipe;
import nl.nn.adapterframework.filesystem.LocalFileSystem;

public class LocalFileSystemPipe extends FileSystemPipe<Path, LocalFileSystem>{

	public LocalFileSystemPipe() {
		setFileSystem(new LocalFileSystem());
	}
	
	@IbisDoc({"1", "the folder that serves as the root of this virtual filesystem", "" })
	public void setRoot(String root) {
		getFileSystem().setRoot(root);
	}

	@IbisDoc({"2", "Filter of files to look for in inputdirectory, e.g. '*.inp'", ""})
	public void setWildcard(String wildcard) {
		getFileSystem().setWildcard(wildcard);
	}

	@IbisDoc({"3", "Filter of files to be excluded when looking in inputdirectory", ""})
	public void setExcludeWildcard(String excludeWildcard) {
		getFileSystem().setExcludeWildcard(excludeWildcard);
	}
}
