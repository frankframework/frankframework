/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.filesystem.ftp;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPFile;

import lombok.Getter;

/**
 * Wrapper around a FTPFile to allow for relative path operations
 *
 * @author Niels Meijer
 *
 */
public class FTPFileRef extends FTPFile {

	private static final long serialVersionUID = 9010790363003272021L;
	private @Getter String folder;

	private FTPFileRef() {
		setType(FTPFile.FILE_TYPE);
	}

	/**
	 * Create a new file reference, strips the folder of the filename when present
	 */
	public FTPFileRef(String name) {
		this();
		setName(name);
	}

	/**
	 * @param name A canonical name might be provided, strip the path when present and only use the actual file name.
	 * @param folder The directory the file. This always has precedence over the canonical path provided by the name.
	 */
	public FTPFileRef(String name, String folder) {
		this();
		setName(name);
		setFolder(folder);
	}

	/**
	 * Returns the filename, not the full (relative) path
	 */
	public String getFileName() {
		return super.getName();
	}

	/** Strip folder prefix of filename if present. May not be changed after creation */
	@Override
	public void setName(String name) {
		String normalized = FilenameUtils.normalize(name, true);
		super.setName(FilenameUtils.getName(normalized));
		setFolder(FilenameUtils.getFullPathNoEndSeparator(normalized));
	}

	/** Overwrites the folder of the file, in case setName was called with a canonical path */
	private void setFolder(String folder) {
		if(StringUtils.isNotEmpty(folder)) {
			this.folder = FilenameUtils.normalize(folder, true);
		}
	}

	/** Returns the canonical name inclusive file path when present */
	@Override
	public String getName() {
		String prefix = folder != null ? folder + "/" : "";
		String returnValue = prefix + super.getName();
		if (org.springframework.util.StringUtils.hasLength(returnValue)) {
			return returnValue;
		}
		return null;
	}

	@Override
	public String toString() {
		return "file-ref name["+super.getName()+"] folder["+getFolder()+"]";
	}

	/** Update the FTPFile attributes */
	public void updateFTPFile(FTPFile ftpFile) {
		setGroup(ftpFile.getGroup());
		setHardLinkCount(ftpFile.getHardLinkCount());
		setLink(ftpFile.getLink());
		setName(ftpFile.getName());
		setRawListing(ftpFile.getRawListing());
		setSize(ftpFile.getSize());
		setTimestamp(ftpFile.getTimestamp());
		setType(ftpFile.getType());
		setUser(ftpFile.getUser());
	}

	/**
	 * Creates a deep-copy of FTPFile
	 */
	public static FTPFileRef fromFTPFile(FTPFile ftpFile) {
		return fromFTPFile(ftpFile, null);
	}

	/**
	 * Creates a deep-copy of FTPFile, relative to the provided folder
	 */
	public static FTPFileRef fromFTPFile(FTPFile ftpFile, String folder) {
		FTPFileRef file = new FTPFileRef();
		file.updateFTPFile(ftpFile);
		file.setFolder(folder);
		return file;
	}
}
