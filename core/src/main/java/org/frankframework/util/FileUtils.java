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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;


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
	private static String getFilename(ParameterList definedParameters, PipeLineSession session, String originalFilename, String filenamePattern) throws ParameterException {
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
				log.debug("Could not move file [{}] to [{}], now trying alternate move (copy and delete)", orgFile.getPath(), rename2File.getPath());
				success = copyFile(orgFile, rename2File, false);
				if (success) {
					success = orgFile.delete();
					if (!success) {
						log.debug("Could not delete source file [{}] after copying it to [{}]", orgFile.getPath(), rename2File.getPath());
						if (!rename2FileExists) {
							log.debug("Deleting destination file [{}]: {}", rename2File.getPath(), rename2File.delete());
						}
					}
				} else {
					log.debug("Could not copy file in alternate move");
				}
			}

			if (!success) {
				log.debug("Retries left for moving file [{}]", numberOfAttempts - errCount);
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
		if (!file.exists()) {
			return file;
		}
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

	protected static void makeBackups(File targetFile, int numBackups)  {
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

	@FunctionalInterface
	public interface Authenticator {
		boolean isUserInRole(String role);
	}

	public static boolean readAllowed(String rules, String fileName, Authenticator authenticator) {
		String[] rulesList = rules.split("\\|");
		for (String rule: rulesList) {
			List<String> parts = Arrays.asList(rule.trim().split("\\s+"));
			if (parts.size() != 3) {
				log.debug("invalid rule '{}' contains {} part(s): {}", rule, parts.size(), parts);
			} else {
				String canonicalFileName = null;
				try {
					canonicalFileName = new File(fileName).getCanonicalPath();
				} catch(Exception e) {
					log.error("cannot determine canonical path for file name '{}'", fileName, e);
				}
				String canonicalPath = null;
				if ("*".equals(parts.get(0))) {
					canonicalPath = parts.get(0);
				} else {
					try {
						canonicalPath = new File(parts.get(0)).getCanonicalPath();
					} catch(Exception e) {
						log.error("cannot determine canonical path for first part '{}' of rule", parts.get(0), e);
					}
				}
				if (canonicalFileName != null && canonicalPath != null) {
					String role = parts.get(1);
					String type = parts.get(2);
					log.trace("check allow read file '{}' with rule path '{}', role '{}' and type '{}'", canonicalFileName, canonicalPath, role, type);
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

}
