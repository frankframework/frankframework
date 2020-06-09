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
package nl.nn.adapterframework.extensions.rekenbox;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.*;

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
 * Change History
 * Author                   Date        Version     Details
 * Tim N. van der Leeuw     30-07-2002  1.0         Initial release
 * Tim N. van der Leeuw     14-08-2002  1.1         Use base-class AbstractTranformer.
 * 
 */
public class XmlToLabelFormat /*extends AbstractTransformer*/
{

    /**
     * Method makeTagLabel.
     * Makes a label for the rekenbox from the tag-name and optional
     * volgnummer-attribute of the element.
     * 
     * <P>
     * @param parentLabel
     * <P>
     * Label of the parent-tags. This is prefixed to the label of this tag.
     * @param el
     * <P>
     * Element of which the label needs to be constructed.
     * 
     * @return String
     * <P>
     * The constructed label.
     * 
     */
    static String makeTagLabel(String parentLabel, Element el)
    {
        StringBuffer    tag = new StringBuffer(60);
        
        
        if (parentLabel.length() > 0)
        {
            tag.append(parentLabel).append(".");
        }
        tag.append(el.getTagName()).append(el.getAttribute("volgnummer"));
        return tag.toString();
    }
    
    static Collection getElementChildren(Element el)
    {
        Collection  c;
        NodeList    nl;
        int         len;
        
        c = new LinkedList();
        nl = el.getChildNodes();
        len = nl.getLength();
        
        for (int i = 0; i < len; i++)
        {
            Node n = nl.item(i);
            if (n instanceof Element)
            {
                c.add(n);
            }
        }
        
        return c;
    }
    
    static StringBuffer getTextValue(Element el)
    {
        StringBuffer sb = new StringBuffer(1024);
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i)
        {
            Node n = nl.item(i);
            if (n instanceof Text)
            {
                sb.append(n.getNodeValue());
            }
        }
        return sb;
    }
    
    static void convertTagsToLabels(StringBuffer buf, String parentLabel,
            Collection elements)
    {
        Collection  children;
        String tagLabel;

        for (Iterator i = elements.iterator(); i.hasNext();)
        {
            Element el = (Element) i.next();

            tagLabel = makeTagLabel(parentLabel, el);
            children = getElementChildren(el);
            
            if (children.size() > 0)
            {
                buf.append(tagLabel).append(" : #SAMENGESTELD\n");
                convertTagsToLabels(buf, tagLabel, children);
            }
            else
            {
                StringBuffer text = getTextValue(el);
                if (text != null && text.length() > 0)
                {
                    buf.append(tagLabel).append(" :").
                        append(text.toString()).append("\n"); // JDK1.4 needs no converstion text.toString()
                }
            }
         }
    }
    
    /**
     * Convert XML DOM document to flat string label-format of the rekenbox.
     * Input must be of type <code>org.w3c.dom.Element</code> or 
     * <code>org.w3c.dom.Document</code>; output will be of type
     * <code>java.lang.String</code>.

     */
    public static Object doTransformation(/*Message message, Map scratchpad,*/ Object data)
            throws Exception
    {
        Document        doc;
        Element         el;
        StringBuffer    buf;
        Collection      c;
        
        buf = new StringBuffer(10*1024);
        if (data instanceof Document)
        {
            doc = (Document)data;
            el = doc.getDocumentElement();
        }
        else if (data instanceof Element)
        {
            el = (Element)data;
        }
        else
        {
            throw new Exception(
                "Input not of type Document or Element, but of type " + data.getClass());
        }
        c = getElementChildren(el);
        convertTagsToLabels(buf, "", c);
        
        return buf.toString();
    }
}
