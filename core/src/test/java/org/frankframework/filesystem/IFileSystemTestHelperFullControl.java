package org.frankframework.filesystem;

import java.util.Date;

public interface IFileSystemTestHelperFullControl extends IFileSystemTestHelper {

	public abstract void setFileDate(String folderName, String filename, Date modifiedDate) throws Exception;

}
