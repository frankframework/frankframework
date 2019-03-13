/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
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
 */
public class FileUtils {
	static Logger log = LogUtil.getLogger(FileUtils.class);

	/**
	 * Construct a filename from a pattern and session variables. 
	 */
	public static String getFilename(ParameterList definedParameters, IPipeLineSession session, String originalFilename, String filenamePattern) throws ParameterException {
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
	
	public static String getFilename(ParameterList definedParameters, IPipeLineSession session, File originalFile, String filenamePattern) throws ParameterException {
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

	public static String moveFile(File orgFile, File rename2File, boolean overwrite, int numBackups, int numberOfAttempts, long waitTime) throws InterruptedException, IOException {
		if (orgFile.exists()) {
			if (numBackups>0) {
				makeBackups(rename2File,numBackups);
			} else {
				if (overwrite && rename2File.exists()) {
					rename2File.delete();
				}
			}
		}
		String result=moveFile(orgFile, rename2File, numberOfAttempts, waitTime);
		if (result==null) {
			throw new IOException("Could not move file ["+orgFile.getPath()+"] to ["+rename2File.getPath()+"]");
		}
		return result;
	}

	public static String moveFile(File orgFile, File rename2File, int numberOfAttempts, long waitTime) throws InterruptedException {
		boolean rename2FileExists = rename2File.exists();
		int errCount = 0;
		
		while (errCount++ < numberOfAttempts) {
			// Move file to new directory using renameTo
			boolean success = orgFile.renameTo(rename2File);
			// Move file to new directory using copy and delete in case renameTo
			// doesn't work (for example when running on Linux and the file
			// needs to be moved to another filesystem).
			if (!success) {
				log.debug("Could not move file ["+orgFile.getPath()+"] to ["+rename2File.getPath()+"], now trying alternate move (copy and delete)");
				success = copyFile(orgFile, rename2File, false);
				if (success) {
					success = orgFile.delete();
					if (!success) {
						log.debug("Could not delete source file ["+orgFile.getPath()+"] after copying it to ["+rename2File.getPath()+"]");
						if (!rename2FileExists) {
							log.debug("Deleting destination file ["+rename2File.getPath()+"]: " + rename2File.delete());
						}
					}
				} else {
					log.debug("Could not copy file in alternate move");
				}
			}
			
			if (!success) {
				log.debug("Retries left for moving file [" + (numberOfAttempts - errCount) + "]");
				if (errCount < numberOfAttempts) {
					Thread.sleep(waitTime);
				}
			} else {
				return rename2File.getAbsolutePath();
			}
		}
		return null;
	}

	public static File getFreeFile(File file)  {
		if (file.exists()) {
			String extension = FileUtils.getFileNameExtension(file.getPath());
			int count = 1;
			while (true) {
				String newFileName;
				String countStr;
				if (count < 1000) {
					countStr = StringUtils.leftPad(("" + count), 3, "0");
				} else {
					countStr = "" + count;
				}
				if (extension!=null) {
					newFileName = StringUtils.substringBeforeLast(file.getPath(), ".") + "_" + countStr + "." + extension;
				} else {
					newFileName = file.getPath() + "_" + countStr;
				}
				File newFile = new File(newFileName);
				if (newFile.exists()) {
					count++;
				} else {
					return newFile;
				}
			}
		} else {
			return file;
		}
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

	public static boolean copyFile(File orgFile, File destFile, boolean append) {
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

	public static File createTempFile() throws IOException {
		return createTempFile(null);
	}
	public static File createTempFile(String suffix) throws IOException {
		return createTempFile(null,null);
	}
	public static File createTempFile(String prefix, String suffix) throws IOException {
		String directory=AppConstants.getInstance().getResolvedProperty("upload.dir");
		if (StringUtils.isEmpty(prefix)) {
			prefix="ibis";
		}
		if (StringUtils.isEmpty(suffix)) {
			suffix=".tmp";
		}
		if (log.isDebugEnabled()) log.debug("creating tempfile prefix ["+prefix+"] suffix ["+suffix+"] directory ["+directory+"]");
		File tmpFile = File.createTempFile(prefix, suffix, new File(directory));
		tmpFile.deleteOnExit();
		return tmpFile;
	}

	public static File createTempDir() throws IOException {
		return createTempDir(null);
	}
	public static File createTempDir(File baseDir) throws IOException {
		return createTempDir(baseDir, null);
	}
	public static File createTempDir(File baseDir, String subDir) throws IOException {
		return createTempDir(baseDir, subDir, null, null);
	}
	public static File createTempDir(File baseDir, String subDir, String prefix, String suffix) throws IOException {
		if (baseDir == null) {
			String baseDirStr = AppConstants.getInstance().getString("ibis.tmpdir", null);
			if (baseDirStr == null) {
				throw new IOException("Property [ibis.tmpdir] is not specified");
			}
			baseDir = new File(baseDirStr);

		}
		if (!baseDir.exists()) {
			if (!baseDir.mkdirs()) {
				throw new IOException("Unable to create temp directory ["
						+ baseDir.getPath() + "]");
			}
		}
		String baseName = System.currentTimeMillis() + "-";
		int tempDirAttempts = 50;
		File tempDir = null;
		for (int counter = 0; counter < tempDirAttempts; counter++) {
			String tempSubDir = (prefix != null ? prefix : "") + baseName
					+ counter + (suffix != null ? suffix : "")
					+ (subDir != null ? File.separator + subDir : "");
			tempDir = new File(baseDir, tempSubDir);
			if (tempDir.mkdirs()) {
				// Do not use deleteOnExit() even if you explicitly delete it later.
				// Google 'deleteonexit is evil' for more info, but the gist of the problem is:
				// 1.deleteOnExit() only deletes for normal JVM shutdowns, not crashes or killing the JVM process.
				// 2.deleteOnExit() only deletes on JVM shutdown - not good for long running server processes because:
				// 3.The most evil of all - deleteOnExit() consumes memory for each temp file entry. If your process is running for months, or creates a lot of temp files in a short time, you consume memory and never release it until the JVM shuts down
 				return tempDir;
			}
		}
		throw new IOException("Failed to create temp directory within ["
				+ tempDirAttempts + "] attempts (last attempt is [" + tempDir.getPath() + "])");
	}
	
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

	public static File[] getFiles(String directory, String wildcard, String excludeWildcard, long minStability) {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(directory);
		File[] files = dir.listFiles(filter);

		WildCardFilter excludeFilter = null;
		if (StringUtils.isNotEmpty(excludeWildcard)) {
			excludeFilter = new WildCardFilter(excludeWildcard);
		}
		
		long lastChangedAllowed=minStability>0?new Date().getTime()-minStability:0;
		
		List<File> result = new ArrayList<File>();
		int count = (files == null ? 0 : files.length);
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (!file.isFile()) {
				continue;
			}
			if (excludeFilter!=null && excludeFilter.accept(dir, file.getName())) {
				continue;
			}
			if (minStability>0 && file.lastModified()>lastChangedAllowed) {
				continue;
			}
			result.add(file);
		}
		Collections.sort(result, new FileComparator());
		return result.toArray(new File[0]);
	}

	public static File getFirstFile(File directory) {
		String[] fileNames = directory.list();

		for (int i = 0; i < fileNames.length; i++) {
			File file = new File(directory, fileNames[i]);
			if (file.isFile()) {
				return file;
			}
		}
		return null;
	}
	public static File getFirstFile(String directory, long minStability) {
		File dir = new File(directory);
		String[] fileNames = dir.list();

		long lastChangedAllowed=minStability>0?new Date().getTime()-minStability:0;

		for (int i = 0; i < fileNames.length; i++) {
			File file = new File(directory, fileNames[i]);
			if (file.isFile()) {
				if (minStability>0 && file.lastModified()<=lastChangedAllowed) {
					return file;
				}
			}
		}
		return null;
	}

	public static List<String> getListFromNames(String names, char seperator) {
		StringTokenizer st = new StringTokenizer(names, "" + seperator);
		LinkedList<String> list = new LinkedList<String>();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}

	public static List<String> getListFromNames(String[] names) {
		if (names == null) {
			return null;
		}
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
	
	public static String getNamesFromList(List<String> filenames, char seperator) {
		if (filenames == null)
			return "";
			
		StringBuffer result = new StringBuffer();
		for (Iterator<String> nameIterator = filenames.iterator(); nameIterator.hasNext();) {
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
		} else if (val.length() == length) {
			result.append(val);
		} else {
			char[] fill = getFilledArray(length - val.length(), fillchar);
			if (leftAlign) {
				result.append(val).append(fill);			
			} else {
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
		} 
		idx++;
		if (idx >= fileName.length()) {
			return null;
		} 
		return fileName.substring(idx);
	}

	public static String getBaseName(String fileName) {
		File file = new File(fileName);
		String fname = file.getName();
		int idx = fname.lastIndexOf('.');
		if (idx<0) {
			return null;
		} 
		return fname.substring(0, idx);
	}

	public static boolean extensionEqualsIgnoreCase(String fileName, String extension) {
		String fileNameExtension = getFileNameExtension(fileName);
		if (fileNameExtension==null) {
			return false;
		} 
		return fileNameExtension.equalsIgnoreCase(extension);
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

	static public String encodeFileName(String fileName) {
		String mark = "-_.+=";
		StringBuffer encodedFileName = new StringBuffer();
		int len = fileName.length();
		for (int i = 0; i < len; i++) {
			char c = fileName.charAt(i);
			if ((c >= '0' && c <= '9')
				|| (c >= 'a' && c <= 'z')
				|| (c >= 'A' && c <= 'Z'))
				encodedFileName.append(c);
			else {
				int imark = mark.indexOf(c);
				if (imark >= 0) {
					encodedFileName.append(c);
				} else {
					encodedFileName.append('_');
				}
			}
		}
		return encodedFileName.toString();
	}

	public static void unzipStream(InputStream inputStream, File dir)
			throws IOException {
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
				inputStream));
		try {
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String filename = ze.getName();
				File zipFile = new File(dir, filename);
				if (ze.isDirectory()) {
					if (!zipFile.exists()) {
						log.debug("creating directory [" + zipFile.getPath()
								+ "] for ZipEntry [" + ze.getName() + "]");
						if (!zipFile.mkdir()) {
							throw new IOException(zipFile.getPath()
									+ " could not be created");
						}
					}
				} else {
					File zipParentFile = zipFile.getParentFile(); 
					if (!zipParentFile.exists()) {
						log.debug("creating directory [" + zipParentFile.getPath()
								+ "] for ZipEntry [" + ze.getName() + "]");
						if (!zipParentFile.mkdir()) {
							throw new IOException(zipParentFile.getPath()
									+ " could not be created");
						}
					}
					FileOutputStream fos = new FileOutputStream(zipFile);
					log.debug("writing ZipEntry [" + ze.getName()
							+ "] to file [" + zipFile.getPath() + "]");
					Misc.streamToStream(zis, fos, false);
					fos.close();
				}
			}
		} finally {
			try {
				zis.close();
			} catch (IOException e) {
				log.warn("exception closing unzip", e);
			}
		}
	}

	public static boolean readAllowed(String rules, HttpServletRequest request, String fileName) throws IOException {
		List<String> rulesList = Arrays.asList(rules.split("\\|"));
		for (String rule: rulesList) {
			List<String> parts = Arrays.asList(rule.trim().split("\\s+"));
			if (parts.size() != 3) {
				log.debug("invalid rule '" + rule + "' contains " + parts.size() + " part(s): " + parts);
			} else {
				String canonicalFileName = null;
				try {
					canonicalFileName = new File(fileName).getCanonicalPath();
				} catch(Exception e) {
					log.error("cannot determine canonical path for file name '" + fileName + "'", e);
				}
				String canonicalPath = null;
				if ("*".equals(parts.get(0))) {
					canonicalPath = parts.get(0);
				} else {
					try {
						canonicalPath = new File(parts.get(0)).getCanonicalPath();
					} catch(Exception e) {
						log.error("cannot determine canonical path for first part '" + parts.get(0) + "' of rule", e);
					}
				}
				if (canonicalFileName != null && canonicalPath != null) {
					String role = parts.get(1);
					String type = parts.get(2);
					log.trace("check allow read file '" + canonicalFileName + "' with rule path '" + canonicalPath + "', role '" + role + "' and type '" + type + "'");
					if ("*".equals(canonicalPath) || canonicalFileName.startsWith(canonicalPath)) {
						log.trace("path match");
						if ("*".equals(role) || request.isUserInRole(role)) {
							log.trace("role match");
							if ("allow".equals(type)) {
								log.trace("allow");
								return true;
							} else if ("deny".equals(type)) {
								log.trace("deny");
								return false;
							} else {
								log.error("invalid rule type");
							}
						}
					}
				}
			}
		}
		log.debug("deny");
		return false;
	}

	public static long getLastModifiedDelta(File file) {
		return System.currentTimeMillis() - file.lastModified();
	}

	public static boolean isFileBinaryEqual(File first, File second)
			throws IOException {
		boolean retval = false;

		if ((first.exists()) && (second.exists()) && (first.isFile())
				&& (second.isFile())) {
			if (first.getCanonicalPath().equals(second.getCanonicalPath())) {
				retval = true;
			} else {
				FileInputStream firstInput = null;
				FileInputStream secondInput = null;
				BufferedInputStream bufFirstInput = null;
				BufferedInputStream bufSecondInput = null;

				try {
					firstInput = new FileInputStream(first);
					secondInput = new FileInputStream(second);
					bufFirstInput = new BufferedInputStream(firstInput);
					bufSecondInput = new BufferedInputStream(secondInput);

					int firstByte;
					int secondByte;

					while (true) {
						firstByte = bufFirstInput.read();
						secondByte = bufSecondInput.read();
						if (firstByte != secondByte) {
							break;
						}
						if ((firstByte < 0) && (secondByte < 0)) {
							retval = true;
							break;
						}
					}
				} finally {
					try {
						if (bufFirstInput != null) {
							bufFirstInput.close();
						}
					} finally {
						if (bufSecondInput != null) {
							bufSecondInput.close();
						}
					}
				}
			}
		}
		return retval;
	}
}