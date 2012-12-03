package nl.nn.adapterframework.util;

import java.io.File;
import java.util.Comparator;

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
