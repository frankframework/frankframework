/*
 * $Log: FileUtils.java,v $
 * Revision 1.1  2004-08-24 09:03:05  a1909356#db2admin
 * Utility class for copying files and directories
 *
 * Revision 1.1  2004/07/08 07:47:06  unknown <unknown@ibissource.org>
 * Moved KM expression evaluator to extensions project
 *
 * Revision 1.1  2004/07/07 09:45:25  unknown <unknown@ibissource.org>
 * Generate resources that are part of a war project within wsad
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author John Dekker
 *
 * Utilities concerning files and directories
 */
public class FileUtils {
	/**
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void copyFile(File source, File destination) throws IOException {
		FileChannel s = null;
		FileChannel d = null;
		try {
			s = new FileInputStream(source).getChannel();
			d = new FileOutputStream(destination).getChannel();
			s.transferTo(0, s.size(), d);
		}
		finally {
			if (d != null) try { d.close(); } catch(IOException e) {}
			if (s != null) try { s.close(); } catch(IOException e) {}
		}
	}
	
	/**
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void copyDirectory(File source, File destination) throws IOException {
		if (source.isFile()) {
			copyFile(source, destination);
		}
		else {
			destination.mkdirs();
			File[] childs = source.listFiles();
			for (int i = 0; i < childs.length; i++) {
				File srcChild = childs[i];
				File destChild = new File(destination.getAbsoluteFile() + File.separator + srcChild.getName());
				copyDirectory(srcChild, destChild);
			}
		}
	}
}
