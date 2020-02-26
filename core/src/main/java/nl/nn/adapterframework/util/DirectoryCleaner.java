/*
   Copyright 2015 Nationale-Nederlanden

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

import java.io.File;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * Cleans up a directory.
 *
 * 
 * @author Peter Leeuwenburgh
 */
public class DirectoryCleaner {
	protected Logger log = LogUtil.getLogger(this);

	private String directory;
	private String retention = "30d";
	private boolean subdirectories = false;
	private boolean deleteEmptySubdirectories = false;
	private boolean notExistWarn = true;

	public void cleanup() {
		if (StringUtils.isNotEmpty(getDirectory())) {
			log.debug("Cleanup directory [" + getDirectory() + "]");
			File dir = new File(getDirectory());
			if (dir.exists() && dir.isDirectory()) {
				long lastModifiedDelta = Misc.parseAge(getRetention(), -1);
				if (lastModifiedDelta < 0) {
					log.error("retention [" + getRetention()
							+ "] could not be parsed, cleaning up directory ["
							+ getDirectory() + "] is skipped");
				} else {
					File[] files = dir.listFiles();
					if (files != null) {
						for (int i = 0; i < files.length; i++) {
							File file = files[i];
							cleanupFile(file, lastModifiedDelta);
						}
					}
				}
			} else {
				if (isNotExistWarn()) {
					log.warn("directory [" + getDirectory()
							+ "] does not exists or is not a directory");
				}
			}
		}
	}

	private void cleanupFile(File file, long lastModifiedDelta) {
		if (file.isDirectory()) {
			if (subdirectories) {
				log.debug("Cleanup subdirectory [" + file.getAbsolutePath()
						+ "]");
				File[] files = file.listFiles();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						File file2 = files[i];
						cleanupFile(file2, lastModifiedDelta);
					}
				}
			}
			if (deleteEmptySubdirectories) {
				if (file.list().length == 0) {
					if (file.delete()) {
						log.info("deleted empty subdirectory ["
								+ file.getAbsolutePath() + "]");
					} else {
						log.warn("could not delete empty subdirectory ["
								+ file.getAbsolutePath() + "]");
					}
				}
			}
		} else {
			if (file.isFile()) {
				if (FileUtils.getLastModifiedDelta(file) > lastModifiedDelta) {
					String fileStr = "file [" + file.getAbsolutePath()
							+ "] with age [" + Misc.getAge(file.lastModified())
							+ "]";
					if (file.delete()) {
						log.info("deleted " + fileStr);
					} else {
						log.warn("could not delete file " + fileStr);
					}
				}
			}
		}
	}

	@IbisDoc({"directory to be cleaned up", ""})
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getDirectory() {
		return directory;
	}

	@IbisDoc({"time (with suffix 'd', 'h', 'm' or 's' in milliseconds) that must have passed at least before a file will be deleted", "30d"})
	public void setRetention(String retention) {
		this.retention = retention;
	}

	public String getRetention() {
		return retention;
	}

	@IbisDoc({"when <code>true</code>, files in subdirectories will be deleted, too", "false"})
	public void setSubdirectories(boolean b) {
		subdirectories = b;
	}

	public boolean isSubdirectories() {
		return subdirectories;
	}

	@IbisDoc({"when <code>true</code>, empty subdirectories will be deleted, too", "false"})
	public void setDeleteEmptySubdirectories(boolean b) {
		deleteEmptySubdirectories = b;
	}

	public boolean isDeleteEmptySubdirectories() {
		return deleteEmptySubdirectories;
	}

	@IbisDoc({"when set <code>true</code>, send warnings to logging and console about not existing directories", "true"})
	public void setNotExistWarn(boolean b) {
		notExistWarn = b;
	}

	public boolean isNotExistWarn() {
		return notExistWarn;
	}
}
