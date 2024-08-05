/*
   Copyright 2021, 2022 WeAreFrank!

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
package org.frankframework.documentbuilder;

import org.frankframework.xml.SaxElementBuilder;
import org.xml.sax.SAXException;

public class XmlArrayBuilder extends ArrayBuilder {

	private final SaxElementBuilder current;
	private final String elementName;


	public XmlArrayBuilder(SaxElementBuilder current, String elementName) {
		this.elementName = elementName;
		this.current = current;
	}



	@Override
	public INodeBuilder addElement() throws SAXException {
		return new XmlNodeBuilder(current, elementName);
	}
}
