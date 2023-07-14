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

import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

import lombok.Getter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.FilenameUtils;

/**
 * Wrapper around a FTPFile to allow for relative path operations
 * 
 * @author Niels Meijer
 *
 */
public class SftpFileRef {

	private @Getter String folder;
	private String name;
	private SftpATTRS attributes = null;

	public SftpFileRef() {
	}

	public SftpFileRef(String name) {
		this(name, null);
	}

	public SftpFileRef(String name, String folder) {
		setName(name);
		setFolder(folder);
	}

	public String getFilename() {
		return name;
	}

	private void setName(String name) {
		String normalized = FilenameUtils.normalize(name, true);
		this.name = FilenameUtils.getName(normalized);
		setFolder(FilenameUtils.getFullPathNoEndSeparator(normalized));
	}

	private void setFolder(String folder) {
		if(StringUtils.isNotEmpty(folder)) {
			this.folder = FilenameUtils.normalize(folder, true);
		}
	}

	public String getName() {
		String prefix = folder != null ? folder + "/" : "";
		return prefix + name;
	}

	@Override
	public String toString() {
		return "file-ref name["+name+"] folder["+getFolder()+"]";
	}

	/**
	 * Creates a deep-copy of FTPFile
	 */
	public static SftpFileRef fromLsEntry(LsEntry entry) {
		return fromLsEntry(entry, null);
	}

	public static SftpFileRef fromLsEntry(LsEntry entry, String folder) {
		SftpFileRef file = new SftpFileRef();
		file.setName(entry.getFilename());
		file.setAttrs(entry.getAttrs());
		file.setFolder(folder);
		return file;
	}

	public long getSize() {
		if(attributes == null) {
			return Message.MESSAGE_SIZE_UNKNOWN;
		}

		return attributes.getSize();
	}

	public SftpATTRS getAttrs() {
		return attributes;
	}

	public void setAttrs(SftpATTRS attributes) {
		this.attributes = attributes;
	}
}