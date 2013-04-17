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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
/**
 * Builds a XML-element with attributes and sub-elements. 
 * In fact it represents an XML element. Attributes can be added
 * with the addAttribute method, the content can be set with the setValue method.
 * Subelements can be added with the addSubElement method.
 * the toXML function returns the node and subnodes as an indented xml string.
 * @version $Id$
 * @author Johan Verrips
 **/
public class XmlBuilder {
	public static final String version = "$RCSfile: XmlBuilder.java,v $ $Revision: 1.12 $ $Date: 2011-11-30 13:51:48 $";

	private List attributeNames = new ArrayList();
	private Hashtable attributes = new Hashtable();

	private String value;
	private Vector subElements = new Vector();
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

	public XmlBuilder() {
	}
	public XmlBuilder(String tagName) {
		this.setTagName(tagName);
	}
	/**
	 * adds an attribute with an attribute value to the list of attributes
	 **/
	public void addAttribute(String name, String value) {
		if (value != null) {
			attributeNames.add(name);
			attributes.put(name, XmlUtils.encodeChars(value));
		}
	}
	public void addAttribute(String name, boolean value) {
		addAttribute(name,""+value);
	}
	public void addAttribute(String name, long value) {
		addAttribute(name,""+value);
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
	  * sets the content of the element as CDATA <br>
	  * <code>setCdataValue(&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;)</code> sets
	  * <code><pre> &lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;</pre></code>
	  **/
	public void setCdataValue(String value) {
		if (value != null)
			this.value = "<![CDATA[" + value + "]]>";
		else
			this.value = value;

	}
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	/**
	 * sets the content of the element
	 **/
	public void setValue(String value) {
		setValue(value, true);
	}
	public void setValue(String value, boolean encode) {
		if (value != null && encode)
			this.value = XmlUtils.encodeChars(value);
		else
			this.value = value;
	}

	/**
	 * returns the xmlelement and all subElements as an xml string.
	 */
	public String toXML() {
		return toXML(0);
	}

	private String toXML(int indentlevel) {
		String attributeName;

		StringBuffer sb = new StringBuffer();

		// indent
		for (int t = 0; t < indentlevel; t++) {
			sb.append(INDENT);
		}
		//construct the tag
		sb.append(OPEN_START);
		sb.append(this.tagName);

		//process attributes
		Iterator i = attributeNames.iterator();
		while (i.hasNext()) {
			attributeName = (String)i.next();
			sb.append(" " + attributeName);
			sb.append("=");
			sb.append(QUOTE + (String)attributes.get(attributeName) + QUOTE);
		}

		if ((this.value == null) && (subElements.size() == 0)) {
			sb.append(SIMPLE_CLOSE);
			return sb.toString();
		}
		sb.append(CLOSE);

		boolean pendingTextValue = false;
		// put the tag value
		if (null != this.value) {
			//      sb.append(NEWLINE);
			//      for (int t=0; t<indentlevel;t++){ sb.append(INDENT);}
			sb.append(this.value);
			pendingTextValue = true;
		}

		//process subelements
		Iterator it = subElements.iterator();
		while (it.hasNext()) {
			XmlBuilder sub = (XmlBuilder)it.next();
			indentlevel = indentlevel + 1;
			if (pendingTextValue) {
				pendingTextValue = false;
			} else {
				sb.append(NEWLINE);
			}
			sb.append(sub.toXML(indentlevel));
			indentlevel = indentlevel - 1;
		}
		// indent
		if (pendingTextValue) {
			pendingTextValue = false;
		} else {
			sb.append(NEWLINE);
			for (int t = 0; t < indentlevel; t++) {
				sb.append(INDENT);
			}
		}
		sb.append(OPEN_END + tagName + CLOSE);

		return sb.toString();
	}

	public String toXML(boolean xmlHeader) {
		if (xmlHeader)
			return "<?xml version=\"1.0\"?>" + NEWLINE + toXML();
		else
			return toXML();
	}
  
}
