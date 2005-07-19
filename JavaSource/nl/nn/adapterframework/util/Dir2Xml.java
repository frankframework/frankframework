/*
 * $Log: Dir2Xml.java,v $
 * Revision 1.5  2005-07-19 11:04:09  europe\L190409
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
/**
 * List the contents of a directory as XML.
 * @version Id
 *
 * @author Johan Verrips IOS
 */
public class Dir2Xml  {
	public static final String version="$RCSfile: Dir2Xml.java,v $ $Revision: 1.5 $ $Date: 2005-07-19 11:04:09 $";
	
  	private String path;
  	private String wildcard="*.*";
  	
	public Dir2Xml() {
		super();
	}
	  
  public String getDirList() {
    WildCardFilter filter = new WildCardFilter(wildcard);
    File dir = new File(path);
    File files[] = dir.listFiles(filter);
    Arrays.sort(files, new FileNameComparator());
    int count=(files==null ? 0 : files.length);
    XmlBuilder dirXml=new XmlBuilder("directory");
    dirXml.addAttribute("name", path);
    for (int i=0; i<count; i++ ) {
	    File file=files[i];
	    if (file.isDirectory()) {
		    continue;
	    }
	    dirXml.addSubElement(getFileAsXmlBuilder(file));
    }
    return dirXml.toXML();
  }
  
  private XmlBuilder getFileAsXmlBuilder(File file){
	  
	  XmlBuilder fileXml=new XmlBuilder("file");
	  fileXml.addAttribute("name", file.getName());
	  fileXml.addAttribute("size", ""+file.length());
	  try {
	  	fileXml.addAttribute("canonicalName", file.getCanonicalPath());
	  } catch (IOException e) { }
      // Get the modification date of the file
      Date modificationDate=new Date(file.lastModified());
	  //add date
	  String date=DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
	  fileXml.addAttribute("modificationDate", date);

      // add the time
      String time=DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
      fileXml.addAttribute("modificationTime", time);
      
	  return fileXml;
  }

  public void setPath(String path) {
    this.path=path;
  }
  
  /**
   * Set a Wildcard
   * @see WildCardFilter
   */
  public void setWildCard(String wildcard) {
    this.wildcard=wildcard;

  }
}
