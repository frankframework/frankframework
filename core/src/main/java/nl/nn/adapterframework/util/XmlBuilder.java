/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

/**
 * Builds a XML-element with attributes and sub-elements. Attributes can be
 * added with the addAttribute method, the content can be set with the setValue
 * method. Subelements can be added with the addSubElement method. the toXML
 * function returns the node and subnodes as an indented xml string.
 * <p/>
 * Before February 2018 this class was deprecated. From then it uses the JDOM
 * standard solution.
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 **/
@Deprecated // Please replace with SaxDocumentBuilder or IDocumentBuilder from DocumentBuilderFactory.startDocument()
public class XmlBuilder extends XmlBuilderNew {

	public XmlBuilder(String tagName) {
		super(tagName);
	}
}
