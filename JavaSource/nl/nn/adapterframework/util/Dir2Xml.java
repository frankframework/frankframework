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
/*
 * $Log: Dir2Xml.java,v $
 * Revision 1.11  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.9  2011/02/04 15:35:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * avoid NPE
 *
 * Revision 1.8  2007/02/12 14:09:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.7  2006/01/05 14:52:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improve error handling
 *
 * Revision 1.6  2005/10/20 15:20:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added feature to include directories
 *
 * Revision 1.5  2005/07/19 11:04:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use explicit FileNameComparator
 *
 * Revision 1.4  2005/05/31 09:20:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added sort for files
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * List the contents of a directory as XML.
 * 
 * @author Johan Verrips IOS
 * @version $Id$
 */
public class Dir2Xml  {
	public static final String version="$RCSfile: Dir2Xml.java,v $ $Revision: 1.11 $ $Date: 2011-11-30 13:51:48 $";
	protected Logger log = LogUtil.getLogger(this);
	
  	private String path;
  	private String wildcard="*.*";
  	
	public String getDirList() {
		return getDirList(false);
	}
	  
	public String getDirList(boolean includeDirectories) {
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
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory() && !includeDirectories) {
				continue;
			}
			dirXml.addSubElement(getFileAsXmlBuilder(file,file.getName()));
		}
		return dirXml.toXML();
	}
  
	private XmlBuilder getFileAsXmlBuilder(File file, String nameShown) {
	
		XmlBuilder fileXml = new XmlBuilder("file");
		fileXml.addAttribute("name", nameShown);
		fileXml.addAttribute("size", "" + file.length());
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
