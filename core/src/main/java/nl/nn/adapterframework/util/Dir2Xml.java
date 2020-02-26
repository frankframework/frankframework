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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

/**
 * List the contents of a directory as XML.
 * 
 * @author Johan Verrips IOS
 */
public class Dir2Xml  {
	protected static Logger log = LogUtil.getLogger(Dir2Xml.class);
	
  	private String path;
  	private String wildcard="*.*";
  	
	public String getDirList() {
		return getDirList(false);
	}
	  
	public String getDirList(boolean includeDirectories) {
		return getDirList(includeDirectories, -1);
	}
	  
	public String getDirList(boolean includeDirectories, int maxItems) {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(path);
		File files[] = dir.listFiles(filter);
		if (files != null) {
			Arrays.sort(files, new FileNameComparator());
		}
		int count = (files == null ? 0 : files.length);
		XmlBuilder dirXml = new XmlBuilder("directory");
		dirXml.addAttribute("name", path);

		if (includeDirectories) {
			File parent = dir.getParentFile();
			if (parent != null) {
				dirXml.addSubElement(getFileAsXmlBuilder(parent,".."));
			}
		}

		int numberOfDirectories = 0;
		int loopCount = count;
		if (maxItems >= 0 && count > maxItems) {
			loopCount = maxItems;
		}
		for (int i = 0; i < loopCount; i++) {
			File file = files[i];
			if (file.isDirectory() && !includeDirectories) {
				numberOfDirectories++;
				continue;
			}
			dirXml.addSubElement(getFileAsXmlBuilder(file,file.getName()));
		}

		// TODO: implement includeDirectories on WildCardFilter
		if (includeDirectories && !"*.*".equals(wildcard)) {
			dirXml.addAttribute("count", count);
		} else {
			dirXml.addAttribute("count", count - numberOfDirectories);
		}
				
		return dirXml.toXML();
	}

	public String getRecursiveDirList() {
		File dir = new File(path);
		Collection<File> filesCol = FileUtils.listFiles(dir, null, true);
		File files[] = filesCol.toArray(new File[filesCol.size()]);
		if (files != null) {
			Arrays.sort(files, new FileNameComparator());
		}
		int count = (files == null ? 0 : files.length);
		XmlBuilder dirXml = new XmlBuilder("directory");
		dirXml.addAttribute("name", path);
		dirXml.addAttribute("count", count);
		for (int i = 0; i < count; i++) {
			File file = files[i];
			dirXml.addSubElement(getFileAsXmlBuilder(file,file.getName()));
		}
		return dirXml.toXML();
	}

	public static XmlBuilder getFileAsXmlBuilder(File file, String nameShown) {
	
		XmlBuilder fileXml = new XmlBuilder("file");
		fileXml.addAttribute("name", nameShown);
		long fileSize = file.length();
		fileXml.addAttribute("size", "" + fileSize);
		fileXml.addAttribute("fSize", "" + Misc.toFileSize(fileSize,true));
		fileXml.addAttribute("directory", "" + file.isDirectory());
		try {
			fileXml.addAttribute("canonicalName", file.getCanonicalPath());
		} catch (IOException e) {
			log.warn("cannot get canonicalName for file ["+nameShown+"]",e);
			fileXml.addAttribute("canonicalName", nameShown);
		}
		// Get the modification date of the file
		Date modificationDate = new Date(file.lastModified());
		//add date
		String date = DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
		fileXml.addAttribute("modificationDate", date);
	
		// add the time
		String time = DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
		fileXml.addAttribute("modificationTime", time);
	
		return fileXml;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * Set a Wildcard
	 * @see WildCardFilter
	 */
	public void setWildCard(String wildcard) {
		this.wildcard = wildcard;
	
	}
}
