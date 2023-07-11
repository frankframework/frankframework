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
package nl.nn.adapterframework.filesystem.smb;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.hierynomus.msfscc.fileinformation.FileAllInformation;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.FilenameUtils;

public class SmbFileRef {
	private @Getter String filename;
	private @Getter String folder;
	private @Nullable @Getter @Setter FileAllInformation attributes = null;

	public SmbFileRef() {
		this(null);
	}

	public SmbFileRef(String name) {
		setName(name);
	}

	//strip folder prefix of filename if present
	public void setName(String filename) {
		String normalized = FilenameUtils.normalize(filename, false);
		this.filename = FilenameUtils.getName(normalized);
		setFolder(FilenameUtils.getFullPathNoEndSeparator(normalized));
	}

	public void setFolder(String folder) {
		if(StringUtils.isNotEmpty(folder)) {
			this.folder = FilenameUtils.normalize(folder, false);
		}
	}

	/** full path (folder + name) */
	public String getName() {
		String prefix = folder != null ? folder + "\\" : "";
		return prefix + filename;
	}

	@Override
	public String toString() {
		return new StringBuilder("SMBFile name [").append(filename)
				.append("] in folder [").append(folder).append("]").toString();
	}
}
