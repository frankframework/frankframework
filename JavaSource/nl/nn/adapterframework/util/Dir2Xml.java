package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;
/**
 * List the contents of a directory as XML.
 * <p>$Id: Dir2Xml.java,v 1.2 2004-02-04 10:02:02 a1909356#db2admin Exp $</p>
 *
 * @author Johan Verrips IOS
 */
public class Dir2Xml  {
	public static final String version="$Id: Dir2Xml.java,v 1.2 2004-02-04 10:02:02 a1909356#db2admin Exp $";
	
  String path;
  String wildcard="*.*";
  public Dir2Xml() {
	  super();
  }
  public String getDirList() {
    WildCardFilter filter = new WildCardFilter(wildcard);
    File dir = new File(path);
    File files[] = dir.listFiles(filter);
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
  public static void main(String[] argv) {
    Dir2Xml dx=new Dir2Xml();
    dx.setPath("c:/temp");
    dx.setWildCard("log*.txt*");
    System.out.println(dx.getDirList());
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
