/*
 * $Log: Dir2Xml.java,v $
 * Revision 1.7  2006-01-05 14:52:02  europe\L190409
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
import java.util.Date;
import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * List the contents of a directory as XML.
 * 
 * @author Johan Verrips IOS
 * @version Id
 */
public class Dir2Xml  {
	public static final String version="$RCSfile: Dir2Xml.java,v $ $Revision: 1.7 $ $Date: 2006-01-05 14:52:02 $";
	protected Logger log = Logger.getLogger(this.getClass());
	
  	private String path;
  	private String wildcard="*.*";
  	
	public String getDirList() {
		return getDirList(false);
	}
	  
	public String getDirList(boolean includeDirectories) {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(path);
		File files[] = dir.listFiles(filter);
		Arrays.sort(files, new FileNameComparator());
		int count = (files == null ? 0 : files.length);
		XmlBuilder dirXml = new XmlBuilder("directory");
		dirXml.addAttribute("name", path);
		if (includeDirectories) {
			dirXml.addSubElement(getFileAsXmlBuilder(dir.getParentFile(),".."));
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
