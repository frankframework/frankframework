/*
 * $Log: FileUtils.java,v $
 * Revision 1.18  2010-08-09 13:03:40  m168309
 * added canWrite()
 *
 * Revision 1.17  2009/12/31 10:06:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * SendJmsMessage/TestIfsaService/TestPipeLine: made zipfile-upload facility case-insensitive
 *
 * Revision 1.16  2009/08/05 14:24:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE writing to null directory
 *
 * Revision 1.15  2009/02/04 13:04:07  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added appendFile()
 *
 * Revision 1.14  2009/01/08 16:40:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getWeeklyRollingFile()
 *
 * Revision 1.13  2008/09/04 12:17:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getDailyRollingFile()
 *
 * Revision 1.12  2008/07/15 12:16:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.11  2008/02/15 13:56:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added functions for backing up, moving and deleting  processed files
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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
import org.apache.log4j.Logger;


/**
 * Utilities for batch file handling.
 * 
 * @author John Dekker
 * @version Id
 */
public class FileUtils {
	public static final String version = "$RCSfile: FileUtils.java,v $  $Revision: 1.18 $ $Date: 2010-08-09 13:03:40 $";
	static Logger log = LogUtil.getLogger(FileUtils.class);

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

	public static void moveFileAfterProcessing(File orgFile, String destDir, boolean delete, boolean overwrite, int numBackups) throws InterruptedException, IOException {
		if (delete) {
			if (orgFile.exists()) {
				orgFile.delete();
//			} else {
//				log.warn("file ["+orgFile.getParent()+"] does not exist anymore, cannot delete");
			}
		} else {
			if (StringUtils.isNotEmpty(destDir)) {
				moveFile(orgFile, destDir, overwrite, numBackups);
			}
		}
	}

	
	public static String moveFile(String filename, String destDir, boolean overwrite, int numBackups) throws InterruptedException, IOException {
		File srcFile = new File(filename);
		return moveFile(srcFile, destDir, overwrite, numBackups);
	}

	public static String moveFile(File orgFile, String destDir, boolean overwrite, int numBackups) throws InterruptedException, IOException {
		File dstFile = new File(destDir, orgFile.getName());
		return moveFile(orgFile, dstFile, overwrite, numBackups);
	}

	public static String moveFile(File orgFile, File rename2File, boolean overwrite, int numBackups) throws InterruptedException, IOException {
		return moveFile(orgFile, rename2File, overwrite, numBackups, 5, 500);
	}

	public static String moveFile(File orgFile, File rename2File, boolean overwrite, int numBackups, int nrRetries, long waitTime) throws InterruptedException, IOException {
		if (orgFile.exists()) {
			if (numBackups>0) {
				makeBackups(rename2File,numBackups);
			} else {
				if (overwrite && rename2File.exists()) {
					rename2File.delete();
				}
			}
		}
		String result=moveFile(orgFile, rename2File, nrRetries, waitTime);
		if (result==null) {
			throw new IOException("Could not move file ["+orgFile.getPath()+"] to ["+rename2File.getPath()+"]");
		}
		return result;
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

	public static String appendFile(File orgFile, File destFile, int nrRetries, long waitTime) throws InterruptedException {
		int errCount = 0;
		
		while (errCount++ < nrRetries) {
			boolean success = copyFile(orgFile, destFile, true);
			
			if (! success) {
				Thread.sleep(waitTime);
			}
			else {
				return destFile.getAbsolutePath();
			}
		}
		return null;
	}

	private static boolean copyFile(File orgFile, File destFile, boolean append) {
		try {
			FileInputStream fis = new FileInputStream(orgFile);
			FileOutputStream fos = new FileOutputStream(destFile, append);
			byte[] buf = new byte[1024];
			int len;
			while ((len = fis.read(buf)) > 0) {
				fos.write(buf, 0, len);
			}
			fis.close();
			fos.close();
		} catch (IOException e) {
			log.warn("Could not copy file ["+orgFile.getPath()+"] to ["+destFile.getPath()+"]", e);
			return false;
		}
		return true;
	}

	/**
	 * 
	 */
	public static void makeBackups(File targetFile, int numBackups)  {
		if (numBackups<=0 || !targetFile.exists()) {
			return;
		}
		if (numBackups>1) {
			File curFile=null;
			int i=1;
			// check for currently available backup files
			for (;i<=numBackups; i++) {
				String filename=targetFile.getPath()+"."+i;
				curFile=new File(filename);
				if (!curFile.exists())  {
					break;
				}
			}
			// delete the oldest backup file
			if (i>numBackups) {
				curFile.delete();
			}
			// move all backup files one step up
			for(;i>1;i--) {
				String srcFilename=targetFile.getPath()+"."+(i-1);
				File srcFile=new File(srcFilename);
				srcFile.renameTo(curFile);
				curFile=srcFile;
			}
		}
		// move current file to backup 
		String backupFilename=targetFile.getPath()+".1";
		File backupFile=new File(backupFilename);
		targetFile.renameTo(backupFile);
	}

	public static File getWeeklyRollingFile(String directory, String filenamePrefix, String filenameSuffix, int retentionDays) {
		return getRollingFile(directory, filenamePrefix, "yyyy'W'ww", filenameSuffix, retentionDays);
	}
	
	public static File getDailyRollingFile(String directory, String filenamePrefix, String filenameSuffix, int retentionDays) {
		return getRollingFile(directory, filenamePrefix, "yyyy-MM-dd", filenameSuffix, retentionDays);
	}
	
	public static File getRollingFile(String directory, String filenamePrefix, String dateformat, String filenameSuffix, int retentionDays) {
		
		final long millisPerDay=24*60*60*1000;

		if (directory==null) {
			return null;
		}
		Date now=new Date();

		String filename=filenamePrefix+DateUtils.format(now,dateformat)+filenameSuffix;
		File result = new File(directory+"/"+filename);
		if (!result.exists()) {
			int year=now.getYear();
			int month=now.getMonth();
			int date=now.getDate();
		
			long thisMorning = new Date(year, month, date).getTime();

			long deleteBefore = thisMorning - retentionDays * millisPerDay;

			WildCardFilter filter = new WildCardFilter(filenamePrefix+"*"+filenameSuffix);
			File dir = new File(directory);
			File[] files = dir.listFiles(filter);

			int count = (files == null ? 0 : files.length);
			for (int i = 0; i < count; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					continue;
				}
				if (file.lastModified()<deleteBefore) {
					file.delete();
				}
			}
		}
		
		return result;

	}


	public static File[] getFiles(String directory, String wildcard) {
		return getFiles(directory, wildcard, null);
	}

	public static File[] getFiles(String directory, String wildcard, String excludeWildcard) {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(directory);
		File[] files = dir.listFiles(filter);

		WildCardFilter excludeFilter = null;
		if (excludeWildcard!=null) {
			excludeFilter = new WildCardFilter(excludeWildcard);
		}
		
		List result = new ArrayList();
		int count = (files == null ? 0 : files.length);
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				continue;
			}
			if (excludeFilter!=null && excludeFilter.accept(dir, file.getName())) {
				continue;
			}
			result.add(file);
		}
		return (File[])result.toArray(new File[0]);
	}

	public static File getFirstMatchingFile(String directory, String wildcard) {
		return getFirstMatchingFile(directory, wildcard, null);
	}

	public static File getFirstMatchingFile(String directory, String wildcard, String excludeWildcard) {
		File[] files = getFiles(directory, wildcard, excludeWildcard);
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

	public static String getFileNameExtension(String fileName) {
		int idx = fileName.lastIndexOf('.');
		if (idx<0) {
			return null;
		} else {
			idx++;
			if (idx >= fileName.length()) {
				return null;
			} else {
				return fileName.substring(idx);
			}
		}
	}

	public static boolean extensionEqualsIgnoreCase(String fileName, String extension) {
		String fileNameExtension = getFileNameExtension(fileName);
		if (fileNameExtension==null) {
			return false;
		} else {
			return fileNameExtension.equalsIgnoreCase(extension);
		}
	}

	public static boolean canWrite(String directory) {
		try {
			File file = new File(directory);
			if (!file.exists()) {
				file.mkdirs();
			}
			if (!file.isDirectory()) {
				log.debug("Directory [" + directory + "] is not a directory");
				return false;
			}
			File tmpFile = File.createTempFile("ibis", null, file);
			try {
				tmpFile.delete();
			} catch (Throwable t) {
				log.warn("Exception while deleting temporary file [" + tmpFile.getName() + "] in directory [" + directory + "]",t);
			}
			return true;
		} catch (IOException ioe) {
			log.debug("Exception while creating a temporary file in directory [" + directory + "]",ioe);
			return false;
		} catch (SecurityException se) {
			log.debug("Exception while testing if the application is allowed to write to directory [" + directory + "]",se);
			return false;
		}
	}
}
