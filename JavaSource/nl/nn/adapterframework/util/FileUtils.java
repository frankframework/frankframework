/*
 * $Log: FileUtils.java,v $
 * Revision 1.3  2005-10-11 12:00:19  europe\m00f531
 * Add utility class with methods for dynamically creating filenames, 
 * with alignment (padding) methods and list-2-string and string-2-list functions
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;


/**
 * @author John Dekker
 * @version Id
 */
public class FileUtils {
	public static final String version="$Id: FileUtils.java,v 1.3 2005-10-11 12:00:19 europe\m00f531 Exp $";

	public static String getFilename(String originalFilename, String filenamePattern, boolean uidParam) {
		// no pattern defined, outputname = inputname
		if (StringUtils.isEmpty(filenamePattern)) {
			return originalFilename; 
		}
		
		// obtain filename
		int ndx = originalFilename.lastIndexOf('.');
		String name = originalFilename;
		String extension = "";

		if (ndx > 0) {
			name = originalFilename.substring(0, ndx);
			extension = originalFilename.substring(ndx + 1);
		}
		Object[] params;
		if (uidParam) {
			params = new Object[] { name, extension, new Date(), Misc.createSimpleUUID()};
		}
		else {
			params = new Object[] { name, extension, new Date()};
		}
		return MessageFormat.format(filenamePattern, params).toString();
	}
	
	public static String getFilename(File originalFile, String filenamePattern, boolean uidParam) {
		return getFilename(originalFile.getName(), filenamePattern, uidParam);
	}
	
	public static String moveFile(File orgFile, File rename2File, int nrRetries, long waitTime) throws InterruptedException {
		int errCount = 0;
		
		while (errCount++ < nrRetries) {
			// Move file to new directory
			boolean success = orgFile.renameTo(rename2File);
			
			if (! success) {
				Thread.sleep(waitTime);
			}
			else {
				return rename2File.getAbsolutePath();
			}
		}
		return null;
	}

	public static File[] getFiles(String directory, String wildcard) {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(directory);
		File[] files = dir.listFiles(filter);
		
		ArrayList result = new ArrayList();
		int count = (files == null ? 0 : files.length);
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				continue;
			}
			result.add(file);
		}
		return (File[])result.toArray(new File[0]);
	}

	public static File getFirstMatchingFile(String directory, String wildcard) {
		File[] files = getFiles(directory, wildcard);
		if (files.length > 0)
			return files[0];

		return null;
	}

	public static File[] getFiles(String directory, final String[] names) {
		File dir = new File(directory);
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				for (int i = 0; i < names.length; i++) {
					if (name.equals(names[i]))
						return true;
				}
				return false;
			}
		});
		
		ArrayList result = new ArrayList();
		int count = (files == null ? 0 : files.length);
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				continue;
			}
			result.add(file);
		}
		return (File[])result.toArray(new File[0]);
	}

	public static File getFirstMatchingFile(String directory, String[] names) {
		File[] files = getFiles(directory, names);
		if (files.length > 0)
			return files[0];

		return null;
	}

	public static List getListFromNames(String names, char seperator) {
		StringTokenizer st = new StringTokenizer(names, "" + seperator);
		LinkedList list = new LinkedList();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}

	public static List getListFromNames(String[] names) {
		if (names == null)
			return null;
		return Arrays.asList(names);
	}

	public static String getNamesFromArray(String[] names, char seperator) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			if (result.length() > 0)
				result.append(seperator);
			result.append(name);
		}
		return result.toString();
	}
	
	public static String getNamesFromList(List filenames, char seperator) {
		if (filenames == null)
			return "";
			
		StringBuffer result = new StringBuffer();
		for (Iterator nameIterator = filenames.iterator(); nameIterator.hasNext();) {
			String name = (String)nameIterator.next();
			if (result.length() > 0)
				result.append(seperator);
			result.append(name);
		}
		return result.toString();
	}
	
	/*
	 * methods to create a fixed length string from a value
	 */
	public static String align(String val, int length, boolean leftAlign, char fillchar) {
		StringBuffer result = new StringBuffer();
		align(result, val, length, leftAlign, fillchar);
		return result.toString();
	}
	
	public static void align(StringBuffer result, String val, int length, boolean leftAlign, char fillchar) {
		if (val.length() > length) {
			result.append(val.substring(0, length));
		}
		else if (val.length() == length) {
			result.append(val);
		}
		else {
			char[] fill = getFilledArray(length - val.length(), fillchar);
			if (leftAlign) {
				result.append(val).append(fill);			
			}
			else {
				result.append(fill).append(val);
			}
		}
	}

	/*
	 * create a filled array   
	 */
	public static char[] getFilledArray(int length, char fillchar) {
		char[] fill = new char[length];
		Arrays.fill(fill, fillchar);
		return fill;
	}
	
}
