/*
 * $Log: FileUtils.java,v $
 * Revision 1.9.4.1  2007-10-10 14:30:36  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.10  2007/10/08 13:35:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.9  2007/02/05 15:02:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.8  2005/11/08 09:31:08  John Dekker <john.dekker@ibissource.org>
 * Bug concerning filenames resolved
 *
 * Revision 1.7  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.6  2005/10/11 12:52:57  John Dekker <john.dekker@ibissource.org>
 * Change version string to not include $id: $, since username contains a 
 * backslash
 *
 * Revision 1.5  2005/10/11 12:38:11  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.4  2005/10/11 12:15:06  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.3  2005/10/11 12:00:19  John Dekker <john.dekker@ibissource.org>
 * Add utility class with methods for dynamically creating filenames, 
 * with alignment (padding) methods and list-2-string and string-2-list functions
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.StringUtils;


/**
 * Utilities for batch file handling.
 * 
 * @author John Dekker
 * @version Id
 */
public class FileUtils {
	public static final String version = "$RCSfile: FileUtils.java,v $  $Revision: 1.9.4.1 $ $Date: 2007-10-10 14:30:36 $";

	/**
	 * Construct a filename from a pattern and session variables. 
	 */
	public static String getFilename(ParameterList definedParameters, PipeLineSession session, String originalFilename, String filenamePattern) throws ParameterException {
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
		
		// construct the parameterlist
		ParameterList pl = new ParameterList();
		try {
			if (definedParameters != null)
				pl.addAll(definedParameters);
			Parameter p = new Parameter();
			p.setName("file");
			p.setDefaultValue(name);
			p.configure();
			pl.add(p);
			p = new Parameter();
			p.setName("ext");
			p.setDefaultValue(extension);
			p.configure();
			pl.add(p);
			p = new Parameter();
			p.setName("__filename");
			p.setPattern(filenamePattern);
			p.configure();
			pl.add(p);
		}
		catch(ConfigurationException e) {
			throw new ParameterException(e);		
		}
		
		// resolve the parameters
		ParameterResolutionContext prc = new ParameterResolutionContext((String)null, session);
		ParameterValueList pvl = prc.getValues(pl);
		String filename = pvl.getParameterValue("__filename").getValue().toString(); 
		
		return filename;
	}
	
	public static String getFilename(ParameterList definedParameters, PipeLineSession session, File originalFile, String filenamePattern) throws ParameterException {
		if (originalFile == null)
			return getFilename(definedParameters, session, "", filenamePattern);
		return getFilename(definedParameters, session, originalFile.getName(), filenamePattern);
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
		
		List result = new ArrayList();
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
		
		List result = new ArrayList();
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
