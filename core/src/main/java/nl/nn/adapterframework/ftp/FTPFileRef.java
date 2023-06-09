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
package nl.nn.adapterframework.ftp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPFile;

import lombok.Getter;
import nl.nn.adapterframework.util.FilenameUtils;

/**
 * Wrapper around a FTPFile to allow for relative path operations
 * 
 * @author Niels Meijer
 *
 */
public class FTPFileRef extends FTPFile {

	private static final long serialVersionUID = 9010790363003272021L;
	private @Getter String folder;

	public FTPFileRef() {
		setType(FTPFile.FILE_TYPE);
	}

	public FTPFileRef(String name) {
		this();
		setName(name);
	}

	public String getFileName() {
		return super.getName();
	}

	@Override
	public void setName(String name) {
		String normalized = FilenameUtils.normalize(name, true);
		super.setName(FilenameUtils.getName(normalized));
		setFolder(FilenameUtils.getFullPathNoEndSeparator(normalized));
	}

	public void setFolder(String folder) {
		if(StringUtils.isNotEmpty(folder)) {
			if(this.folder != null) {
				this.folder = FilenameUtils.normalize(folder + "/" + this.folder, true);
			} else {
				this.folder = folder;
			}
		}
	}

	@Override
	public String getName() {
		String prefix = folder != null ? folder + "/" : "";
		return prefix + super.getName();
	}

	@Override
	public String toString() {
		return "file-ref name["+super.getName()+"] folder["+getFolder()+"]";
	}

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
		FTPFileRef file = new FTPFileRef();
		file.setGroup(ftpFile.getGroup());
		file.setHardLinkCount(ftpFile.getHardLinkCount());
		file.setLink(ftpFile.getLink());
		file.setName(ftpFile.getName());
		file.setRawListing(ftpFile.getRawListing());
		file.setSize(ftpFile.getSize());
		file.setTimestamp(ftpFile.getTimestamp());
		file.setType(ftpFile.getType());
		file.setUser(ftpFile.getUser());
		return file;
	}
}