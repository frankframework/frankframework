/*
 * $Log: XmlBuilder.java,v $
 * Revision 1.7  2006-11-13 15:43:15  europe\L190409
 * corrected version string
 *
 */
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
/**
 * Builds a XML-element with attributes and sub-elements. 
 * In fact it represents an XML element. Attributes can be added
 * with the addAttribute method, the content can be set with the setValue method.
 * Subelements can be added with the addSubElement method.
 * the toXML function returns the node and subnodes as an indented xml string.
 * @version Id
 * @author Johan Verrips
 **/
public class XmlBuilder {
	public static final String version = "$RCSfile: XmlBuilder.java,v $ $Revision: 1.7 $ $Date: 2006-11-13 15:43:15 $";
	
  private ArrayList attributeNames = new ArrayList();;
  private Hashtable attributes = new Hashtable();;
  private String value;
  private Vector subElements=new Vector();
  private String tagName;

  /**
   * &lt; sign
   */
  public final static String OPEN_START = "<";
  /**
   * /&lt; sign
   */
  public final static String SIMPLE_CLOSE = "/>";

  /**
   * &gt;/ sign
   */
  public final static String OPEN_END = "</";
  /**
   * /&gt; sign
   */
  public final static String CLOSE = ">";
  /**
   * a new line constant
   */
  public final static String NEWLINE = "\n";
  /**
   * the tab constant
   */
  public final static String INDENT = "\t";
  /**
   * a quote like &quote;
   */
  public final static String QUOTE = "\"";

  protected static int indentlevel; //level of indentation

  public XmlBuilder() {}
  public XmlBuilder(String tagName){
    this.setTagName(tagName);
  }
  /**
   * adds an attribute with an attribute value to the list of attributes
   **/
  public void addAttribute(String name, String value) {
	if (value!=null) {
	    attributeNames.add(name);
	    attributes.put(name, XmlUtils.encodeChars(value));
	}
  }
/**
 * adds an XmlBuilder element to the list of subelements
 */
public void addSubElement(XmlBuilder newElement) {
    if (newElement != null) {
        subElements.add(newElement);
    }
}
  public String getTagName() {
    return this.tagName;
  }
  /**
   * for testing purposes.
   */
  public static void main(String args[]) {
      XmlBuilder xl=new XmlBuilder("TestTag");
      xl.addAttribute("att1name",  "att1value");
      xl.addAttribute("att2name",  "att2value");
      xl.setValue("dit is de value");
      XmlBuilder sb1=new XmlBuilder("sub1");
      sb1.addAttribute("sb1at1", "sb1at1value");
      //add to root element
      xl.addSubElement(sb1);
      XmlBuilder sb2=new XmlBuilder("sub2");
      sb2.addAttribute("sb2at1", "sb2at1value");
      sb2.setValue("dit is een value voor sub2");
      sb1.addSubElement(sb2);
      XmlBuilder sb3=new XmlBuilder("sub3");
      sb3.addAttribute("sb3at1", "sb3\"+>at1value");
      sb3.setValue("dit is een value voor sub2");
      sb2.addSubElement(sb3);
      XmlBuilder sb4=new XmlBuilder("sub4");
      xl.addSubElement(sb4);

      System.out.println(xl.toXML());
  }
 /**
   * sets the content of the element as CDATA <br>
   * <code>setCdataValue(&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;)</code> sets
   * <code><pre> &lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;</pre></code>
   **/
  public void setCdataValue (String value) {
	if (value!=null) 
	    this.value="<![CDATA["+value+"]]>";
	else this.value=value;
	
  }
  public void setTagName (String tagName) {
    this.tagName=tagName;
  }
  /**
   * sets the content of the element
   **/
  public void setValue (String value) {
		setValue(value, true);
  }
  public void setValue (String value, boolean encode) {
	if (value!=null && encode)
		this.value=XmlUtils.encodeChars(value);
	else this.value=value;
  }
  /**
   * returns the xmlelement and all subElements as an xml string.
   */
  public String toXML() {
    String attributeName;
 
    StringBuffer sb = new StringBuffer();

    // indent
    for (int t=0; t<indentlevel;t++){ sb.append(INDENT);}
    //construct the tag
    sb.append (OPEN_START);
    sb.append (this.tagName);

    //process attributes
    Iterator i = attributeNames.iterator();
    while (i.hasNext()) {
        attributeName = (String) i.next();
        sb.append(" "+attributeName);
        sb.append("=");
        sb.append(QUOTE+(String) attributes.get(attributeName)+QUOTE);
    }

    if ((this.value==null) && (subElements.size()==0)) {
      sb.append(SIMPLE_CLOSE);
      return sb.toString();
    }
    sb.append (CLOSE);

    boolean pendingTextValue=false;
    // put the tag value
    if (null!=this.value) {
//      sb.append(NEWLINE);
//      for (int t=0; t<indentlevel;t++){ sb.append(INDENT);}
      sb.append(this.value);
      pendingTextValue=true;
    }

    //process subelements
    Iterator it=subElements.iterator();
    while (it.hasNext()) {
        XmlBuilder sub=(XmlBuilder) it.next();
        indentlevel=indentlevel+1;
        if (pendingTextValue) {
        	pendingTextValue=false;
        } else {
        	sb.append(NEWLINE);
        }
        sb.append(sub.toXML());
        indentlevel=indentlevel-1;
    }
    // indent
    if (pendingTextValue) {
      	pendingTextValue=false;
    } else {
       	sb.append(NEWLINE);
	    for (int t=0; t<indentlevel;t++){ sb.append(INDENT);}
    }
    sb.append(OPEN_END + tagName + CLOSE);

    return sb.toString();
  }

  public String toXML(boolean xmlHeader) {
  	if (xmlHeader) return  "<?xml version=\"1.0\"?>"+NEWLINE+toXML();
  	else return toXML();
  }
  
}
