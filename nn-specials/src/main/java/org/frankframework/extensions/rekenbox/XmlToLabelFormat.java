/*
   Copyright 2002, 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.extensions.rekenbox;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * <P>
 * Convert an XML DOM document to the flat label-format of the rekenbox.
 * <P>
 * Input must be of type <code>org.w3c.dom.Element</code> or
 * <code>org.w3c.dom.Document</code>; output will be of type
 * <code>java.lang.String</code>.
 *
 * @author leeuwt
 *
 *         Change History Author Date Version Details Tim N. van der Leeuw
 *         30-07-2002 1.0 Initial release Tim N. van der Leeuw 14-08-2002 1.1
 *         Use base-class AbstractTranformer.
 *
 */
public class XmlToLabelFormat {

	/**
	 * Method makeTagLabel. Makes a label for the rekenbox from the tag-name and
	 * optional volgnummer-attribute of the element.
	 *
	 * <P>
	 *
	 * @param parentLabel
	 *                    <P>
	 *                    Label of the parent-tags. This is prefixed to the label of
	 *                    this tag.
	 * @param el
	 *                    <P>
	 *                    Element of which the label needs to be constructed.
	 *
	 * @return String
	 *         <P>
	 *         The constructed label.
	 *
	 */
	static String makeTagLabel(String parentLabel, Element el) {
		StringBuilder tag = new StringBuilder(60);

		if(!parentLabel.isEmpty()) {
			tag.append(parentLabel).append(".");
		}
		tag.append(el.getTagName()).append(el.getAttribute("volgnummer"));
		return tag.toString();
	}

	static Collection getElementChildren(Element el) {
		Collection c;
		NodeList nl;
		int len;

		c = new LinkedList();
		nl = el.getChildNodes();
		len = nl.getLength();

		for(int i = 0; i < len; i++) {
			Node n = nl.item(i);
			if(n instanceof Element) {
				c.add(n);
			}
		}

		return c;
	}

	static StringBuilder getTextValue(Element el) {
		StringBuilder sb = new StringBuilder(1024);
		NodeList nl = el.getChildNodes();
		for(int i = 0; i < nl.getLength(); ++i) {
			Node n = nl.item(i);
			if(n instanceof Text) {
				sb.append(n.getNodeValue());
			}
		}
		return sb;
	}

	static void convertTagsToLabels(StringBuilder buf, String parentLabel, Collection elements) {
		Collection children;
		String tagLabel;

		for(Iterator i = elements.iterator(); i.hasNext();) {
			Element el = (Element) i.next();

			tagLabel = makeTagLabel(parentLabel, el);
			children = getElementChildren(el);

			if(!children.isEmpty()) {
				buf.append(tagLabel).append(" : #SAMENGESTELD\n");
				convertTagsToLabels(buf, tagLabel, children);
			} else {
				StringBuilder text = getTextValue(el);
				if(text != null && !text.isEmpty()) {
					buf.append(tagLabel).append(" :").append(text).append("\n"); // JDK1.4 needs no converstion text.toString()
				}
			}
		}
	}

	/**
	 * Convert XML DOM document to flat string label-format of the rekenbox. Input
	 * must be of type <code>org.w3c.dom.Element</code> or
	 * <code>org.w3c.dom.Document</code>; output will be of type
	 * <code>java.lang.String</code>.
	 *
	 */
	public static String doTransformation(/* Message message, Map scratchpad, */ Object data) {
		Document doc;
		Element el;
		StringBuilder buf;
		Collection c;

		buf = new StringBuilder(10 * 1024);
		if(data instanceof Document document) {
			doc = document;
			el = doc.getDocumentElement();
		} else if(data instanceof Element element) {
			el = element;
		} else {
			throw new IllegalStateException("Input not of type Document or Element, but of type " + data.getClass());
		}
		c = getElementChildren(el);
		convertTagsToLabels(buf, "", c);

		return buf.toString();
	}
}
