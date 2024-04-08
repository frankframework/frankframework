/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;

import lombok.extern.log4j.Log4j2;


/**
 * Utilities for batch file handling.
 *
 * @author John Dekker
 */
@Log4j2
public class FileUtils {

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
			throw new ParameterException("file", e);
		}

		// resolve the parameters
		ParameterValueList pvl = pl.getValues(null, session);

		return pvl.get("__filename").getValue().toString();
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
			}
		} else {
			if (StringUtils.isNotEmpty(destDir)) {
				moveFile(orgFile, destDir, overwrite, numBackups);
			}
		}
	}

	protected static String moveFile(File orgFile, String destDir, boolean overwrite, int numBackups) throws InterruptedException, IOException {
		File dstFile = new File(destDir, orgFile.getName());
		return moveFile(orgFile, dstFile, overwrite, numBackups);
	}

	private static String moveFile(File orgFile, File rename2File, boolean overwrite, int numBackups) throws InterruptedException, IOException {
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

			if (success) {
				return destFile.getAbsolutePath();
			}

			Thread.sleep(waitTime);
		}
		return null;
	}

	public static boolean copyFile(File orgFile, File destFile, boolean append) {
		try {
			byte[] buf = new byte[StreamUtil.BUFFERSIZE];
			try (FileInputStream fis = new FileInputStream(orgFile); FileOutputStream fos = new FileOutputStream(destFile, append)) {
				int len;
				while ((len = fis.read(buf)) > 0) {
					fos.write(buf, 0, len);
				}
			}
			return true;
		} catch (IOException e) {
			log.warn("Could not copy file [{}] to [{}]", orgFile.getPath(), destFile.getPath(), e);
			return false;
		}
	}

	/**
	 * Creates a temporary file inside the ${ibis.tmpdir} using the default '.tmp' extension.
	 */
	public static File createTempFile() throws IOException {
		return createTempFile(null);
	}

	/**
	 * Creates a temporary file inside the ${ibis.tmpdir} using the specified extension.
	 */
	public static File createTempFile(final String extension) throws IOException {
		final File directory = new File( getTempDirectory() );
		final String suffix = StringUtils.isNotEmpty(extension) ? extension : ".tmp";
		final String prefix = "frank";
		log.debug("creating tempfile prefix [{}] suffix [{}] directory [{}]", prefix, suffix, directory);
		return File.createTempFile(prefix, suffix, directory);
	}

	/**
	 * If the ${ibis.tmpdir} is relative it will turn it into an absolute path
	 * @return The absolute path of ${ibis.tmpdir} or IOException if it cannot be resolved
	 */
	public static @Nonnull String getTempDirectory() {
		String directory = AppConstants.getInstance().getProperty("ibis.tmpdir");

		if (StringUtils.isNotEmpty(directory)) {
			File file = new File(directory);
			if (!file.isAbsolute()) {
				String absPath = new File("").getAbsolutePath();
				file = new File(absPath, directory);
			}
			if(!file.exists()) {
				file.mkdirs();
			}
			String fileDir = file.getPath();
			if(StringUtils.isEmpty(fileDir) || !file.isDirectory()) {
				throw new IllegalStateException("unknown or invalid path ["+(StringUtils.isEmpty(fileDir)?"NULL":fileDir)+"]");
			}
			directory = file.getAbsolutePath();
		}
		log.debug("resolved temp directory to [{}]", directory);

		//Directory may be NULL but not empty. The directory has to valid, available and the IBIS must have read+write access to it.
		if(StringUtils.isEmpty(directory)) {
			log.error("unable to determine ibis temp directory, falling back to [java.io.tmpdir]");
			return System.getProperty("java.io.tmpdir");
		}
		return directory;
	}

	/**
	 * @return the ${ibis.tmpdir}/folder or IOException if it cannot be resolved.
	 * If the ${ibis.tmpdir} is relative it will turn it into an absolute path
	 */
	public static File getTempDirectory(String folder) throws IOException {
		String tempDir = getTempDirectory();
		File newDir = new File(tempDir, folder);
		if (!newDir.exists() && !newDir.mkdirs()) {
			throw new IOException("unable to create temp directory [" + newDir.getPath() + "]");
		}
		return newDir;
	}

	/**
	 * Creates a new temporary directory in the specified 'fromDirectory'.
	 */
	public static File createTempDirectory(File fromDirectory) throws IOException {
		if (!fromDirectory.exists() || !fromDirectory.isDirectory()) {
			throw new IOException("base directory [" + fromDirectory.getPath() + "] must be a directory and must exist");
		}

		Path path = Files.createTempDirectory(fromDirectory.toPath(), "tmp");
		return path.toFile();
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
		// move current file to back up
		String backupFilename=targetFile.getPath()+".1";
		File backupFile=new File(backupFilename);
		targetFile.renameTo(backupFile);
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

		List<File> result = new ArrayList<>();
		int count = files == null ? 0 : files.length;
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
		result.sort(new FileComparator());
		return result.toArray(new File[0]);
	}

	@Nullable
	public static File getFirstFile(File directory) {
		String[] fileNames = directory.list();
		if (fileNames == null) {
			return null;
		}
		for (String fileName : fileNames) {
			File file = new File(directory, fileName);
			if (file.isFile()) {
				return file;
			}
		}
		return null;
	}
	public static File getFirstFile(String directory, long minStability) {
		File dir = new File(directory);
		String[] fileNames = dir.list();
		if (fileNames == null) {
			return null;
		}

		long lastChangedAllowed = minStability > 0 ? System.currentTimeMillis() - minStability : 0;

		for (String fileName : fileNames) {
			File file = new File(directory, fileName);
			if (file.isFile() && (minStability > 0 && file.lastModified() <= lastChangedAllowed)) {
					return file;
			}
		}
		return null;
	}

	public static List<String> getListFromNames(String names, char seperator) {
		StringTokenizer st = new StringTokenizer(names, "" + seperator);
		LinkedList<String> list = new LinkedList<>();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}

	public static List<String> getListFromNames(String[] names) {
		if (names == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(names);
	}

	public static String getNamesFromArray(@Nonnull String[] names, char separator) {
		StringBuilder result = new StringBuilder();
		for (String name : names) {
			if (result.length() > 0)
				result.append(separator);
			result.append(name);
		}
		return result.toString();
	}

	public static String getNamesFromList(List<String> filenames, char separator) {
		if (filenames == null)
			return "";

		StringBuilder result = new StringBuilder();
		for (String name : filenames) {
			if (result.length() > 0)
				result.append(separator);
			result.append(name);
		}
		return result.toString();
	}

	/*
	 * methods to create a fixed length string from a value
	 */
	public static String align(String val, int length, boolean leftAlign, char fillchar) {
		StringBuilder result = new StringBuilder();
		align(result, val, length, leftAlign, fillchar);
		return result.toString();
	}

	public static void align(StringBuilder result, String val, int length, boolean leftAlign, char fillchar) {
		if (val.length() > length) {
			result.append(val, 0, length);
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

	/**
	 * create a filled array
	 */
	public static char[] getFilledArray(int length, char fillChar) {
		char[] fill = new char[length];
		Arrays.fill(fill, fillChar);
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
				Files.delete(tmpFile.toPath());
			} catch (Exception t) {
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

	public static String encodeFileName(String fileName) {
		String mark = "-_.+=";
		StringBuilder encodedFileName = new StringBuilder();
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

	public static void unzipStream(InputStream inputStream, File dir) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String filename = ze.getName();
				File zipFile = new File(dir, filename);
				if (ze.isDirectory()) {
					if (!zipFile.exists()) {
						log.debug("creating directory [" + zipFile.getPath() + "] for ZipEntry [" + ze.getName() + "]");
						if (!zipFile.mkdir()) {
							throw new IOException(zipFile.getPath() + " could not be created");
						}
					}
				} else {
					File zipParentFile = zipFile.getParentFile();
					if (!zipParentFile.exists()) {
						log.debug("creating directory [" + zipParentFile.getPath() + "] for ZipEntry [" + ze.getName() + "]");
						if (!zipParentFile.mkdir()) {
							throw new IOException(zipParentFile.getPath() + " could not be created");
						}
					}
					try (FileOutputStream fos = new FileOutputStream(zipFile)) {
						log.debug("writing ZipEntry [" + ze.getName() + "] to file [" + zipFile.getPath() + "]");
						StreamUtil.streamToStream(StreamUtil.dontClose(zis), fos);
					}
				}
			}
		}
	}

	public static boolean readAllowed(String rules, HttpServletRequest request, String fileName) {
		return readAllowed(rules, fileName, request::isUserInRole);
	}

	@FunctionalInterface
	public interface Authenticator {
		boolean isUserInRole(String role);
	}

	public static boolean readAllowed(String rules, String fileName, Authenticator authenticator) {
		String[] rulesList = rules.split("\\|");
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
						if ("*".equals(role) || authenticator.isUserInRole(role)) {
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

		if ((!first.exists()) || (!second.exists()) || (!first.isFile())
				|| (!second.isFile())) {
			return false;
		}
		if (first.length() != second.length()) {
			return false;
		}
		if (first.getCanonicalPath().equals(second.getCanonicalPath())) {
			return true;
		}

		try (InputStream bufFirstInput = new BufferedInputStream(new FileInputStream(first));
			 InputStream bufSecondInput = new BufferedInputStream(new FileInputStream(second))) {

			boolean retval;
			while (true) {
				int firstByte = bufFirstInput.read();
				int secondByte = bufSecondInput.read();
				if (firstByte != secondByte) {
					retval = false;
					break;
				}
				// End of file, must be end of both files.
				if (firstByte < 0) {
					retval = true;
					break;
				}
			}
			return retval;
		}
	}
}
