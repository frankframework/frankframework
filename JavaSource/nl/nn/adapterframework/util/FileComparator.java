/*
 * $Log: FileComparator.java,v $
 * Revision 1.2  2013-03-18 15:41:08  m00f069
 * Added log and author
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.util.Comparator;

/**
 * @author Jaco de Groot
 */
public class FileComparator implements Comparator<File> {

	public int compare(File file1, File file2) {
		long l = file1.lastModified() - file2.lastModified();
		if (l < 0) {
			return -1;
		} if (l > 0) {
			return 1;
		} else {
			return 0;
		}
	}

}
