/*
   Copyright 2023-2024 WeAreFrank!

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
package org.frankframework.filesystem.sftp;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.stream.Message;

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
	private @Setter @Getter boolean isDirectory = false;

	private SftpFileRef() {
	}

	public SftpFileRef(String name) {
		this(name, null);
	}

	/**
	 * @param name A canonical name might be provided, strip the path when present and only use the actual file name.
	 * @param folder The directory the file. This always has precedence over the canonical path provided by the name.
	 */
	public SftpFileRef(String name, String folder) {
		setName(name);
		setFolder(folder);
	}

	/**
	 * Returns the filename, not the full (relative) path
	 */
	public String getFilename() {
		return name;
	}

	/** Strip folder prefix of filename if present. May not be changed after creation */
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

	/** Returns the canonical name inclusive file path when present */
	public String getName() {
		String prefix = folder != null ? folder + "/" : "";
		String returnValue = prefix + name;
		if (org.springframework.util.StringUtils.hasLength(returnValue)) {
			return returnValue;
		}
		return null;
	}

	@Override
	public String toString() {
		return "file-ref name["+name+"] folder["+getFolder()+"]";
	}

	/**
	 * Creates a deep-copy of LsEntry
	 */
	public static SftpFileRef fromLsEntry(LsEntry entry) {
		return fromLsEntry(entry, null);
	}

	/**
	 * Creates a deep-copy of LsEntry, relative to the provided folder
	 */
	public static SftpFileRef fromLsEntry(LsEntry entry, String folder) {
		SftpFileRef file = new SftpFileRef();
		file.setName(entry.getFilename());
		file.setFolder(folder);
		file.setAttrs(entry.getAttrs());
		file.setDirectory(entry.getAttrs().isDir());
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
