package org.frankframework.filesystem;

import java.util.Date;

public interface IFileSystemTestHelperFullControl extends IFileSystemTestHelper {

	void setFileDate(String folderName, String filename, Date modifiedDate) throws Exception;

}
